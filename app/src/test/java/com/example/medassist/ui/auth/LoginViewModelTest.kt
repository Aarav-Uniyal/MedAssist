package com.example.medassist.ui.auth

import com.example.medassist.data.repository.AuthError
import com.example.medassist.data.repository.AuthResult
import com.example.medassist.domain.validation.FieldError
import com.example.medassist.testutil.FakeAuthRepository
import com.example.medassist.testutil.MainDispatcherRule
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class LoginViewModelTest {

    @get:Rule val mainDispatcher = MainDispatcherRule()

    private val repo = FakeAuthRepository()
    private val viewModel = LoginViewModel(repo)

    @Test fun emptyForm_showsFieldErrors_andDoesNotCallFirebase() {
        viewModel.submit()

        val state = viewModel.state.value
        assertEquals(FieldError.EMAIL_EMPTY, state.emailError)
        assertEquals(FieldError.PASSWORD_EMPTY, state.passwordError)
        assertTrue(repo.signInCalls.isEmpty())
        assertFalse(state.isLoading)
    }

    @Test fun invalidEmail_isCaughtBeforeTheNetwork() {
        viewModel.onEmailChange("not-an-email")
        viewModel.onPasswordChange("whatever1")
        viewModel.submit()

        assertEquals(FieldError.EMAIL_INVALID, viewModel.state.value.emailError)
        assertTrue(repo.signInCalls.isEmpty())
    }

    @Test fun validSubmit_sendsTrimmedEmail_andClearsThePasswordOnSuccess() {
        viewModel.onEmailChange("  asha@example.com ")
        viewModel.onPasswordChange("secret123")
        viewModel.submit()

        assertEquals(listOf("asha@example.com" to "secret123"), repo.signInCalls)
        assertEquals("", viewModel.state.value.password) // not kept around after signing in
    }

    @Test fun wrongCredentials_showsError_andStopsLoading() {
        repo.nextResult = AuthResult.Failure(AuthError.INVALID_CREDENTIALS)
        viewModel.onEmailChange("asha@example.com")
        viewModel.onPasswordChange("wrong")
        viewModel.submit()

        val state = viewModel.state.value
        assertEquals(AuthError.INVALID_CREDENTIALS, state.error)
        assertFalse(state.isLoading)
        assertEquals("asha@example.com", state.email) // the user does not have to retype it
    }

    @Test fun editingAField_clearsTheOldErrors() {
        repo.nextResult = AuthResult.Failure(AuthError.NETWORK)
        viewModel.onEmailChange("asha@example.com")
        viewModel.onPasswordChange("pw")
        viewModel.submit()
        assertEquals(AuthError.NETWORK, viewModel.state.value.error)

        viewModel.onPasswordChange("pw2")
        assertNull(viewModel.state.value.error)
    }

    @Test fun passwordVisibility_toggles() {
        assertFalse(viewModel.state.value.passwordVisible)
        viewModel.togglePasswordVisible()
        assertTrue(viewModel.state.value.passwordVisible)
    }
}
