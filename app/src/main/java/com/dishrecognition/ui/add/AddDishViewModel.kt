package com.dishrecognition.ui.add

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dishrecognition.domain.usecase.AddDishUseCase
import com.dishrecognition.domain.usecase.NutritionInput
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AddDishUiState(
    val capturedImages: List<Bitmap> = emptyList(),
    val dishName: String = "",
    val calories: String = "",
    val fat: String = "",
    val protein: String = "",
    val carbohydrate: String = "",
    val vitamin: String = "",
    val sodium: String = "",
    val isSaving: Boolean = false,
    val isSuccess: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class AddDishViewModel @Inject constructor(
    private val addDishUseCase: AddDishUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(AddDishUiState())
    val uiState: StateFlow<AddDishUiState> = _uiState.asStateFlow()

    fun addImage(bitmap: Bitmap) {
        val currentImages = _uiState.value.capturedImages.toMutableList()
        if (currentImages.size < 5) {
            currentImages.add(bitmap)
            _uiState.value = _uiState.value.copy(capturedImages = currentImages)
        }
    }

    fun removeImage(index: Int) {
        val currentImages = _uiState.value.capturedImages.toMutableList()
        if (index in currentImages.indices) {
            currentImages.removeAt(index)
            _uiState.value = _uiState.value.copy(capturedImages = currentImages)
        }
    }

    fun updateDishName(name: String) {
        _uiState.value = _uiState.value.copy(dishName = name)
    }

    fun updateCalories(value: String) {
        _uiState.value = _uiState.value.copy(calories = value)
    }

    fun updateFat(value: String) {
        _uiState.value = _uiState.value.copy(fat = value)
    }

    fun updateProtein(value: String) {
        _uiState.value = _uiState.value.copy(protein = value)
    }

    fun updateCarbohydrate(value: String) {
        _uiState.value = _uiState.value.copy(carbohydrate = value)
    }

    fun updateVitamin(value: String) {
        _uiState.value = _uiState.value.copy(vitamin = value)
    }

    fun updateSodium(value: String) {
        _uiState.value = _uiState.value.copy(sodium = value)
    }

    fun saveDish() {
        val state = _uiState.value
        
        if (state.dishName.isBlank()) {
            _uiState.value = state.copy(error = "请输入菜品名称")
            return
        }
        
        if (state.capturedImages.isEmpty()) {
            _uiState.value = state.copy(error = "请至少拍摄一张菜品照片")
            return
        }
        
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSaving = true, error = null)
            
            try {
                val nutritionInput = NutritionInput(
                    calories = state.calories.toFloatOrNull() ?: 0f,
                    fat = state.fat.toFloatOrNull() ?: 0f,
                    protein = state.protein.toFloatOrNull() ?: 0f,
                    carbohydrate = state.carbohydrate.toFloatOrNull() ?: 0f,
                    vitamin = state.vitamin.toFloatOrNull() ?: 0f,
                    sodium = state.sodium.toFloatOrNull() ?: 0f
                )
                
                addDishUseCase.addNewDish(
                    images = state.capturedImages,
                    name = state.dishName,
                    nutritionInfo = nutritionInput
                )
                
                _uiState.value = _uiState.value.copy(isSaving = false, isSuccess = true)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isSaving = false,
                    error = e.message ?: "保存失败"
                )
            }
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}
