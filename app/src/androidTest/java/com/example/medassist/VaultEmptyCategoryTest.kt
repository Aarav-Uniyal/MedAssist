package com.example.medassist

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.medassist.data.local.AppNarrator
import com.example.medassist.data.local.AppPreferences
import com.example.medassist.data.model.DocumentCategory
import com.example.medassist.ui.common.UiState
import com.example.medassist.ui.theme.LocalAppNarrator
import com.example.medassist.ui.theme.LocalAppPreferences
import com.example.medassist.ui.theme.MedAssistTheme
import com.example.medassist.ui.vault.VaultContent
import com.example.medassist.ui.vault.VaultControls
import com.example.medassist.ui.vault.VaultList
import org.junit.After
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class VaultEmptyCategoryTest {
    @get:Rule
    val composeRule = createComposeRule()

    private var narrator: AppNarrator? = null

    @After
    fun releaseNarrator() {
        narrator?.release()
    }

    @Test
    fun selectingCategoryWithNoDocumentsShowsHelpfulEmptyState() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val preferences = AppPreferences(context).also {
            it.setNarrationEnabled(false)
        }
        val appNarrator = AppNarrator(context)
        narrator = appNarrator
        val snackbar = SnackbarHostState()
        val selectedCategory = mutableStateOf<DocumentCategory?>(null)

        composeRule.setContent {
            MedAssistTheme {
                CompositionLocalProvider(
                    LocalAppPreferences provides preferences,
                    LocalAppNarrator provides appNarrator,
                ) {
                    VaultContent(
                        list = UiState.Success(
                            VaultList(documents = emptyList(), hasAnyDocuments = true),
                        ),
                        controls = VaultControls(category = selectedCategory.value),
                        isReadingFile = false,
                        snackbar = snackbar,
                        onUploadClick = {},
                        onRetry = {},
                        onSearchChange = {},
                        onCategoryFilter = { selectedCategory.value = it },
                        onSortChange = {},
                        onOpen = {},
                        onEdit = {},
                        onDelete = {},
                    )
                }
            }
        }

        composeRule.onNodeWithText(context.getString(R.string.category_blood_tests)).performClick()
        composeRule.onNodeWithText(
            context.getString(R.string.vault_category_empty_title),
        ).assertIsDisplayed()
    }
}
