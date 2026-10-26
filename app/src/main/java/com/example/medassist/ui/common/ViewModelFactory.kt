package com.example.medassist.ui.common

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.medassist.AppContainer
import com.example.medassist.MedAssistApplication

/**
 * Builds a ViewModel with the shared [AppContainer] handed to it. Use it in a screen like:
 *
 *     val vm: LoginViewModel = viewModel(factory = containerViewModelFactory { LoginViewModel(it.authRepository) })
 */
inline fun <reified VM : ViewModel> containerViewModelFactory(
    crossinline create: (AppContainer) -> VM,
): ViewModelProvider.Factory = viewModelFactory {
    initializer {
        val app = this[APPLICATION_KEY] as MedAssistApplication
        create(app.container)
    }
}
