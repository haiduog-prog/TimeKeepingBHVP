package com.bienhieu.chamcong.ml

import kotlin.math.sqrt

/**
 * Vector similarity/distance functions for face matching.
 *
 * These operate on the embedding vectors produced by MobileFaceNet.
 * Both functions run in O(n) time where n = vector dimension.
 */
object VectorMath {

    /**
     * Calculate the Cosine Similarity between two vectors.
     *
     * ─── Mathematical Definition ───
     *
     *                    A · B             Σ(aᵢ × bᵢ)
     *   cos(θ) = ─────────────── = ─────────────────────────
     *             ‖A‖ × ‖B‖       √Σ(aᵢ²) × √Σ(bᵢ²)
     *
     * Where:
     *   - A · B  is the dot product of vectors A and B
     *   - ‖A‖    is the L2 norm (magnitude) of vector A
     *   - ‖B‖    is the L2 norm (magnitude) of vector B
     *
     * ─── Interpretation ───
     *   - Result range: [-1.0, 1.0]
     *   -  1.0  → identical direction (same face)
     *   -  0.0  → orthogonal (unrelated)
     *   - -1.0  → opposite direction
     *
     * For face recognition, a threshold of ~0.5–0.7 on cosine similarity
     * (or equivalently ~0.85+ after normalization) is typical.
     *
     * @param a First embedding vector.
     * @param b Second embedding vector.
     * @return Cosine similarity score in [-1.0, 1.0].
     */
    fun cosineSimilarity(a: FloatArray, b: FloatArray): Float {
        require(a.size == b.size) { "Vectors must have the same dimension: ${a.size} vs ${b.size}" }

        var dotProduct = 0.0f  // Σ(aᵢ × bᵢ)
        var normA = 0.0f       // Σ(aᵢ²)
        var normB = 0.0f       // Σ(bᵢ²)

        for (i in a.indices) {
            dotProduct += a[i] * b[i]
            normA += a[i] * a[i]
            normB += b[i] * b[i]
        }

        val denominator = sqrt(normA) * sqrt(normB)

        // Guard against division by zero (zero-magnitude vector)
        if (denominator == 0.0f) return 0.0f

        return dotProduct / denominator
    }

    /**
     * Calculate the Euclidean Distance (L2 distance) between two vectors.
     *
     * ─── Mathematical Definition ───
     *
     *   d(A, B) = √ Σ(aᵢ - bᵢ)²
     *
     * ─── Interpretation ───
     *   - Result range: [0, +∞)
     *   - 0.0  → identical vectors (same face)
     *   - Lower values → more similar faces
     *
     * For L2-normalized embeddings (which MobileFaceNet produces),
     * a distance threshold of ~1.0–1.2 is commonly used.
     *
     * @param a First embedding vector.
     * @param b Second embedding vector.
     * @return Euclidean distance (lower = more similar).
     */
    fun euclideanDistance(a: FloatArray, b: FloatArray): Float {
        require(a.size == b.size) { "Vectors must have the same dimension: ${a.size} vs ${b.size}" }

        var sum = 0.0f
        for (i in a.indices) {
            val diff = a[i] - b[i]
            sum += diff * diff
        }
        return sqrt(sum)
    }

    /**
     * L2-normalize a vector in-place.
     *
     * After normalization, the vector has unit length (‖v‖ = 1).
     * This makes cosine similarity equivalent to the dot product,
     * which is computationally cheaper for repeated comparisons.
     *
     * @param vector The vector to normalize.
     * @return The same array, modified in-place, for chaining.
     */
    fun l2Normalize(vector: FloatArray): FloatArray {
        var norm = 0.0f
        for (v in vector) norm += v * v
        norm = sqrt(norm)

        if (norm > 0f) {
            for (i in vector.indices) {
                vector[i] /= norm
            }
        }
        return vector
    }
}
