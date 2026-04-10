package com.dishrecognition.di

import android.content.Context
import androidx.room.Room
import com.dishrecognition.data.local.db.AppDatabase
import com.dishrecognition.data.local.db.DishDao
import com.dishrecognition.data.local.db.NutritionDao
import com.dishrecognition.data.local.db.TablewareMappingDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "dish_recognition_db"
        ).build()
    }

    @Provides
    fun provideDishDao(database: AppDatabase): DishDao {
        return database.dishDao()
    }

    @Provides
    fun provideNutritionDao(database: AppDatabase): NutritionDao {
        return database.nutritionDao()
    }

    @Provides
    fun provideTablewareMappingDao(database: AppDatabase): TablewareMappingDao {
        return database.tablewareMappingDao()
    }
}
