package com.dishrecognition.ui.nutrition

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dishrecognition.domain.model.Dish
import com.dishrecognition.domain.model.NutritionInfo
import com.dishrecognition.domain.repository.DishRepository
import com.dishrecognition.domain.repository.NutritionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

data class NutritionSummary(
    val dish: Dish,
    val nutritionInfo: NutritionInfo
)

data class NutritionUiState(
    val summaries: List<NutritionSummary> = emptyList(),
    val isLoading: Boolean = true,
    val totalNutrition: NutritionInfo? = null
)

@HiltViewModel
class NutritionViewModel @Inject constructor(
    private val dishRepository: DishRepository,
    private val nutritionRepository: NutritionRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(NutritionUiState())
    val uiState: StateFlow<NutritionUiState> = _uiState.asStateFlow()

    init {
        loadNutritionData()
    }

    private fun loadNutritionData() {
        viewModelScope.launch {
            val dishes = dishRepository.getAllDishes().first()
            val summaries = mutableListOf<NutritionSummary>()
            
            for (dish in dishes) {
                val nutrition = nutritionRepository.getNutritionByDishId(dish.id)
                nutrition?.let {
                    summaries.add(NutritionSummary(dish, it))
                }
            }
            
            val totalNutrition = if (summaries.isNotEmpty()) {
                NutritionInfo(
                    dishId = 0,
                    calories = summaries.sumOf { it.nutritionInfo.calories.toDouble() }.toFloat(),
                    fat = summaries.sumOf { it.nutritionInfo.fat.toDouble() }.toFloat(),
                    protein = summaries.sumOf { it.nutritionInfo.protein.toDouble() }.toFloat(),
                    carbohydrate = summaries.sumOf { it.nutritionInfo.carbohydrate.toDouble() }.toFloat(),
                    vitamin = summaries.sumOf { it.nutritionInfo.vitamin.toDouble() }.toFloat(),
                    sodium = summaries.sumOf { it.nutritionInfo.sodium.toDouble() }.toFloat()
                )
            } else null
            
            _uiState.value = NutritionUiState(
                summaries = summaries,
                isLoading = false,
                totalNutrition = totalNutrition
            )
        }
    }
}
