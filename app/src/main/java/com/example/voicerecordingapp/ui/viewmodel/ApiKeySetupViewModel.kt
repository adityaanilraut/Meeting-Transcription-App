package com.example.voicerecordingapp.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.voicerecordingapp.data.repository.ApiKeyRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ApiKeySetupViewModel @Inject constructor(
    private val apiKeyRepository: ApiKeyRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(ApiKeySetupUiState())
    val uiState: StateFlow<ApiKeySetupUiState> = _uiState.asStateFlow()
    
    fun saveApiKey(apiKey: String) {
        if (apiKey.isBlank()) {
            _uiState.value = _uiState.value.copy(
                errorMessage = "API key cannot be empty"
            )
            return
        }
        
        if (!isValidApiKeyFormat(apiKey)) {
            _uiState.value = _uiState.value.copy(
                errorMessage = "Invalid API key format. Please check your key."
            )
            return
        }
        
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = "")
            
            try {
                apiKeyRepository.saveApiKey(apiKey)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    isApiKeySaved = true
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = "Failed to save API key: ${e.message}"
                )
            }
        }
    }
    
    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = "")
    }
    
    private fun isValidApiKeyFormat(apiKey: String): Boolean {
        // Basic validation for OpenAI API key format
        return apiKey.startsWith("sk-") && apiKey.length > 20
    }
}

data class ApiKeySetupUiState(
    val isLoading: Boolean = false,
    val isApiKeySaved: Boolean = false,
    val errorMessage: String = ""
)
