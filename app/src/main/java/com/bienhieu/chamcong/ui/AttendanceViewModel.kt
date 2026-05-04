package com.bienhieu.chamcong.ui

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import java.io.File
import java.io.FileOutputStream
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.bienhieu.chamcong.data.local.AttendanceEntity
import com.bienhieu.chamcong.data.local.TimeKeepingDatabase
import com.bienhieu.chamcong.data.local.EmployeeEntity
import com.bienhieu.chamcong.data.remote.SupabaseSyncManager
import com.bienhieu.chamcong.ml.FaceEmbeddingHelper
import com.bienhieu.chamcong.ml.FaceMatcher
import com.bienhieu.chamcong.ml.VectorMath
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Calendar
import com.bienhieu.chamcong.data.repository.AttendanceRepository
import com.bienhieu.chamcong.domain.ProcessAttendanceUseCase
import com.bienhieu.chamcong.domain.ProcessResult
import kotlinx.coroutines.delay

/**
 * ViewModel managing the attendance workflow state.
 *
 * ─── Responsibilities ───
 *  1. Accept cropped face bitmaps from the camera layer.
 *  2. Delegate processing to [ProcessAttendanceUseCase].
 *  3. Handle UI state transitions.
 */
class AttendanceViewModel(
    private val repository: AttendanceRepository,
    private val processAttendanceUseCase: ProcessAttendanceUseCase,
    private val embeddingHelper: FaceEmbeddingHelper
) : ViewModel() {

    companion object {
        private const val TAG = "AttendanceVM"
    }

    init {
        // Auto-sync employees on startup
        viewModelScope.launch {
            repository.syncRemoteEmployees()
            // Also try to push any unsynced offline records from previous sessions
            repository.syncOfflineAttendance()
        }
    }

    // ─── UI State ───

    /** Current state of the attendance scanning flow. */
    private val _uiState = MutableStateFlow<AttendanceUiState>(AttendanceUiState.Scanning)
    val uiState: StateFlow<AttendanceUiState> = _uiState.asStateFlow()

    // ─── Liveness State ───
    private val _isLivenessEnabled = MutableStateFlow(false)
    val isLivenessEnabled: StateFlow<Boolean> = _isLivenessEnabled.asStateFlow()

    fun toggleLiveness(enabled: Boolean) {
        _isLivenessEnabled.value = enabled
    }

    private val _showLivenessPrompt = MutableStateFlow(false)
    val showLivenessPrompt: StateFlow<Boolean> = _showLivenessPrompt.asStateFlow()

    fun updateLivenessPrompt(show: Boolean) {
        _showLivenessPrompt.value = show
    }

    /** Holds the most recent face cropped by the camera analyzer (used for registration preview). */
    val latestDetectedFace = MutableStateFlow<Bitmap?>(null)

    /** Observable list of today's attendance records for the dashboard. */
    val todayRecords: Flow<List<AttendanceEntity>> = repository.observeTodayRecords()

    /** Observable list of all registered employees. */
    val employees: Flow<List<EmployeeEntity>> = repository.observeAllEmployees()

    // ─── Face Registration State ───
    private val _employeeToRegister = MutableStateFlow<EmployeeEntity?>(null)
    val employeeToRegister: StateFlow<EmployeeEntity?> = _employeeToRegister.asStateFlow()

    fun startRegistrationFor(employee: EmployeeEntity) {
        _employeeToRegister.value = employee
    }

    fun cancelRegistration() {
        _employeeToRegister.value = null
    }

    // ─── Attendance Flow ───

    fun onFaceDetected(faceBitmap: Bitmap, yaw: Float) {
        // Always update the latest face so the registration dialog can preview it
        latestDetectedFace.value = faceBitmap

        // Don't start a new scan if we're already showing a result
        if (_uiState.value !is AttendanceUiState.Scanning) return

        viewModelScope.launch {
            _uiState.value = AttendanceUiState.Processing

            // ── Delegate to UseCase (runs on Default dispatcher internally) ──
            val result = withContext(Dispatchers.Default) {
                processAttendanceUseCase(faceBitmap)
            }

            // ── Handle match result ──
            when (result) {
                is ProcessResult.Success -> {
                    _uiState.value = AttendanceUiState.Matched(
                        employeeName = result.employeeName,
                        score = result.score,
                        type = result.type
                    )
                    // No auto-reset: user must dismiss the popup to continue
                }
                is ProcessResult.Unknown -> {
                    _uiState.value = AttendanceUiState.Unknown(score = result.score)
                    // Auto-reset only for unrecognized faces
                    launch {
                        delay(3000L)
                        _uiState.value = AttendanceUiState.Scanning
                    }
                }
                is ProcessResult.Error -> {
                    Log.e(TAG, "Attendance processing failed: ${result.message}")
                    _uiState.value = AttendanceUiState.Error(result.message)
                    launch {
                        delay(3000L)
                        _uiState.value = AttendanceUiState.Scanning
                    }
                }
            }
        }
    }

    // ─── Employee Registration ───

    fun registerEmployee(name: String, faceBitmap: Bitmap, context: Context) {
        viewModelScope.launch {
            try {
                // 1. Extract embedding
                val embedding = withContext(Dispatchers.Default) {
                    val raw = embeddingHelper.getEmbedding(faceBitmap)
                    VectorMath.l2Normalize(raw)
                    raw
                }

                // 2. Save the face image to a file
                val imagePath = withContext(Dispatchers.IO) {
                    val fileName = "face_${System.currentTimeMillis()}.jpg"
                    val file = File(context.filesDir, fileName)
                    FileOutputStream(file).use { out ->
                        faceBitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
                    }
                    file.absolutePath
                }

                // 3. Save to database
                val employee = EmployeeEntity(
                    id = java.util.UUID.randomUUID().toString(),
                    name = name,
                    faceVectors = listOf(embedding),
                    photoPath = imagePath
                )

                withContext(Dispatchers.IO) {
                    repository.insertEmployee(employee)
                }

                Log.d(TAG, "Registered employee: $name (photo: $imagePath)")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to register employee: $name", e)
            }
        }
    }

    fun updateEmployeeFace(context: Context) {
        val employee = _employeeToRegister.value ?: return
        val faceBitmap = latestDetectedFace.value ?: return
        
        viewModelScope.launch {
            try {
                // 1. Extract embedding
                val embedding = withContext(Dispatchers.Default) {
                    val raw = embeddingHelper.getEmbedding(faceBitmap)
                    VectorMath.l2Normalize(raw)
                    raw
                }

                // 2. Save the face image to a file
                val imagePath = withContext(Dispatchers.IO) {
                    val fileName = "face_${employee.id}.jpg"
                    val file = File(context.filesDir, fileName)
                    FileOutputStream(file).use { out ->
                        faceBitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
                    }
                    file.absolutePath
                }

                // 3. Update database & Trigger Sync
                val updatedEmployee = employee.copy(
                    faceVectors = listOf(embedding),
                    photoPath = imagePath
                )

                withContext(Dispatchers.IO) {
                    repository.updateEmployeeWithSync(updatedEmployee)
                }

                Log.d(TAG, "Updated face for employee: ${employee.name}")
                _employeeToRegister.value = null
            } catch (e: Exception) {
                Log.e(TAG, "Failed to update employee face: ${employee.name}", e)
            }
        }
    }

    fun deleteEmployee(employeeId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                repository.deleteEmployee(employeeId)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to delete employee", e)
            }
        }
    }

    /** Reset state back to scanning. */
    fun resetToScanning() {
        viewModelScope.launch {
            _uiState.value = AttendanceUiState.Paused
            delay(2000L) // Wait 2s before resuming camera
            _uiState.value = AttendanceUiState.Scanning
        }
    }

    // ─── Factory ───

    class Factory(
        private val database: TimeKeepingDatabase,
        private val embeddingHelper: FaceEmbeddingHelper
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            val syncManager = SupabaseSyncManager(database.employeeDao(), database.attendanceDao())
            val repository = AttendanceRepository(database.employeeDao(), database.attendanceDao(), syncManager)
            val useCase = ProcessAttendanceUseCase(repository, embeddingHelper)
            return AttendanceViewModel(repository, useCase, embeddingHelper) as T
        }
    }
}

/**
 * Enum representing check-in / check-out attendance type.
 * Using an enum instead of raw strings prevents mismatch bugs.
 */
enum class AttendanceType { IN, OUT }

/**
 * Sealed class representing all possible states of the attendance scanning UI.
 */
sealed class AttendanceUiState {
    /** Camera is active, waiting for a face. */
    data object Scanning : AttendanceUiState()

    /** A face was detected and is being processed (embedding + matching). */
    data object Processing : AttendanceUiState()

    /** Face matched a registered employee. User must dismiss popup to continue. */
    data class Matched(
        val employeeName: String,
        val score: Float,
        val type: AttendanceType
    ) : AttendanceUiState()

    /** Face was detected but didn't match any registered employee. */
    data class Unknown(val score: Float) : AttendanceUiState()

    /** An error occurred during processing. */
    data class Error(val message: String) : AttendanceUiState()

    /** Camera paused for a short duration before scanning again. */
    data object Paused : AttendanceUiState()
}
