package com.bienhieu.chamcong.ml

import android.content.Context
import android.content.res.AssetFileDescriptor
import android.graphics.Bitmap
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel

/**
 * TensorFlow Lite helper for extracting face embeddings using MobileFaceNet.
 *
 * ─── Model Details ───
 * - Input:  112 × 112 × 3 (RGB normalized to [-1, 1])
 * - Output: 1 × 192 (or 128/512 depending on your .tflite variant)
 *
 * Place your `mobilefacenet.tflite` model file in `app/src/main/assets/`.
 *
 * @param context Application context for loading the model from assets.
 */
class FaceEmbeddingHelper(context: Context) {

    companion object {
        /** Name of the TFLite model file in the assets folder. */
        private const val MODEL_FILE = "mobilefacenet.tflite"

        /**
         * Input image dimensions expected by MobileFaceNet.
         * For this specific model variant, it requires 160x160.
         */
        const val INPUT_SIZE = 160

        /**
         * Dimension of the output embedding vector.
         * Adjust this to match your specific model variant:
         *   - MobileFaceNet standard: 192
         *   - Some variants: 128 or 512
         */
        const val EMBEDDING_DIM = 512
    }

    private val interpreter: Interpreter

    init {
        // Load the .tflite model from assets using memory-mapped file
        // (no dependency on tensorflow-lite-support library)
        val modelBuffer = loadModelFile(context)
        val options = Interpreter.Options().apply {
            setNumThreads(4) // Use multiple CPU threads for inference speed
        }
        interpreter = Interpreter(modelBuffer, options)
    }

    /**
     * Load a TFLite model file from the assets folder as a MappedByteBuffer.
     * This replaces FileUtil.loadMappedFile() to avoid the
     * tensorflow-lite-support dependency (which causes litert-api conflicts).
     */
    private fun loadModelFile(context: Context): MappedByteBuffer {
        val assetFileDescriptor: AssetFileDescriptor = context.assets.openFd(MODEL_FILE)
        val inputStream = FileInputStream(assetFileDescriptor.fileDescriptor)
        val fileChannel = inputStream.channel
        val startOffset = assetFileDescriptor.startOffset
        val declaredLength = assetFileDescriptor.declaredLength
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)
    }

    /**
     * Extract a face embedding vector from a cropped face bitmap.
     *
     * Processing pipeline:
     *   1. Resize the bitmap to 112×112
     *   2. Normalize pixel values from [0, 255] → [-1.0, 1.0]
     *   3. Pack into a ByteBuffer (FLOAT32, NHWC format)
     *   4. Run TFLite inference
     *   5. Return the embedding as a FloatArray
     *
     * @param faceBitmap A cropped bitmap containing just the face region.
     * @return FloatArray of size [EMBEDDING_DIM] representing the face embedding.
     */
    fun getEmbedding(faceBitmap: Bitmap): FloatArray {
        // ── Step 1: Resize to model input size ──
        val resized = Bitmap.createScaledBitmap(faceBitmap, INPUT_SIZE, INPUT_SIZE, true)

        // ── Step 2 & 3: Normalize and pack into ByteBuffer ──
        val inputBuffer = bitmapToByteBuffer(resized)

        // ── Step 4: Prepare output array ──
        // Shape: [1, EMBEDDING_DIM] – batch size of 1
        val output = Array(1) { FloatArray(EMBEDDING_DIM) }

        // ── Step 5: Run inference ──
        interpreter.run(inputBuffer, output)

        val embedding = output[0]
        VectorMath.l2Normalize(embedding)
        return embedding
    }

    /**
     * Convert a 112×112 Bitmap into a ByteBuffer suitable for TFLite input.
     *
     * ─── Normalization Math ───
     * Each pixel channel (R, G, B) is an integer in [0, 255].
     * We normalize to [-1.0, 1.0] using:
     *     normalized = (pixelValue / 255.0f) * 2.0f - 1.0f
     *
     * This is equivalent to:
     *     normalized = (pixelValue - 127.5f) / 127.5f
     *
     * The resulting buffer is in NHWC format:
     *   N = 1 (batch), H = 112, W = 112, C = 3 (RGB)
     */
    private fun bitmapToByteBuffer(bitmap: Bitmap): ByteBuffer {
        val byteBuffer = ByteBuffer.allocateDirect(
            1 * INPUT_SIZE * INPUT_SIZE * 3 * Float.SIZE_BYTES
        ).apply {
            order(ByteOrder.nativeOrder())
        }

        val pixels = IntArray(INPUT_SIZE * INPUT_SIZE)
        bitmap.getPixels(pixels, 0, INPUT_SIZE, 0, 0, INPUT_SIZE, INPUT_SIZE)

        for (pixel in pixels) {
            // Extract RGB channels and normalize to [-1, 1]
            val r = ((pixel shr 16) and 0xFF) / 127.5f - 1.0f
            val g = ((pixel shr 8) and 0xFF) / 127.5f - 1.0f
            val b = (pixel and 0xFF) / 127.5f - 1.0f

            byteBuffer.putFloat(r)
            byteBuffer.putFloat(g)
            byteBuffer.putFloat(b)
        }

        byteBuffer.rewind()
        return byteBuffer
    }

    /**
     * Release the TFLite interpreter resources.
     * Call this when the app is being destroyed.
     */
    fun close() {
        interpreter.close()
    }
}
