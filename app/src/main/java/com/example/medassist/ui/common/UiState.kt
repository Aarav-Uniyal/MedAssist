package com.example.medassist.ui.common

import androidx.annotation.StringRes

/**
 * The four situations every screen with data must handle. ViewModels expose one of these
 * as a StateFlow, and [com.example.medassist.ui.components.UiStateContent] draws it.
 *
 * Error messages are string-resource ids (not text), so ViewModels never need a Context
 * and the message is shown in whichever language the user chose.
 */
sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data object Empty : UiState<Nothing>
    data class Error(@StringRes val messageRes: Int) : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
}
