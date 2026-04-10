package com.dishrecognition.data.local

import com.dishrecognition.data.local.db.NutritionDao
import com.dishrecognition.data.local.entity.NutritionEntity
import com.dishrecognition.domain.model.NutritionInfo
import com.dishrecognition.domain.repository.NutritionRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NutritionRepositoryImpl @Inject constructor(
    private val nutritionDao: NutritionDao
) : NutritionRepository {

    override suspend fun getNutritionByDishId(dishId: Long): NutritionInfo? {
        return nutritionDao.getNutritionByDishId(dishId)?.toDomain()
    }

    override suspend fun insertNutrition(nutrition: NutritionInfo) {
        nutritionDao.insertNutrition(nutrition.toEntity())
    }

    override suspend fun updateNutrition(nutrition: NutritionInfo) {
        nutritionDao.updateNutrition(nutrition.toEntity())
    }

    private fun NutritionEntity.toDomain(): NutritionInfo {
        return NutritionInfo(
            dishId = dishId,
            calories = calories,
            fat = fat,
            protein = protein,
            carbohydrate = carbohydrate,
            vitamin = vitamin,
            sodium = sodium
        )
    }

    private fun NutritionInfo.toEntity(): NutritionEntity {
        return NutritionEntity(
            dishId = dishId,
            calories = calories,
            fat = fat,
            protein = protein,
            carbohydrate = carbohydrate,
            vitamin = vitamin,
            sodium = sodium
        )
    }
}
