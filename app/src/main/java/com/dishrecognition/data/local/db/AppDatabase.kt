package com.dishrecognition.data.local.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.dishrecognition.data.local.entity.DishEntity
import com.dishrecognition.data.local.entity.NutritionEntity
import com.dishrecognition.data.local.entity.TablewareMappingEntity

@Database(
    entities = [
        DishEntity::class,
        NutritionEntity::class,
        TablewareMappingEntity::class
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun dishDao(): DishDao
    abstract fun nutritionDao(): NutritionDao
    abstract fun tablewareMappingDao(): TablewareMappingDao
}
