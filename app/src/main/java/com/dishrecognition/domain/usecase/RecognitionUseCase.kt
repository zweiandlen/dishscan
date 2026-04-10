package com.dishrecognition.domain.usecase

import android.graphics.Bitmap
import com.dishrecognition.data.ml.FeatureExtractorModel
import com.dishrecognition.data.ml.ObjectDetectionModel
import com.dishrecognition.data.ml.VectorSearchEngine
import com.dishrecognition.domain.model.BoundingBox
import com.dishrecognition.domain.model.Dish
import com.dishrecognition.domain.model.RecognitionResult
import com.dishrecognition.domain.model.TablewareType
import com.dishrecognition.domain.repository.DishRepository
import com.dishrecognition.domain.repository.NutritionRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import javax.inject.Inject

class RecognitionUseCase @Inject constructor(
    private val dishRepository: DishRepository,
    private val nutritionRepository: NutritionRepository,
    private val objectDetectionModel: ObjectDetectionModel,
    private val featureExtractor: FeatureExtractorModel,
    private val vectorSearchEngine: VectorSearchEngine
) {
    suspend fun recognizeDish(frame: Bitmap): RecognitionResult = withContext(Dispatchers.Default) {
        val detections = objectDetectionModel.detect(frame)
        
        val dishDetection = detections.find { it.label == "dish" }
        val tablewareDetection = detections.find { 
            it.label in listOf(
                "small_bowl", "medium_bowl", "large_bowl",
                "small_plate", "medium_plate", "large_plate", "long_plate"
            )
        }
        
        if (dishDetection == null) {
            return@withContext RecognitionResult(
                dish = null,
                nutritionInfo = null,
                tablewareType = tablewareDetection?.label?.toTablewareType() ?: TablewareType.MEDIUM_BOWL,
                detectedWeight = tablewareDetection?.label?.toTablewareType()?.defaultWeight ?: TablewareType.MEDIUM_BOWL.defaultWeight,
                confidence = 0f
            )
        }
        
        val dishBitmap = cropBitmap(frame, dishDetection.boundingBox)
        
        val featureVector = featureExtractor.extractFeaturesFromBitmap(dishBitmap)
        
        val dishes = dishRepository.getAllDishes().first()
        
        if (dishes.isEmpty()) {
            return@withContext RecognitionResult(
                dish = null,
                nutritionInfo = null,
                tablewareType = tablewareDetection?.label?.toTablewareType() ?: TablewareType.MEDIUM_BOWL,
                detectedWeight = tablewareDetection?.label?.toTablewareType()?.defaultWeight ?: TablewareType.MEDIUM_BOWL.defaultWeight,
                confidence = 0f
            )
        }
        
        val similarDishes = vectorSearchEngine.findSimilarDishes(featureVector, dishes)
        
        val tablewareType = tablewareDetection?.label?.toTablewareType() ?: TablewareType.MEDIUM_BOWL
        
        if (similarDishes.isEmpty()) {
            return@withContext RecognitionResult(
                dish = null,
                nutritionInfo = null,
                tablewareType = tablewareType,
                detectedWeight = tablewareType.defaultWeight,
                confidence = 0f
            )
        }
        
        val (matchedDish, confidence) = similarDishes.first()
        val nutritionInfo = nutritionRepository.getNutritionByDishId(matchedDish.id)
        
        RecognitionResult(
            dish = matchedDish,
            nutritionInfo = nutritionInfo,
            tablewareType = tablewareType,
            detectedWeight = tablewareType.defaultWeight,
            confidence = confidence
        )
    }
    
    private fun cropBitmap(source: Bitmap, box: BoundingBox): Bitmap {
        val left = (box.left * source.width).toInt().coerceIn(0, source.width - 1)
        val top = (box.top * source.height).toInt().coerceIn(0, source.height - 1)
        val width = ((box.right - box.left) * source.width).toInt().coerceIn(1, source.width - left)
        val height = ((box.bottom - box.top) * source.height).toInt().coerceIn(1, source.height - top)
        
        return Bitmap.createBitmap(source, left, top, width, height)
    }
    
    private fun String.toTablewareType(): TablewareType {
        return when (this) {
            "small_bowl" -> TablewareType.SMALL_BOWL
            "medium_bowl" -> TablewareType.MEDIUM_BOWL
            "large_bowl" -> TablewareType.LARGE_BOWL
            "small_plate" -> TablewareType.SMALL_PLATE
            "medium_plate" -> TablewareType.MEDIUM_PLATE
            "large_plate" -> TablewareType.LARGE_PLATE
            "long_plate" -> TablewareType.LONG_PLATE
            else -> TablewareType.MEDIUM_BOWL
        }
    }
}
