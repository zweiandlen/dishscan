package com.dishrecognition.domain.repository

import com.dishrecognition.domain.model.Dish
import kotlinx.coroutines.flow.Flow

interface DishRepository {
    fun getAllDishes(): Flow<List<Dish>>
    suspend fun getDishById(id: Long): Dish?
    suspend fun insertDish(dish: Dish): Long
    suspend fun updateDish(dish: Dish)
    suspend fun deleteDish(dish: Dish)
    suspend fun getAllDishVectors(): List<Pair<Long, FloatArray>>
}
