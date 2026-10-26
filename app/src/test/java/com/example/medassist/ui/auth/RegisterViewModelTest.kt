package com.example.medassist.ui.auth

import com.example.medassist.data.repository.AuthError
import com.example.medassist.data.repository.AuthResult
import com.example.medassist.domain.validation.FieldError
import com.example.medassist.testutil.FakeAuthRepository
import com.example.medassist.testutil.MainDispatcherRule
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class RegisterViewModelTest {

    @get:Rule val mainDispatcher = MainDispatcherRule()

    private val repo = FakeAuthRepository()
    private val viewModel = RegisterViewModel(repo)

    private fun fill(
        name: String = "Asha",
        email: String = "asha@example.com",
        password: String = "abcd1234",
        confirm: String = "abcd1234",
    ) {
        viewModel.onNameChange(name)
        viewModel.onEmailChange(email)
        viewModel.onPasswordChange(password)
        viewModel.onConfirmChange(confirm)
    }

    @Test fun emptyForm_reportsEveryField() {
        viewModel.submit()

        val s = viewModel.state.value
        assertEquals(FieldError.NAME_EMPTY, s.nameError)
        assertEquals(FieldError.EMAIL_EMPTY, s.emailError)
        assertEquals(FieldError.PASSWORD_EMPTY, s.passwordError)
        assertEquals(FieldError.CONFIRM_EMPTY, s.confirmError)
        assertTrue(repo.registerCalls.isEmpty())
    }

    @Test fun weakPassword_isRejectedLocally() {
        fill(password = "short1", confirm = "short1")
        viewModel.submit()

        assertEquals(FieldError.PASSWORD_TOO_SHORT, viewModel.state.value.passwordError)
        assertTrue(repo.registerCalls.isEmpty())
    }

    @Test fun mismatchedConfirmation_isRejectedLocally() {
        fill(confirm = "abcd9999")
        viewModel.submit()

        assertEquals(FieldError.PASSWORDS_MISMATCH, viewModel.state.value.confirmError)
        assertTrue(repo.registerCalls.isEmpty())
    }

    @Test fun validForm_registersWithTrimmedNameAndEmail() {
        fill(name = "  Asha Sharma ", email = " asha@example.com ")
        viewModel.submit()

        assertEquals(listOf(Triple("Asha Sharma", "asha@example.com", "abcd1234")), repo.registerCalls)
    }

    @Test fun emailAlreadyInUse_isShown_andFormIsKept() {
        repo.nextResult = AuthResult.Failure(AuthError.EMAIL_IN_USE)
        fill()
        viewModel.submit()

        val s = viewModel.state.value
        assertEquals(AuthError.EMAIL_IN_USE, s.error)
        assertEquals("asha@example.com", s.email)
        assertEquals(false, s.isLoading)
    }
}
