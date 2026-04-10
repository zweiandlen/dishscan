package com.dishrecognition.di

import com.dishrecognition.data.local.DishRepositoryImpl
import com.dishrecognition.data.local.NutritionRepositoryImpl
import com.dishrecognition.domain.repository.DishRepository
import com.dishrecognition.domain.repository.NutritionRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindDishRepository(impl: DishRepositoryImpl): DishRepository

    @Binds
    @Singleton
    abstract fun bindNutritionRepository(impl: NutritionRepositoryImpl): NutritionRepository
}
