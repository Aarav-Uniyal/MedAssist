package com.example.medassist.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.medassist.R
import com.example.medassist.data.model.AuthUser
import com.example.medassist.ui.common.containerViewModelFactory
import com.example.medassist.ui.components.DangerButton
import com.example.medassist.ui.components.MedAssistCard
import com.example.medassist.ui.components.MedAssistTextField
import com.example.medassist.ui.components.MedAssistTopBar
import com.example.medassist.ui.components.SecondaryButton
import com.example.medassist.ui.theme.LocalAppNarrator
import com.example.medassist.ui.theme.LocalAppPreferences
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import java.util.Locale
import com.example.medassist.ui.theme.accessibleAquaTextColor
import com.example.medassist.ui.theme.Spacing

/** Profile and Settings. Phase 2 adds the account details, sign out and delete account. */
@Composable
fun ProfileScreen(user: AuthUser) {
    val viewModel: ProfileViewModel = viewModel(
        key = "profile:${user.uid}",
        factory = containerViewModelFactory { ProfileViewModel(it.authRepository) },
    )
    val state by viewModel.state.collectAsStateWithLifecycle()
    ProfileContent(
        user = user,
        state = state,
        onSignOut = viewModel::signOut,
        onDeleteClick = viewModel::openDeleteDialog,
        onDeletePasswordChange = viewModel::onPasswordChange,
        onTogglePasswordVisible = viewModel::togglePasswordVisible,
        onConfirmDelete = viewModel::confirmDelete,
        onDismissDelete = viewModel::dismissDeleteDialog,
    )
}

