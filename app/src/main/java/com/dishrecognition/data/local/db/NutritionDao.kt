package com.dishrecognition.data.local.db

import androidx.room.*
import com.dishrecognition.data.local.entity.NutritionEntity

@Dao
interface NutritionDao {
    @Query("SELECT * FROM nutrition WHERE dishId = :dishId")
    suspend fun getNutritionByDishId(dishId: Long): NutritionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNutrition(nutrition: NutritionEntity)

    @Update
    suspend fun updateNutrition(nutrition: NutritionEntity)

    @Delete
    suspend fun deleteNutrition(nutrition: NutritionEntity)
}
