package com.bienhieu.chamcong.domain

import android.graphics.Bitmap
import com.bienhieu.chamcong.data.local.AttendanceEntity
import com.bienhieu.chamcong.data.repository.AttendanceRepository
import com.bienhieu.chamcong.ml.FaceEmbeddingHelper
import com.bienhieu.chamcong.ml.FaceMatcher
import com.bienhieu.chamcong.ui.AttendanceType

/**
 * Result of the face processing pipeline.
 */
sealed class ProcessResult {
    data class Success(val employeeName: String, val score: Float, val type: AttendanceType) : ProcessResult()
    data class Unknown(val score: Float) : ProcessResult()
    data class Error(val message: String) : ProcessResult()
}

/**
 * UseCase encapsulating the complex logic of identifying a face and recording attendance.
 */
class ProcessAttendanceUseCase(
    private val repository: AttendanceRepository,
    private val embeddingHelper: FaceEmbeddingHelper
) {
    /**
     * Extracts embedding from [faceBitmap], matches it against local employees,
     * determines IN/OUT type, and records the attendance.
     *
     * Should be called from a worker thread (Dispatchers.Default/IO).
     */
    suspend operator fun invoke(faceBitmap: Bitmap): ProcessResult {
        return try {
            // 1. Extract face embedding via TFLite (already L2-normalized internally)
            val embedding = embeddingHelper.getEmbedding(faceBitmap)

            // 2. Load all registered employees & find best match
            val employees = repository.getAllEmployees()
            val match = FaceMatcher.findBestMatch(embedding, employees)

            if (match.isMatch && match.employee != null) {
                val emp = match.employee

                // 3. Determine Attendance Type (IN / OUT)
                val latest = repository.getLatestAttendanceToday(emp.id)
                val type = if (latest == null || latest.status == AttendanceType.OUT.name) AttendanceType.IN else AttendanceType.OUT

                // 4. Record to DB
                val record = AttendanceEntity(
                    employeeId = emp.id,
                    employeeName = emp.name,
                    status = type.name,
                    confidence = match.score,
                    isSynced = false
                )
                repository.insertAttendanceWithSync(record)

                ProcessResult.Success(emp.name, match.score, type)
            } else {
                ProcessResult.Unknown(match.score)
            }
        } catch (e: Exception) {
            ProcessResult.Error(e.message ?: "Unknown error")
        }
    }
}