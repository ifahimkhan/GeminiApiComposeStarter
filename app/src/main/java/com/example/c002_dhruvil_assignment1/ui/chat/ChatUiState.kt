package com.example.c002_dhruvil_assignment1.ui.chat

sealed interface ChatUiState {
    data object Initial : ChatUiState
    data object Loading : ChatUiState
    data class Success(val responseText: String) : ChatUiState
    data class Error(val errorMessage: String) : ChatUiState
}
