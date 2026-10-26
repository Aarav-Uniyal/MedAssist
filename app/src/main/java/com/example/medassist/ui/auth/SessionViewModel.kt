package com.example.medassist.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.medassist.data.model.Session
import com.example.medassist.data.repository.AuthRepository
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

/** Root-level ViewModel: exposes the sign-in state that decides which part of the app is shown. */
class SessionViewModel(private val repository: AuthRepository) : ViewModel() {

    val session: StateFlow<Session> = repository.session

    init {
        // Make sure every verified user has a profile document. Best effort: a failure here
        // (for example while offline) is logged by the repository and retried at the next sign-in.
        viewModelScope.launch {
            repository.session
                .map { (it as? Session.Verified)?.user?.uid }
                .distinctUntilChanged()
                .filterNotNull()
                .collect { repository.ensureProfile() }
        }
    }
}
