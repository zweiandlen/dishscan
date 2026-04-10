package com.dishrecognition.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "nutrition")
data class NutritionEntity(
    @PrimaryKey val dishId: Long,
    val calories: Float,
    val fat: Float,
    val protein: Float,
    val carbohydrate: Float,
    val vitamin: Float,
    val sodium: Float
)
