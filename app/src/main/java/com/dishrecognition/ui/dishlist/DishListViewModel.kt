package com.dishrecognition.ui.dishlist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dishrecognition.domain.model.Dish
import com.dishrecognition.domain.repository.DishRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DishListUiState(
    val dishes: List<Dish> = emptyList(),
    val isLoading: Boolean = true
)

@HiltViewModel
class DishListViewModel @Inject constructor(
    private val dishRepository: DishRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(DishListUiState())
    val uiState: StateFlow<DishListUiState> = _uiState.asStateFlow()

    init {
        loadDishes()
    }

    private fun loadDishes() {
        viewModelScope.launch {
            dishRepository.getAllDishes().collect { dishes ->
                _uiState.value = DishListUiState(
                    dishes = dishes,
                    isLoading = false
                )
            }
        }
    }

    fun deleteDish(dish: Dish) {
        viewModelScope.launch {
            dishRepository.deleteDish(dish)
        }
    }
}
