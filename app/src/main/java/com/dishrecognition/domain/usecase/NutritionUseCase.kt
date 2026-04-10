package com.dishrecognition.domain.usecase

import com.dishrecognition.domain.model.NutritionInfo
import com.dishrecognition.domain.model.TablewareType
import com.dishrecognition.domain.repository.NutritionRepository
import javax.inject.Inject

class NutritionUseCase @Inject constructor(
    private val nutritionRepository: NutritionRepository
) {
    suspend fun getNutritionInfo(
        dishId: Long,
        tablewareType: TablewareType
    ): NutritionInfo? {
        val baseNutrition = nutritionRepository.getNutritionByDishId(dishId)
        return baseNutrition?.calculateForWeight(tablewareType.defaultWeight)
    }
}
