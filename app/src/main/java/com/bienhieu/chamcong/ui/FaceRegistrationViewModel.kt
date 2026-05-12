package com.bienhieu.chamcong.ui

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import java.io.File
import java.io.FileOutputStream
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.bienhieu.chamcong.data.local.EmployeeEntity
import com.bienhieu.chamcong.data.local.TimeKeepingDatabase
import com.bienhieu.chamcong.data.remote.SupabaseSyncManager
import com.bienhieu.chamcong.data.repository.AttendanceRepository
import com.bienhieu.chamcong.ml.FaceEmbeddingHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * ViewModel dedicated to multi-angle face registration.
 *
 * ─── Responsibilities ───
 *  1. Guide the user through 3 capture steps (Straight, Left, Right).
 *  2. Extract embeddings from each captured face.
 *  3. Save/update employee with all collected vectors.
 *
 * This ViewModel is scoped to the FaceRegistrationScreen and
 * does NOT share state with AttendanceViewModel.
 */
class FaceRegistrationViewModel(
    private val repository: AttendanceRepository,
    private val embeddingHelper: FaceEmbeddingHelper
) : ViewModel() {

    companion object {
        private const val TAG = "FaceRegVM"
    }

    // ─── Registration Step State ───

    /**
     * Steps: 0 = Straight, 1 = Left, 2 = Right, 3 = Done (show name input)
     * -1 = not registering
     */
    private val _registrationStep = MutableStateFlow(-1)
    val registrationStep: StateFlow<Int> = _registrationStep.asStateFlow()

    /** Prompt shown on camera overlay */
    private val _cameraPrompt = MutableStateFlow<String?>(null)
    val cameraPrompt: StateFlow<String?> = _cameraPrompt.asStateFlow()

    /** Latest face detected by the registration camera */
    val latestDetectedFace = MutableStateFlow<Bitmap?>(null)

    /** Employee being updated (null = new employee registration) */
    private val _employeeToRegister = MutableStateFlow<EmployeeEntity?>(null)
    val employeeToRegister: StateFlow<EmployeeEntity?> = _employeeToRegister.asStateFlow()

    /** Whether a save/update operation is in progress */
    private val _isSaving = MutableStateFlow(false)
    val isSaving: StateFlow<Boolean> = _isSaving.asStateFlow()

    // ─── Internal collected data ───
    private val _collectedEmbeddings = mutableListOf<FloatArray>()
    private val _collectedPhotoPaths = mutableListOf<String>()

    private val stepLabels = listOf(
        "Nhìn thẳng vào camera",
        "Quay mặt sang TRÁI",
        "Quay mặt sang PHẢI"
    )

    // ─── Lifecycle ───

    /**
     * Start a fresh 3-step registration flow.
     * Call this when the screen enters composition.
     */
    fun startRegistration() {
        _collectedEmbeddings.clear()
        _collectedPhotoPaths.clear()
        _registrationStep.value = 0
        _cameraPrompt.value = stepLabels[0]
    }

    /**
     * Start registration for an existing employee (update face).
     */
    fun startRegistrationFor(employeeId: String) {
        viewModelScope.launch {
            val employee = withContext(Dispatchers.IO) {
                repository.getAllEmployees().find { it.id == employeeId }
            }
            _employeeToRegister.value = employee
            startRegistration()
        }
    }

    /**
     * Reset all registration state. Safe to call multiple times.
     */
    fun resetRegistration() {
        _registrationStep.value = -1
        _collectedEmbeddings.clear()
        _collectedPhotoPaths.clear()
        _cameraPrompt.value = null
        _employeeToRegister.value = null
    }

    // ─── Camera callbacks ───

    /**
     * Called by the registration camera's FaceAnalyzer when a face is detected.
     */
    fun onFaceDetected(faceBitmap: Bitmap, yaw: Float) {
        latestDetectedFace.value = faceBitmap
    }

    // ─── Capture Flow ───

    /**
     * Capture the current face at the current step.
     * Extracts embedding and advances to next step.
     */
    fun captureStep(context: Context) {
        val face = latestDetectedFace.value ?: return
        val step = _registrationStep.value
        if (step < 0 || step > 2) return

        viewModelScope.launch {
            try {
                val embedding = withContext(Dispatchers.Default) {
                    embeddingHelper.getEmbedding(face)
                }

                val imagePath = withContext(Dispatchers.IO) {
                    val fileName = "face_reg_${System.currentTimeMillis()}.jpg"
                    val file = File(context.filesDir, fileName)
                    FileOutputStream(file).use { out ->
                        face.compress(Bitmap.CompressFormat.JPEG, 90, out)
                    }
                    file.absolutePath
                }

                _collectedEmbeddings.add(embedding)
                _collectedPhotoPaths.add(imagePath)

                val nextStep = step + 1
                if (nextStep <= 2) {
                    _registrationStep.value = nextStep
                    _cameraPrompt.value = stepLabels[nextStep]
                } else {
                    _registrationStep.value = 3
                    _cameraPrompt.value = null
                }

                Log.d(TAG, "Captured step $step, total: ${_collectedEmbeddings.size}")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to capture step $step", e)
            }
        }
    }

    // ─── Save / Update ───

    /**
     * Save a new employee with all collected multi-angle embeddings.
     * Snapshots the data immediately so it's safe even if reset is called afterward.
     */
    fun saveNewEmployee(name: String, context: Context, onComplete: () -> Unit) {
        // Snapshot data BEFORE launching coroutine to avoid race with resetRegistration()
        val embeddings = _collectedEmbeddings.toList()
        val photoPath = _collectedPhotoPaths.firstOrNull()
        if (embeddings.isEmpty()) return

        _isSaving.value = true
        viewModelScope.launch {
            try {
                val employee = EmployeeEntity(
                    id = java.util.UUID.randomUUID().toString(),
                    name = name,
                    faceVectors = embeddings,
                    photoPath = photoPath
                )

                withContext(Dispatchers.IO) {
                    repository.insertEmployee(employee)
                }

                Log.d(TAG, "Registered: $name with ${embeddings.size} vectors")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to register: $name", e)
            } finally {
                _isSaving.value = false
                onComplete()
            }
        }
    }

    /**
     * Update an existing employee's face with collected multi-angle embeddings.
     * Snapshots the data BEFORE launching to fix race condition with DisposableEffect cleanup.
     */
    fun updateExistingEmployee(context: Context, onComplete: () -> Unit) {
        val employee = _employeeToRegister.value ?: return
        // Snapshot data BEFORE launching coroutine
        val embeddings = _collectedEmbeddings.toList()
        val photoPath = _collectedPhotoPaths.firstOrNull() ?: employee.photoPath
        if (embeddings.isEmpty()) return

        _isSaving.value = true
        viewModelScope.launch {
            try {
                val updatedEmployee = employee.copy(
                    faceVectors = embeddings,
                    photoPath = photoPath
                )

                withContext(Dispatchers.IO) {
                    repository.updateEmployeeWithSync(updatedEmployee)
                }

                Log.d(TAG, "Updated: ${employee.name} with ${embeddings.size} vectors")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to update: ${employee.name}", e)
            } finally {
                _isSaving.value = false
                onComplete()
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
            return FaceRegistrationViewModel(repository, embeddingHelper) as T
        }
    }
}
