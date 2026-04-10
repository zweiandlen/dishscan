package com.dishrecognition.domain.repository

import com.dishrecognition.domain.model.NutritionInfo

interface NutritionRepository {
    suspend fun getNutritionByDishId(dishId: Long): NutritionInfo?
    suspend fun insertNutrition(nutrition: NutritionInfo)
    suspend fun updateNutrition(nutrition: NutritionInfo)
}
