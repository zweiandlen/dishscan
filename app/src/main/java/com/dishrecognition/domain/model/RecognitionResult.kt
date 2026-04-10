package com.dishrecognition.domain.model

data class RecognitionResult(
    val dish: Dish?,
    val nutritionInfo: NutritionInfo?,
    val tablewareType: TablewareType,
    val detectedWeight: Float,
    val confidence: Float
)
