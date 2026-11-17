package zouitel.neighborhood.ai.embedding

/**
 * Similarity operations for vectors in the same embedding space.
 */
object EmbeddingSimilarity {

    /**
     * Returns cosine similarity in the range [-1, 1].
     *
     * Vectors must come from the same model revision and have equal dimensions.
     * A zero-magnitude vector has no direction, so its similarity is defined as 0.
     */
    fun cosine(left: EmbeddingVector, right: EmbeddingVector): Float {
        require(left.modelId == right.modelId) {
            "Cannot compare embeddings from different models: ${left.modelId} and ${right.modelId}."
        }
        require(left.dimensions == right.dimensions) {
            "Embedding dimensions differ: ${left.dimensions} and ${right.dimensions}."
        }

        var dot = 0.0
        var leftMagnitude = 0.0
        var rightMagnitude = 0.0

        for (index in 0 until left.dimensions) {
            val a = left[index].toDouble()
            val b = right[index].toDouble()
            dot += a * b
            leftMagnitude += a * a
            rightMagnitude += b * b
        }

        if (leftMagnitude == 0.0 || rightMagnitude == 0.0) return 0f

        return (dot / (kotlin.math.sqrt(leftMagnitude) * kotlin.math.sqrt(rightMagnitude)))
            .coerceIn(-1.0, 1.0)
            .toFloat()
    }
}
