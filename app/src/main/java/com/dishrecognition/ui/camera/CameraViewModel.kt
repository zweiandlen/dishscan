package com.dishrecognition.ui.camera

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dishrecognition.domain.model.RecognitionResult
import com.dishrecognition.domain.model.TablewareType
import com.dishrecognition.domain.usecase.RecognitionUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CameraUiState(
    val isProcessing: Boolean = false,
    val recognitionResult: RecognitionResult? = null,
    val error: String? = null
)

@HiltViewModel
class CameraViewModel @Inject constructor(
    private val recognitionUseCase: RecognitionUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(CameraUiState())
    val uiState: StateFlow<CameraUiState> = _uiState.asStateFlow()

    fun processFrame(bitmap: Bitmap) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isProcessing = true)
            try {
                val result = recognitionUseCase.recognizeDish(bitmap)
                _uiState.value = _uiState.value.copy(
                    isProcessing = false,
                    recognitionResult = result,
                    error = null
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isProcessing = false,
                    error = e.message
                )
            }
        }
    }

    fun updateTablewareType(type: TablewareType) {
        val currentResult = _uiState.value.recognitionResult ?: return
        _uiState.value = _uiState.value.copy(
            recognitionResult = currentResult.copy(
                tablewareType = type,
                detectedWeight = type.defaultWeight
            )
        )
    }
}
