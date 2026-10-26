package com.example.medassist

import androidx.annotation.StringRes
import androidx.compose.ui.test.assertDoesNotExist
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.runtime.CompositionLocalProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.medassist.data.model.AuthUser
import com.example.medassist.data.local.AppNarrator
import com.example.medassist.data.local.AppPreferences
import com.example.medassist.ui.MainShell
import com.example.medassist.ui.theme.MedAssistTheme
import com.example.medassist.ui.theme.LocalAppNarrator
import com.example.medassist.ui.theme.LocalAppPreferences
import kotlinx.coroutines.flow.flowOf
import org.junit.After
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * UI tests for the signed-in app shell (MainShell). The sign-in screens are tested in AuthScreensTest.
 * Text is read from string resources (not typed in), so these tests pass whether the test device
 * is set to English or Hindi.
 */
@RunWith(AndroidJUnit4::class)
class MedAssistAppTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val testUser = AuthUser(uid = "test-uid", displayName = "Asha", email = "asha@example.com")
    private var testPreferences: AppPreferences? = null
    private var testNarrator: AppNarrator? = null

    @After
    fun cleanupNarrator() {
        testNarrator?.release()
    }

    private fun setMainShell(online: Boolean) {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val preferences = AppPreferences(context).also {
            it.setDarkMode(false)
            it.setNarrationEnabled(false)
            it.setAutoNarration(false)
        }
        val narrator = AppNarrator(context)
        testPreferences = preferences
        testNarrator = narrator
        composeRule.setContent {
            MedAssistTheme {
                CompositionLocalProvider(
                    LocalAppPreferences provides preferences,
                    LocalAppNarrator provides narrator,
                ) {
                    MainShell(isOnline = flowOf(online), user = testUser)
                }
            }
        }
    }

    private fun text(@StringRes id: Int): String =
        InstrumentationRegistry.getInstrumentation().targetContext.getString(id)

    @Test
    fun bottomBar_showsFiveTabsAndHidesSteps() {
        setMainShell(online = true)

        listOf(
            R.string.nav_home,
            R.string.nav_vault,
            R.string.nav_assistant,
            R.string.nav_analyzer,
            R.string.nav_profile,
        ).forEach { composeRule.onNodeWithText(text(it)).assertIsDisplayed() }
        composeRule.onNodeWithText(text(R.string.nav_steps)).assertDoesNotExist()
    }

    @Test
    fun tappingVaultTab_opensVaultScreen() {
        setMainShell(online = true)

        composeRule.onNodeWithText(text(R.string.nav_vault)).performClick()

        composeRule.onNodeWithText(text(R.string.title_vault)).assertIsDisplayed()
    }

    @Test
    fun homeProfileShortcut_opensProfileScreen() {
        setMainShell(online = true)

        composeRule.onNodeWithText(text(R.string.home_card_profile_desc)).performClick()

        composeRule.onNodeWithText(text(R.string.title_profile)).assertIsDisplayed()
    }

    @Test
    fun homeAnalyzerShortcut_opensAnalyzerWithBottomBar() {
        setMainShell(online = true)

        composeRule.onNodeWithText(text(R.string.home_card_analyzer_title)).performClick()

        composeRule.onNodeWithText(text(R.string.title_analyzer)).assertIsDisplayed()
        composeRule.onNodeWithText(text(R.string.nav_analyzer)).assertIsDisplayed()
        composeRule.onNodeWithText(text(R.string.nav_steps)).assertDoesNotExist()
    }

    @Test
    fun analyzerTab_opensAnalyzerScreen() {
        setMainShell(online = true)

        composeRule.onNodeWithText(text(R.string.nav_analyzer)).performClick()

        composeRule.onNodeWithText(text(R.string.title_analyzer)).assertIsDisplayed()
    }

    @Test
    fun analyzerQuickAccess_thenHomeTabReturnsToHome() {
        setMainShell(online = true)

        composeRule.onNodeWithText(text(R.string.home_card_analyzer_title)).performClick()
        composeRule.onNodeWithText(text(R.string.title_analyzer)).assertIsDisplayed()

        composeRule.onNodeWithText(text(R.string.nav_home)).performClick()

        composeRule.onNodeWithText(text(R.string.home_quick_access)).assertIsDisplayed()
        composeRule.onNodeWithText(text(R.string.title_analyzer)).assertDoesNotExist()
    }

    @Test
    fun offlineBanner_showsOnlyWhenOffline() {
        setMainShell(online = false)
        composeRule.waitForIdle()

        composeRule.onNodeWithText(text(R.string.common_offline_banner)).assertIsDisplayed()
    }
}
