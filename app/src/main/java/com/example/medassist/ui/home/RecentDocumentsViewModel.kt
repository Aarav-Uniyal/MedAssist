package com.example.medassist.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.medassist.data.model.MedicalDocument
import com.example.medassist.data.repository.VaultRepository
import com.example.medassist.data.repository.VaultResult
import com.example.medassist.ui.common.UiState
import com.example.medassist.ui.vault.messageRes
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

/** The few newest vault documents for the Home screen. */
class RecentDocumentsViewModel(repository: VaultRepository) : ViewModel() {
    val recent: StateFlow<UiState<List<MedicalDocument>>> = repository.observeDocuments()
        .map { result ->
            when (result) {
                is VaultResult.Success ->
                    if (result.value.isEmpty()) UiState.Empty else UiState.Success(result.value.take(RECENT_COUNT))
                is VaultResult.Failure -> UiState.Error(result.error.messageRes())
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), UiState.Loading)

    companion object {
        const val RECENT_COUNT = 3
    }
}
