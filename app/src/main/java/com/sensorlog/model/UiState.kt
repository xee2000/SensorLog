package com.sensorlog.model

/**
 * UI 상태를 나타내는 sealed class
 */
sealed class UiState<out T> {
    object Loading : UiState<Nothing>()
    data class Success<T>(val data: T) : UiState<T>()
    data class Error(val message: String, val code: Int? = null) : UiState<Nothing>()
    object Empty : UiState<Nothing>()
}
