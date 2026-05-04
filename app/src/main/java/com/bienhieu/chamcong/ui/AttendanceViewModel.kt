package com.bienhieu.chamcong.ui

import android.graphics.Bitmap
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.bienhieu.chamcong.data.local.AttendanceEntity
import com.bienhieu.chamcong.data.local.TimeKeepingDatabase
import com.bienhieu.chamcong.data.local.EmployeeEntity
import com.bienhieu.chamcong.data.remote.SupabaseSyncManager
import com.bienhieu.chamcong.data.repository.AttendanceRepository
import com.bienhieu.chamcong.domain.ProcessAttendanceUseCase
import com.bienhieu.chamcong.domain.ProcessResult
import com.bienhieu.chamcong.ml.FaceEmbeddingHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * ViewModel managing the attendance scanning workflow.
 *
 * ─── Responsibilities ───
 *  1. Accept cropped face bitmaps from the camera layer.
 *  2. Delegate processing to [ProcessAttendanceUseCase].
 *  3. Handle UI state transitions (Scanning → Processing → Matched/Unknown/Error).
 *  4. Manage liveness toggle and yaw quality filter.
 *
 * This ViewModel does NOT handle face registration — see [FaceRegistrationViewModel].
 */
class AttendanceViewModel(
    private val repository: AttendanceRepository,
    private val processAttendanceUseCase: ProcessAttendanceUseCase
) : ViewModel() {

    companion object {
        private const val TAG = "AttendanceVM"
        /** Max yaw angle (degrees) for accepting a face during check-in */
        private const val MAX_YAW_FOR_CHECKIN = 20f
    }

    init {
        // Auto-sync employees on startup
        viewModelScope.launch {
            repository.syncRemoteEmployees()
            repository.syncOfflineAttendance()
        }
    }

    // ─── UI State ───

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

    // ─── Camera Prompt (yaw quality filter) ───

    private val _cameraPrompt = MutableStateFlow<String?>(null)
    val cameraPrompt: StateFlow<String?> = _cameraPrompt.asStateFlow()

    // ─── Observable Data ───

    val todayRecords: Flow<List<AttendanceEntity>> = repository.observeTodayRecords()
    val employees: Flow<List<EmployeeEntity>> = repository.observeAllEmployees()

    // ─── Attendance Flow ───

    fun onFaceDetected(faceBitmap: Bitmap, yaw: Float) {
        // Don't start a new scan if we're already showing a result
        if (_uiState.value !is AttendanceUiState.Scanning) return

        // Quality filter: reject tilted faces during check-in
        if (kotlin.math.abs(yaw) > MAX_YAW_FOR_CHECKIN) {
            _cameraPrompt.value = "Vui lòng nhìn thẳng vào camera"
            return
        }
        _cameraPrompt.value = null

        viewModelScope.launch {
            _uiState.value = AttendanceUiState.Processing

            val result = withContext(Dispatchers.Default) {
                processAttendanceUseCase(faceBitmap)
            }

            when (result) {
                is ProcessResult.Success -> {
                    _uiState.value = AttendanceUiState.Matched(
                        employeeName = result.employeeName,
                        score = result.score,
                        type = result.type
                    )
                }
                is ProcessResult.Unknown -> {
                    _uiState.value = AttendanceUiState.Unknown(score = result.score)
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

    /** Reset state back to scanning. */
    fun resetToScanning() {
        viewModelScope.launch {
            _uiState.value = AttendanceUiState.Paused
            delay(2000L)
            _uiState.value = AttendanceUiState.Scanning
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
            return AttendanceViewModel(repository, useCase) as T
        }
    }
}

/**
 * Enum representing check-in / check-out attendance type.
 */
enum class AttendanceType { IN, OUT }

/**
 * Sealed class representing all possible states of the attendance scanning UI.
 */
sealed class AttendanceUiState {
    data object Scanning : AttendanceUiState()
    data object Processing : AttendanceUiState()
    data class Matched(
        val employeeName: String,
        val score: Float,
        val type: AttendanceType
    ) : AttendanceUiState()
    data class Unknown(val score: Float) : AttendanceUiState()
    data class Error(val message: String) : AttendanceUiState()
    data object Paused : AttendanceUiState()
}