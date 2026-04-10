package com.dishrecognition.domain.usecase

import android.graphics.Bitmap
import com.dishrecognition.data.ml.FeatureExtractorModel
import com.dishrecognition.data.ml.VectorSearchEngine
import com.dishrecognition.domain.model.Dish
import com.dishrecognition.domain.model.NutritionInfo
import com.dishrecognition.domain.model.RecognitionResult
import com.dishrecognition.domain.model.TablewareType
import com.dishrecognition.domain.repository.DishRepository
import com.dishrecognition.domain.repository.NutritionRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import javax.inject.Inject

class RecognitionUseCase @Inject constructor(
    private val dishRepository: DishRepository,
    private val nutritionRepository: NutritionRepository,
    private val featureExtractor: FeatureExtractorModel,
    private val vectorSearchEngine: VectorSearchEngine
) {
    suspend fun recognizeDish(bitmap: Bitmap): RecognitionResult = withContext(Dispatchers.Default) {
        val featureVector = featureExtractor.extractFeaturesFromBitmap(bitmap)
        val dishes = dishRepository.getAllDishes().first()
        
        val similarDishes = vectorSearchEngine.findSimilarDishes(featureVector, dishes)
        
        if (similarDishes.isEmpty()) {
            return@withContext RecognitionResult(
                dish = null,
                nutritionInfo = null,
                tablewareType = TablewareType.MEDIUM_BOWL,
                detectedWeight = TablewareType.MEDIUM_BOWL.defaultWeight,
                confidence = 0f
            )
        }
        
        val (matchedDish, confidence) = similarDishes.first()
        val nutritionInfo = nutritionRepository.getNutritionByDishId(matchedDish.id)
        val tablewareType = TablewareType.MEDIUM_BOWL
        
        RecognitionResult(
            dish = matchedDish,
            nutritionInfo = nutritionInfo,
            tablewareType = tablewareType,
            detectedWeight = tablewareType.defaultWeight,
            confidence = confidence
        )
    }
}
