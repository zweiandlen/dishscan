package com.dishrecognition.data.ml

import com.dishrecognition.domain.model.Dish
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.sqrt

@Singleton
class VectorSearchEngine @Inject constructor() {
    
    companion object {
        private const val MATCHING_THRESHOLD = 0.75f
    }

    fun findSimilarDishes(
        queryVector: FloatArray,
        dishes: List<Dish>,
        topK: Int = 5
    ): List<Pair<Dish, Float>> {
        val similarities = dishes.map { dish ->
            dish to cosineSimilarity(queryVector, dish.featureVector)
        }
        
        return similarities
            .filter { (_, similarity) -> similarity >= MATCHING_THRESHOLD }
            .sortedByDescending { (_, similarity) -> similarity }
            .take(topK)
    }

    fun cosineSimilarity(vecA: FloatArray, vecB: FloatArray): Float {
        if (vecA.size != vecB.size) return 0f
        
        var dotProduct = 0f
        var normA = 0f
        var normB = 0f
        
        for (i in vecA.indices) {
            dotProduct += vecA[i] * vecB[i]
            normA += vecA[i] * vecA[i]
            normB += vecB[i] * vecB[i]
        }
        
        normA = sqrt(normA)
        normB = sqrt(normB)
        
        if (normA == 0f || normB == 0f) return 0f
        
        return dotProduct / (normA * normB)
    }
}
