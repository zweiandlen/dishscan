package com.dishrecognition.domain.usecase

import android.graphics.Bitmap
import com.dishrecognition.data.ml.FeatureExtractorModel
import com.dishrecognition.domain.model.Dish
import com.dishrecognition.domain.model.NutritionInfo
import com.dishrecognition.domain.repository.DishRepository
import com.dishrecognition.domain.repository.NutritionRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

class AddDishUseCase @Inject constructor(
    private val dishRepository: DishRepository,
    private val nutritionRepository: NutritionRepository,
    private val featureExtractor: FeatureExtractorModel
) {
    suspend fun addNewDish(
        images: List<Bitmap>,
        name: String,
        nutritionInfo: NutritionInput
    ): Long = withContext(Dispatchers.Default) {
        if (images.isEmpty()) {
            throw IllegalArgumentException("At least one image is required")
        }
        
        val imagePaths = images.mapIndexed { index, bitmap ->
            saveBitmapToCache(bitmap, "dish_${System.currentTimeMillis()}_$index.jpg")
        }
        
        val featureVector = featureExtractor.extractFeaturesFromBitmap(images.first())
        
        val dish = Dish(
            name = name,
            featureVector = featureVector,
            imagePaths = imagePaths,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
        
        val dishId = dishRepository.insertDish(dish)
        
        val nutrition = NutritionInfo(
            dishId = dishId,
            calories = nutritionInfo.calories,
            fat = nutritionInfo.fat,
            protein = nutritionInfo.protein,
            carbohydrate = nutritionInfo.carbohydrate,
            vitamin = nutritionInfo.vitamin,
            sodium = nutritionInfo.sodium
        )
        nutritionRepository.insertNutrition(nutrition)
        
        dishId
    }
    
    private fun saveBitmapToCache(bitmap: Bitmap, filename: String): String {
        val file = java.io.File("/data/user/0/com.dishrecognition/cache/$filename")
        file.parentFile?.mkdirs()
        file.outputStream().use { out ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
        }
        return file.absolutePath
    }
}

data class NutritionInput(
    val calories: Float,
    val fat: Float,
    val protein: Float,
    val carbohydrate: Float,
    val vitamin: Float,
    val sodium: Float
)