@Composable
fun ProfileContent(
    user: AuthUser,
    state: ProfileUiState,
    onSignOut: () -> Unit,
    onDeleteClick: () -> Unit,
    onDeletePasswordChange: (String) -> Unit,
    onTogglePasswordVisible: () -> Unit,
    onConfirmDelete: () -> Unit,
    onDismissDelete: () -> Unit,
) {
    val preferences = LocalAppPreferences.current
    val settings by preferences.settings.collectAsStateWithLifecycle()
    val narrator = LocalAppNarrator.current
    val narrationState by narrator.state.collectAsStateWithLifecycle()
    val configuration = LocalConfiguration.current
    val activeLanguage = AppCompatDelegate.getApplicationLocales().toLanguageTags()
        .ifBlank { configuration.locales[0]?.language ?: Locale.getDefault().language }
        .substringBefore(',').substringBefore('-')
    val sampleNarration = stringResource(R.string.a11y_speech_profile) + " " +
        stringResource(R.string.settings_narration_summary)
    val speechSpeedDescription = stringResource(R.string.settings_narration_speed, settings.narrationSpeed)
    val speechVolumeDescription = stringResource(R.string.settings_narration_volume, (settings.narrationVolume * 100).toInt())
    val localeTag = AppCompatDelegate.getApplicationLocales().toLanguageTags()
        .ifBlank { configuration.locales[0]?.toLanguageTag() ?: Locale.getDefault().toLanguageTag() }

    LaunchedEffect(settings.narrationEnabled) {
        if (!settings.narrationEnabled) narrator.stop()
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = { MedAssistTopBar(title = stringResource(R.string.title_profile), narrationText = stringResource(R.string.a11y_speech_profile)) },
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(Spacing.md),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Column(
                modifier = Modifier
                    .widthIn(max = 480.dp)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(Spacing.md),
            ) {
                Text(
                    text = stringResource(R.string.profile_account_section),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.semantics { heading() },
                )
                MedAssistCard(modifier = Modifier.fillMaxWidth()) {
                    AccountRow(
                        label = stringResource(R.string.profile_name_label),
                        value = user.displayName.ifBlank { stringResource(R.string.profile_name_unset) },
                    )
                    Spacer(Modifier.height(Spacing.md))
                    AccountRow(label = stringResource(R.string.profile_email_label), value = user.email)
                    Spacer(Modifier.height(Spacing.md))
                    // Words, not just a colour or icon, say the email is confirmed.
                    Text(
                        text = stringResource(R.string.profile_email_status_verified),
                        style = MaterialTheme.typography.bodyMedium,
                        color = accessibleAquaTextColor(),
                    )
                }

                Text(
                    text = stringResource(R.string.settings_accessibility_title),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.semantics { heading() },
                )
                MedAssistCard(modifier = Modifier.fillMaxWidth()) {
                    PreferenceSwitchRow(
                        title = stringResource(R.string.settings_dark_mode_title),
                        summary = stringResource(R.string.settings_dark_mode_summary),
                        checked = settings.darkMode,
                        onCheckedChange = preferences::setDarkMode,
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    Spacer(Modifier.height(Spacing.sm))
                    Text(stringResource(R.string.settings_language_title), style = MaterialTheme.typography.titleSmall)
                    Text(
                        stringResource(R.string.settings_language_summary),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    androidx.compose.foundation.layout.Row(
                        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                    ) {
                        FilterChip(
                            selected = activeLanguage == "en",
                            onClick = { AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags("en")) },
                            label = { Text(stringResource(R.string.settings_language_english)) },
                        )
                        FilterChip(
                            selected = activeLanguage == "hi",
                            onClick = { AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags("hi")) },
                            label = { Text(stringResource(R.string.settings_language_hindi)) },
                        )
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    Spacer(Modifier.height(Spacing.sm))
                    Text(stringResource(R.string.settings_narration_title), style = MaterialTheme.typography.titleSmall)
                    Text(
                        stringResource(R.string.settings_narration_summary),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    PreferenceSwitchRow(
                        title = stringResource(R.string.settings_narration_enabled),
                        checked = settings.narrationEnabled,
                        onCheckedChange = preferences::setNarrationEnabled,
                    )
                    PreferenceSwitchRow(
                        title = stringResource(R.string.settings_auto_narration),
                        checked = settings.autoNarration,
                        enabled = settings.narrationEnabled,
                        onCheckedChange = preferences::setAutoNarration,
                    )
                    Text(
                        speechSpeedDescription,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Slider(
                        value = settings.narrationSpeed,
                        onValueChange = preferences::setNarrationSpeed,
                        valueRange = 0.5f..2.0f,
                        steps = 5,
                        enabled = settings.narrationEnabled,
                        modifier = Modifier.semantics { contentDescription = speechSpeedDescription },
                    )
                    Text(
                        speechVolumeDescription,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Slider(
                        value = settings.narrationVolume,
                        onValueChange = preferences::setNarrationVolume,
                        valueRange = 0.0f..1.0f,
                        steps = 9,
                        enabled = settings.narrationEnabled,
                        modifier = Modifier.semantics { contentDescription = speechVolumeDescription },
                    )
                    androidx.compose.foundation.layout.Row(
                        horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        TextButton(
                            onClick = { narrator.speak(sampleNarration, localeTag, settings.narrationSpeed, settings.narrationVolume) },
                            enabled = settings.narrationEnabled,
                            modifier = Modifier.weight(1f),
                        ) { Text(stringResource(R.string.settings_narration_play)) }
                        TextButton(
                            onClick = narrator::pause,
                            enabled = settings.narrationEnabled && narrationState.isSpeaking,
                            modifier = Modifier.weight(1f),
                        ) { Text(stringResource(R.string.settings_narration_pause)) }
                    }
                    androidx.compose.foundation.layout.Row(
                        horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        TextButton(
                            onClick = narrator::resume,
                            enabled = settings.narrationEnabled && narrationState.isPaused,
                            modifier = Modifier.weight(1f),
                        ) { Text(stringResource(R.string.settings_narration_resume)) }
                        TextButton(
                            onClick = narrator::stop,
                            enabled = narrationState.isSpeaking || narrationState.isPaused,
                            modifier = Modifier.weight(1f),
                        ) { Text(stringResource(R.string.settings_narration_stop)) }
                    }
                    if (narrationState.failed) {
                        Text(
                            text = stringResource(R.string.a11y_voice_unavailable),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.semantics(mergeDescendants = true) { liveRegion = LiveRegionMode.Polite },
                        )
                    }
                }

                SecondaryButton(
                    text = stringResource(R.string.action_sign_out),
                    onClick = onSignOut,
                    modifier = Modifier.fillMaxWidth(),
                )

                Spacer(Modifier.height(Spacing.md))
                Text(
                    text = stringResource(R.string.profile_danger_section),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.semantics { heading() },
                )
                Text(
                    text = stringResource(R.string.profile_delete_explainer),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                DangerButton(
                    text = stringResource(R.string.action_delete_account),
                    onClick = onDeleteClick,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }

    if (state.showDeleteDialog) {
        DeleteAccountDialog(
            state = state,
            onPasswordChange = onDeletePasswordChange,
            onTogglePasswordVisible = onTogglePasswordVisible,
            onConfirm = onConfirmDelete,
            onDismiss = onDismissDelete,
        )
    }
}

@Composable
private fun PreferenceSwitchRow(
    title: String,
    summary: String? = null,
    checked: Boolean,
    enabled: Boolean = true,
    onCheckedChange: (Boolean) -> Unit,
) {
    androidx.compose.foundation.layout.Row(
        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f).padding(end = Spacing.sm)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            if (summary != null) {
                Text(summary, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            enabled = enabled,
            modifier = Modifier.semantics {
                contentDescription = if (summary.isNullOrBlank()) title else "$title. $summary"
            },
        )
    }
}

@Composable
private fun AccountRow(label: String, value: String) {
    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(text = value, style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
private fun DeleteAccountDialog(
    state: ProfileUiState,
    onPasswordChange: (String) -> Unit,
    onTogglePasswordVisible: () -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        title = { Text(stringResource(R.string.delete_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
                Text(
                    text = stringResource(R.string.delete_body),
                    style = MaterialTheme.typography.bodyMedium,
                )
                MedAssistTextField(
                    value = state.password,
                    onValueChange = onPasswordChange,
                    label = stringResource(R.string.field_password),
                    error = state.errorRes?.let { stringResource(it) },
                    enabled = !state.isDeleting,
                    imeAction = ImeAction.Done,
                    onImeAction = onConfirm,
                    isPassword = true,
                    passwordVisible = state.passwordVisible,
                    onTogglePasswordVisible = onTogglePasswordVisible,
                )
            }
        },
        confirmButton = {
            DangerButton(
                text = stringResource(R.string.delete_confirm),
                onClick = onConfirm,
                loading = state.isDeleting,
            )
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !state.isDeleting) {
                Text(text = stringResource(R.string.action_cancel), color = accessibleAquaTextColor())
            }
        },
    )
}
