package com.dishrecognition.domain.model

data class NutritionInfo(
    val dishId: Long,
    val calories: Float,
    val fat: Float,
    val protein: Float,
    val carbohydrate: Float,
    val vitamin: Float,
    val sodium: Float
) {
    fun calculateForWeight(weight: Float): NutritionInfo {
        val factor = weight / 100f
        return NutritionInfo(
            dishId = dishId,
            calories = calories * factor,
            fat = fat * factor,
            protein = protein * factor,
            carbohydrate = carbohydrate * factor,
            vitamin = vitamin * factor,
            sodium = sodium * factor
        )
    }
}
