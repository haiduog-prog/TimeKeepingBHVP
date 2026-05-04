@file:Suppress("UnsafeOptInUsageError")

package com.bienhieu.chamcong.camera

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageFormat
import android.graphics.Matrix
import android.graphics.Rect
import android.graphics.YuvImage
import android.util.Log
import androidx.annotation.OptIn
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.Face
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import java.io.ByteArrayOutputStream

/**
 * CameraX ImageAnalysis.Analyzer that detects faces using ML Kit
 * and crops the detected face region as a Bitmap.
 *
 * ─── Pipeline ───
 *
 *   CameraX Frame (ImageProxy)
 *        │
 *        ▼
 *   ML Kit Face Detection
 *        │
 *        ▼
 *   Crop face bounding box from frame
 *        │
 *        ▼
 *   Callback with cropped Bitmap
 *
 * ─── Threading ───
 *
 * This analyzer runs on the CameraX analysis thread (background).
 * The [onFaceDetected] callback is invoked on that same background thread,
 * so the caller can safely run TFLite inference without blocking the UI.
 *
 * @param onFaceDetected Callback invoked when a face is detected.
 *        Receives the cropped face Bitmap. Called on a background thread.
 * @param onNoFace       Callback invoked when no face is detected in the frame.
 */
class FaceAnalyzer(
    private val isLivenessEnabled: () -> Boolean,
    private val onLivenessStateChange: (Boolean) -> Unit,
    private val onFaceDetected: (Bitmap) -> Unit,
    private val onNoFace: () -> Unit
) : ImageAnalysis.Analyzer {

    companion object {
        private const val TAG = "FaceAnalyzer"

        /**
         * Minimum face size relative to the image.
         * Filters out tiny faces that are too far away for reliable recognition.
         */
        private const val MIN_FACE_SIZE = 0.2f
    }

    /**
     * ML Kit face detector configured for real-time performance.
     *
     * - PERFORMANCE_MODE_FAST: Optimized for speed over accuracy.
     * - setMinFaceSize: Ignores faces smaller than 15% of the image.
     * - No landmarks or classification needed for embedding extraction.
     */
    private val faceDetector = FaceDetection.getClient(
        FaceDetectorOptions.Builder()
            .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
            .setMinFaceSize(MIN_FACE_SIZE)
            .setContourMode(FaceDetectorOptions.CONTOUR_MODE_NONE)
            .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
            .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_ALL)
            .build()
    )

    private val blinkDetector = BlinkLivenessDetector()

    /**
     * Throttle flag to prevent overlapping ML Kit calls.
     * Only one frame is processed at a time.
     */
    @Volatile
    private var isProcessing = false

    @OptIn(ExperimentalGetImage::class)
    override fun analyze(imageProxy: ImageProxy) {
        // ── Throttle: skip frame if previous detection is still running ──
        if (isProcessing) {
            imageProxy.close()
            return
        }

        val mediaImage = imageProxy.image
        if (mediaImage == null) {
            imageProxy.close()
            return
        }

        isProcessing = true

        // ── Create ML Kit InputImage from the CameraX frame ──
        val inputImage = InputImage.fromMediaImage(
            mediaImage,
            imageProxy.imageInfo.rotationDegrees
        )

        // ── Run face detection ──
        faceDetector.process(inputImage)
            .addOnSuccessListener { faces ->
                if (faces.isNotEmpty()) {
                    // Take the largest face (closest to camera) for best recognition
                    val bestFace = faces.maxByOrNull { it.boundingBox.width() * it.boundingBox.height() }
                    if (bestFace != null) {
                        // Anti-spoofing logic
                        if (isLivenessEnabled()) {
                            val passed = blinkDetector.processFace(bestFace)
                            if (!passed) {
                                // Face found, but liveness not passed yet — just show prompt
                                onLivenessStateChange(true)
                                // Do NOT call handleNoFace here — that would reset the blink detector!
                                return@addOnSuccessListener
                            }
                            // Blink detected! Reset for next scan and continue
                            blinkDetector.reset()
                        }

                        // Passed liveness or disabled
                        onLivenessStateChange(false)
                        val croppedFace = cropFaceFromProxy(imageProxy, bestFace)
                        if (croppedFace != null) {
                            onFaceDetected(croppedFace)
                        } else {
                            handleNoFace()
                        }
                    } else {
                        handleNoFace()
                    }
                } else {
                    handleNoFace()
                }
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Face detection failed", e)
                handleNoFace()
            }
            .addOnCompleteListener {
                isProcessing = false
                imageProxy.close()
            }
    }

    private fun handleNoFace() {
        blinkDetector.reset()
        onLivenessStateChange(false)
        onNoFace()
    }

    /**
     * Crop the face region from the camera frame.
     *
     * ─── Pipeline ───
     * 1. Convert ImageProxy (YUV_420_888) → NV21 byte array
     * 2. Encode NV21 → JPEG → Bitmap (full frame)
     * 3. Apply rotation correction
     * 4. Clamp bounding box to image bounds
     * 5. Crop the face region
     *
     * @param imageProxy The CameraX frame.
     * @param face       The detected face with bounding box.
     * @return Cropped face Bitmap, or null on failure.
     */
    private fun cropFaceFromProxy(imageProxy: ImageProxy, face: Face): Bitmap? {
        return try {
            // ── Convert YUV → Bitmap ──
            val fullBitmap = imageProxyToBitmap(imageProxy) ?: return null

            // ── Apply rotation to align bounding box coordinates ──
            val rotatedBitmap = rotateBitmap(fullBitmap, imageProxy.imageInfo.rotationDegrees)

            // ── Calculate square bounding box with 20% padding ──
            val box = face.boundingBox
            val cx = box.centerX().coerceIn(0, rotatedBitmap.width - 1)
            val cy = box.centerY().coerceIn(0, rotatedBitmap.height - 1)

            // Base size is the max dimension to ensure the whole face fits
            val baseSize = maxOf(box.width(), box.height())
            // Add 20% padding
            val paddedSize = (baseSize * 1.2f).toInt()

            // To prevent squishing, the crop MUST be perfectly square.
            // If the box is near the edge of the image, we must shrink the square
            // rather than clamping one side (which would make it a rectangle).
            var halfSize = paddedSize / 2

            val distLeft = cx
            val distRight = rotatedBitmap.width - cx
            val distTop = cy
            val distBottom = rotatedBitmap.height - cy

            val maxAllowedHalfSize = minOf(distLeft, distRight, distTop, distBottom)

            if (halfSize > maxAllowedHalfSize) {
                halfSize = maxAllowedHalfSize
            }

            val finalSize = halfSize * 2
            if (finalSize <= 0) return null

            val left = cx - halfSize
            val top = cy - halfSize

            // ── Crop the perfectly square face region ──
            Bitmap.createBitmap(rotatedBitmap, left, top, finalSize, finalSize)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to crop face", e)
            null
        }
    }

    /**
     * Convert an ImageProxy (YUV_420_888 format) to a Bitmap.
     *
     * The conversion path is:
     *   YUV_420_888 → NV21 byte array → YuvImage → JPEG → Bitmap
     */
    private fun imageProxyToBitmap(imageProxy: ImageProxy): Bitmap? {
        val nv21 = yuv420ToNv21(imageProxy)
        val yuvImage = YuvImage(nv21, ImageFormat.NV21, imageProxy.width, imageProxy.height, null)
        val outputStream = ByteArrayOutputStream()
        yuvImage.compressToJpeg(Rect(0, 0, imageProxy.width, imageProxy.height), 100, outputStream)
        val jpegBytes = outputStream.toByteArray()
        return BitmapFactory.decodeByteArray(jpegBytes, 0, jpegBytes.size)
    }

    /**
     * Convert YUV_420_888 planes to NV21 byte format.
     *
     * NV21 layout:  [Y plane] [VU interleaved plane]
     * This is the format expected by Android's YuvImage class.
     */
    private fun yuv420ToNv21(imageProxy: ImageProxy): ByteArray {
        val yPlane = imageProxy.planes[0]
        val uPlane = imageProxy.planes[1]
        val vPlane = imageProxy.planes[2]

        val yBuffer = yPlane.buffer
        val uBuffer = uPlane.buffer
        val vBuffer = vPlane.buffer

        val ySize = yBuffer.remaining()
        val uSize = uBuffer.remaining()
        val vSize = vBuffer.remaining()

        val nv21 = ByteArray(ySize + uSize + vSize)

        // Copy Y plane
        yBuffer.get(nv21, 0, ySize)

        // Interleave V and U (NV21 = YYYYVUVU)
        val uvPixelStride = vPlane.pixelStride
        if (uvPixelStride == 2) {
            // Already interleaved VU data — just copy V buffer (which includes U)
            vBuffer.get(nv21, ySize, vSize)
        } else {
            // Non-interleaved — manually interleave
            var offset = ySize
            for (i in 0 until uSize) {
                nv21[offset++] = vBuffer.get(i)
                nv21[offset++] = uBuffer.get(i)
            }
        }

        return nv21
    }

    /**
     * Rotate a Bitmap by the specified degrees.
     * CameraX frames may be rotated depending on device orientation.
     */
    private fun rotateBitmap(bitmap: Bitmap, rotationDegrees: Int): Bitmap {
        if (rotationDegrees == 0) return bitmap
        val matrix = Matrix().apply { postRotate(rotationDegrees.toFloat()) }
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }
}
