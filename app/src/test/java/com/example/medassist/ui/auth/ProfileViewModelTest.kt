package com.example.medassist.ui.auth

import com.example.medassist.R
import com.example.medassist.data.repository.AuthError
import com.example.medassist.data.repository.AuthResult
import com.example.medassist.testutil.FakeAuthRepository
import com.example.medassist.testutil.MainDispatcherRule
import com.example.medassist.ui.settings.ProfileViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class ProfileViewModelTest {

    @get:Rule val mainDispatcher = MainDispatcherRule()

    private val repo = FakeAuthRepository()
    private val viewModel = ProfileViewModel(repo)

    @Test fun signOut_callsRepository() {
        viewModel.signOut()
        assertEquals(1, repo.signOutCalls)
    }

    @Test fun delete_requiresPassword_beforeAnythingIsDeleted() {
        viewModel.openDeleteDialog()
        viewModel.confirmDelete()

        assertEquals(R.string.error_password_empty, viewModel.state.value.errorRes)
        assertTrue(repo.deleteCalls.isEmpty())
    }

    @Test fun delete_withWrongPassword_showsSpecificMessage_andKeepsDialogOpen() {
        repo.nextResult = AuthResult.Failure(AuthError.INVALID_CREDENTIALS)
        viewModel.openDeleteDialog()
        viewModel.onPasswordChange("nope")
        viewModel.confirmDelete()

        val s = viewModel.state.value
        assertEquals(R.string.delete_error_wrong_password, s.errorRes)
        assertTrue(s.showDeleteDialog)
        assertFalse(s.isDeleting)
    }

    @Test fun delete_success_resetsState() {
        viewModel.openDeleteDialog()
        viewModel.onPasswordChange("abcd1234")
        viewModel.confirmDelete()

        assertEquals(listOf("abcd1234"), repo.deleteCalls)
        assertFalse(viewModel.state.value.showDeleteDialog)
        assertEquals("", viewModel.state.value.password) // password is not kept in memory
    }

    @Test fun cancel_closesDialog_andForgetsPassword() {
        viewModel.openDeleteDialog()
        viewModel.onPasswordChange("typed")
        viewModel.dismissDeleteDialog()

        assertFalse(viewModel.state.value.showDeleteDialog)
        assertEquals("", viewModel.state.value.password)
    }
}
