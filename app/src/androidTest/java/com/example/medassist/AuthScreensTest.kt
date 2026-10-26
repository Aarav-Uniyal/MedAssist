package com.example.medassist

import androidx.annotation.StringRes
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.runtime.CompositionLocalProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.medassist.data.repository.AuthError
import com.example.medassist.data.local.AppNarrator
import com.example.medassist.data.local.AppPreferences
import com.example.medassist.domain.validation.FieldError
import com.example.medassist.ui.auth.LoginContent
import com.example.medassist.ui.auth.LoginUiState
import com.example.medassist.ui.theme.MedAssistTheme
import com.example.medassist.ui.theme.LocalAppNarrator
import com.example.medassist.ui.theme.LocalAppPreferences
import org.junit.Assert.assertEquals
import org.junit.After
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Tests the stateless sign-in UI directly, so no Firebase account or network is needed.
 * (The end-to-end flow against real Firebase is in the manual test checklist in the README.)
 */
@RunWith(AndroidJUnit4::class)
class AuthScreensTest {

    @get:Rule
    val composeRule = createComposeRule()
    private var testNarrator: AppNarrator? = null

    @After
    fun cleanupNarrator() {
        testNarrator?.release()
    }

    private fun text(@StringRes id: Int): String =
        InstrumentationRegistry.getInstrumentation().targetContext.getString(id)

    private fun setLogin(
        state: LoginUiState = LoginUiState(),
        offline: Boolean = false,
        onSubmit: () -> Unit = {},
    ) {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val preferences = AppPreferences(context).also { it.setNarrationEnabled(false) }
        val narrator = AppNarrator(context)
        testNarrator = narrator
        composeRule.setContent {
            MedAssistTheme {
                CompositionLocalProvider(
                    LocalAppPreferences provides preferences,
                    LocalAppNarrator provides narrator,
                ) {
                    LoginContent(
                        state = state,
                        offline = offline,
                        onEmailChange = {},
                        onPasswordChange = {},
                        onTogglePasswordVisible = {},
                        onSubmit = onSubmit,
                        onForgotPassword = {},
                        onRegister = {},
                    )
                }
            }
        }
    }

    @Test
    fun login_showsFieldsAndActions() {
        setLogin()

        composeRule.onNodeWithText(text(R.string.login_title)).assertIsDisplayed()
        composeRule.onNodeWithText(text(R.string.field_email)).assertIsDisplayed()
        composeRule.onNodeWithText(text(R.string.field_password)).assertIsDisplayed()
        composeRule.onNodeWithText(text(R.string.action_sign_in)).assertIsDisplayed()
        composeRule.onNodeWithText(text(R.string.action_forgot_password)).assertIsDisplayed()
    }

    @Test
    fun login_showsFieldErrorsAsText() {
        setLogin(LoginUiState(emailError = FieldError.EMAIL_INVALID, passwordError = FieldError.PASSWORD_EMPTY))

        composeRule.onNodeWithText(text(R.string.error_email_invalid)).assertIsDisplayed()
        composeRule.onNodeWithText(text(R.string.error_password_empty)).assertIsDisplayed()
    }

    @Test
    fun login_showsServiceErrorBanner() {
        setLogin(LoginUiState(error = AuthError.INVALID_CREDENTIALS))

        composeRule.onNodeWithText(text(R.string.auth_error_invalid_credentials)).assertIsDisplayed()
    }

    @Test
    fun login_signInButton_callsOnSubmit() {
        var submitted = 0
        setLogin(onSubmit = { submitted++ })

        composeRule.onNodeWithText(text(R.string.action_sign_in)).performClick()

        assertEquals(1, submitted)
    }

    @Test
    fun login_ignoresTapsWhileLoading() {
        var submitted = 0
        setLogin(LoginUiState(isLoading = true), onSubmit = { submitted++ })

        composeRule.onNodeWithText(text(R.string.action_sign_in)).performClick()

        assertEquals(0, submitted)
    }

    @Test
    fun login_showsOfflineBanner_whenOffline() {
        setLogin(offline = true)

        composeRule.onNodeWithText(text(R.string.common_offline_banner)).assertIsDisplayed()
    }
}
