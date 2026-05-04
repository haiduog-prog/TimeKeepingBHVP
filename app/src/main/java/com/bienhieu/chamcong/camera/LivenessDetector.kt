package com.bienhieu.chamcong.camera

import com.google.mlkit.vision.face.Face

/**
 * Isolated logic for anti-spoofing / liveness detection.
 */
class BlinkLivenessDetector {

    // Internal state machine
    private var hasEyesClosed = false
    private var hasEyesOpenedAgain = false

    /**
     * Feed a face into the detector.
     * Returns true if the liveness check (blink) has been passed.
     */
    fun processFace(face: Face): Boolean {
        // If already passed, stay passed until reset
        if (hasEyesOpenedAgain) return true

        val leftEye = face.leftEyeOpenProbability
        val rightEye = face.rightEyeOpenProbability

        // If probabilities are not available, we can't determine liveness
        if (leftEye == null || rightEye == null) return false

        val isClosed = leftEye < 0.3f && rightEye < 0.3f
        val isOpen = leftEye > 0.7f && rightEye > 0.7f

        if (isClosed) {
            hasEyesClosed = true
        } else if (isOpen && hasEyesClosed) {
            hasEyesOpenedAgain = true
        }

        return hasEyesOpenedAgain
    }

    /**
     * Reset the liveness state machine.
     * Should be called after a successful attendance match or when a new face enters the frame.
     */
    fun reset() {
        hasEyesClosed = false
        hasEyesOpenedAgain = false
    }
}
