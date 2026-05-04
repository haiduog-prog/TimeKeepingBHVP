package com.bienhieu.chamcong.ml

import com.bienhieu.chamcong.data.local.EmployeeEntity

/**
 * Result of a face matching operation against the employee database.
 *
 * @property employee  The best-matching employee, or null if no match above threshold.
 * @property score     The cosine similarity score of the best match.
 * @property isMatch   Whether the score exceeds the acceptance threshold.
 */
data class MatchResult(
    val employee: EmployeeEntity?,
    val score: Float,
    val isMatch: Boolean
)

/**
 * Face matching engine that compares a probe embedding against all registered employees.
 *
 * ─── Algorithm ───
 *
 * 1. Receive the probe face embedding (from the camera frame).
 * 2. Load all employee embeddings from Room (cached in memory for speed).
 * 3. For each employee, compute cosine similarity between probe and stored embedding.
 * 4. Find the employee with the highest similarity score.
 * 5. If the highest score ≥ threshold → match found; otherwise → unknown face.
 *
 * ─── Complexity ───
 *
 * O(N × D) where N = number of employees, D = embedding dimension.
 * For 1,000 employees × 192 dimensions, this takes < 1ms on modern devices.
 */
object FaceMatcher {

    /**
     * Minimum cosine similarity score required to consider a match valid.
     *
     * ─── Threshold Selection Guide ───
     *   - 0.50 → Very lenient (high false positive rate)
     *   - 0.60 → Moderate (good for indoor, controlled lighting)
     *   - 0.70 → Strict (recommended for production kiosk)
     *   - 0.85 → Very strict (may cause false rejections)
     *
     * Tune this based on your lighting conditions and camera quality.
     * Start with 0.60 and adjust based on testing.
     */
    const val SIMILARITY_THRESHOLD = 0.90f

    /**
     * Find the best matching employee for a given face embedding.
     *
     * @param probeEmbedding The embedding vector extracted from the camera frame.
     * @param employees      All registered employees from the database.
     * @return [MatchResult] containing the best match (if any) and score.
     */
    fun findBestMatch(
        probeEmbedding: FloatArray,
        employees: List<EmployeeEntity>
    ): MatchResult {
        if (employees.isEmpty()) {
            return MatchResult(employee = null, score = 0f, isMatch = false)
        }

        var bestEmployee: EmployeeEntity? = null
        var bestScore = -1f // Cosine similarity ranges [-1, 1], so -1 is a safe floor

        for (employee in employees) {
            if (employee.faceVectors.isNullOrEmpty()) {
                continue
            }

            // ── Compute similarity between probe and all stored vectors for this employee ──
            var bestScoreForEmployee = -1f
            for (vector in employee.faceVectors) {
                if (probeEmbedding.size != vector.size) continue
                
                val score = VectorMath.cosineSimilarity(probeEmbedding, vector)
                if (score > bestScoreForEmployee) {
                    bestScoreForEmployee = score
                }
            }

            if (bestScoreForEmployee > bestScore) {
                bestScore = bestScoreForEmployee
                bestEmployee = employee
            }
        }

        return MatchResult(
            employee = bestEmployee,
            score = bestScore,
            isMatch = bestScore >= SIMILARITY_THRESHOLD
        )
    }
}
