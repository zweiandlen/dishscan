package com.dishrecognition.data.local

import com.dishrecognition.data.local.db.DishDao
import com.dishrecognition.data.local.entity.DishEntity
import com.dishrecognition.domain.model.Dish
import com.dishrecognition.domain.repository.DishRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DishRepositoryImpl @Inject constructor(
    private val dishDao: DishDao
) : DishRepository {

    override fun getAllDishes(): Flow<List<Dish>> {
        return dishDao.getAllDishes().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun getDishById(id: Long): Dish? {
        return dishDao.getDishById(id)?.toDomain()
    }

    override suspend fun insertDish(dish: Dish): Long {
        return dishDao.insertDish(dish.toEntity())
    }

    override suspend fun updateDish(dish: Dish) {
        dishDao.updateDish(dish.toEntity())
    }

    override suspend fun deleteDish(dish: Dish) {
        dishDao.deleteDish(dish.toEntity())
    }

    override suspend fun getAllDishVectors(): List<Pair<Long, FloatArray>> {
        val dishes = mutableListOf<Pair<Long, FloatArray>>()
        dishDao.getAllDishes().collect { entities ->
            entities.forEach { entity ->
                dishes.add(entity.id to entity.featureVector)
            }
        }
        return dishes
    }

    private fun DishEntity.toDomain(): Dish {
        return Dish(
            id = id,
            name = name,
            featureVector = featureVector,
            imagePaths = imagePaths,
            createdAt = createdAt,
            updatedAt = updatedAt
        )
    }

    private fun Dish.toEntity(): DishEntity {
        return DishEntity(
            id = id,
            name = name,
            featureVector = featureVector,
            imagePaths = imagePaths,
            createdAt = createdAt,
            updatedAt = updatedAt
        )
    }
}
