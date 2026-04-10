package com.dishrecognition.data.local.db

import androidx.room.*
import com.dishrecognition.data.local.entity.DishEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DishDao {
    @Query("SELECT * FROM dishes ORDER BY updatedAt DESC")
    fun getAllDishes(): Flow<List<DishEntity>>

    @Query("SELECT * FROM dishes WHERE id = :id")
    suspend fun getDishById(id: Long): DishEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDish(dish: DishEntity): Long

    @Update
    suspend fun updateDish(dish: DishEntity)

    @Delete
    suspend fun deleteDish(dish: DishEntity)

    @Query("SELECT COUNT(*) FROM dishes")
    suspend fun getDishCount(): Int
}
