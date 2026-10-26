package com.example.medassist.ui.chatbot

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.pm.PackageManager
import android.speech.RecognizerIntent
import android.widget.Toast
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.medassist.R
import com.example.medassist.data.model.ChatMessage
import com.example.medassist.data.model.ChatRole
import com.example.medassist.domain.assistant.AssistantConfig
import com.example.medassist.ui.components.FormErrorBanner
import com.example.medassist.ui.components.MedAssistTopBar
import com.example.medassist.ui.components.OfflineBanner
import com.example.medassist.ui.components.ReadAloudButton
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

@Composable
fun ChatbotScreen(viewModel: ChatViewModel, modifier: Modifier = Modifier) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val online by viewModel.isOnline.collectAsStateWithLifecycle()
    ChatContent(
        state = state,
        online = online,
        onInputChange = viewModel::onInputChange,
        onSend = viewModel::send,
        onSuggestion = viewModel::useSuggestion,
        onRetry = viewModel::retry,
        onNewConversation = viewModel::requestNewConversation,
        onConfirmNew = viewModel::confirmNewConversation,
        onDismissNew = viewModel::dismissNewConversation,
        onHistoryChange = viewModel::setHistoryEnabled,
        onDismissEmergency = viewModel::dismissEmergency,
        modifier = modifier,
    )
}

@Composable
fun ChatContent(
    state: ChatUiState,
    online: Boolean,
    onInputChange: (String) -> Unit,
    onSend: () -> Unit,
    onSuggestion: (String) -> Unit,
    onRetry: (String) -> Unit,
    onNewConversation: () -> Unit,
    onConfirmNew: () -> Unit,
    onDismissNew: () -> Unit,
    onHistoryChange: (Boolean) -> Unit,
    onDismissEmergency: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val listState = rememberLazyListState()
    val itemCount = state.messages.size + (if (state.sending) 1 else 0)
    LaunchedEffect(itemCount) {
        if (itemCount > 0) listState.animateScrollToItem(itemCount - 1)
    }

    Column(modifier.fillMaxSize().imePadding()) {
        MedAssistTopBar(
            title = stringResource(R.string.title_assistant),
            narrationText = stringResource(R.string.a11y_speech_assistant),
            actions = { ChatMenu(state.historyEnabled, state.messages.isNotEmpty(), onNewConversation, onHistoryChange) },
        )
        OfflineBanner(visible = !online)
        Text(
            text = stringResource(R.string.chat_disclaimer),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .padding(horizontal = 16.dp, vertical = 8.dp),
        )
        if (state.showEmergency) EmergencyBanner(onDismissEmergency)

        Box(Modifier.weight(1f).fillMaxWidth()) {
            if (state.messages.isEmpty() && !state.sending) {
                Welcome(onSuggestion, enabled = online)
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize().testTag("chat_list"),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    items(state.messages, key = { it.id }) { message ->
                        MessageBubble(message, canRetry = !state.sending, onRetry = { onRetry(message.id) })
                    }
                    if (state.sending) item(key = "typing") { TypingIndicator() }
                }
            }
        }

        state.errorRes?.let {
            FormErrorBanner(stringResource(it), Modifier.padding(horizontal = 16.dp, vertical = 4.dp))
        }
        InputRow(state, online, onInputChange, onSend)
    }

    if (state.confirmNewConversation) {
        AlertDialog(
            onDismissRequest = onDismissNew,
            title = { Text(stringResource(R.string.chat_new_title)) },
            text = {
                Text(
                    stringResource(
                        if (state.historyEnabled) R.string.chat_new_body_saved else R.string.chat_new_body,
                    ),
                )
            },
            confirmButton = { TextButton(onClick = onConfirmNew) { Text(stringResource(R.string.chat_new_confirm)) } },
            dismissButton = { TextButton(onClick = onDismissNew) { Text(stringResource(R.string.chat_cancel)) } },
        )
    }
}

@Composable
private fun ChatMenu(
    historyEnabled: Boolean,
    hasMessages: Boolean,
    onNewConversation: () -> Unit,
    onHistoryChange: (Boolean) -> Unit,
) {
    var open by remember { mutableStateOf(false) }
    Box {
        IconButton(onClick = { open = true }) {
            Icon(painterResource(R.drawable.ic_more_vert), contentDescription = stringResource(R.string.chat_menu))
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.chat_new_conversation)) },
                enabled = hasMessages,
                onClick = {
                    open = false
                    onNewConversation()
                },
            )
            DropdownMenuItem(
                text = {
                    Column {
                        Text(stringResource(R.string.chat_save_history))
                        Text(
                            stringResource(R.string.chat_save_history_desc),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                },
                trailingIcon = { Checkbox(checked = historyEnabled, onCheckedChange = null) },
                onClick = { onHistoryChange(!historyEnabled) },
                modifier = Modifier.widthIn(max = 300.dp),
            )
        }
    }
}

@Composable
private fun EmergencyBanner(onDismiss: () -> Unit) {
    val context = LocalContext.current
    Surface(
        color = MaterialTheme.colorScheme.errorContainer,
        modifier = Modifier.fillMaxWidth().testTag("chat_emergency").semantics { liveRegion = LiveRegionMode.Assertive },
    ) {
        Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
            Text(
                stringResource(R.string.chat_emergency_title),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onErrorContainer,
            )
            Text(
                stringResource(R.string.chat_emergency_body),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onErrorContainer,
            )
            Row {
                TextButton(onClick = {
                    // ACTION_DIAL only opens the dialer with the number filled in: no permission needed.
                    try {
                        context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:112")))
                    } catch (_: ActivityNotFoundException) {
                    }
                }) { Text(stringResource(R.string.chat_call_emergency)) }
                TextButton(onClick = onDismiss) { Text(stringResource(R.string.chat_dismiss)) }
            }
        }
    }
}

@Composable
private fun Welcome(onSuggestion: (String) -> Unit, enabled: Boolean) {
    val suggestions = listOf(
        stringResource(R.string.chat_suggestion_1),
        stringResource(R.string.chat_suggestion_2),
        stringResource(R.string.chat_suggestion_3),
    )
    Column(
        Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(stringResource(R.string.chat_welcome_title), style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.size(8.dp))
        Text(
            stringResource(R.string.chat_welcome_body),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.size(16.dp))
        suggestions.forEach { text ->
            SuggestionChip(onClick = { onSuggestion(text) }, label = { Text(text) }, enabled = enabled)
        }
    }
}

@Composable
private fun MessageBubble(message: ChatMessage, canRetry: Boolean, onRetry: () -> Unit) {
    val isUser = message.role == ChatRole.USER
    val time = formatTime(message.timeMillis)
    val description = stringResource(
        if (isUser) R.string.chat_you_said else R.string.chat_assistant_said,
        message.text,
        time,
    )
    Column(
        Modifier.fillMaxWidth(),
        horizontalAlignment = if (isUser) Alignment.End else Alignment.Start,
    ) {
        Surface(
            color = if (isUser) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
            shape = RoundedCornerShape(
                topStart = 16.dp, topEnd = 16.dp,
                bottomStart = if (isUser) 16.dp else 4.dp, bottomEnd = if (isUser) 4.dp else 16.dp,
            ),
            modifier = Modifier.widthIn(max = 320.dp).clearAndSetSemantics { contentDescription = description },
        ) {
            Text(
                message.text,
                style = MaterialTheme.typography.bodyMedium,
                color = if (isUser) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            )
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                if (message.failed) stringResource(R.string.chat_not_sent, time) else time,
                style = MaterialTheme.typography.labelSmall,
                color = if (message.failed) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 2.dp),
            )
            if (message.failed) {
                TextButton(onClick = onRetry, enabled = canRetry) { Text(stringResource(R.string.chat_retry)) }
            }
            if (message.role == ChatRole.ASSISTANT) {
                ReadAloudButton(
                    text = message.text,
                    contentDescription = stringResource(R.string.chat_read_response_aloud),
                    testTag = "chat_read_aloud_${message.id}",
                )
            }
        }
    }
}

@Composable
private fun TypingIndicator() {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.testTag("chat_typing").semantics { liveRegion = LiveRegionMode.Polite },
    ) {
        CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
        Spacer(Modifier.size(8.dp))
        Text(
            stringResource(R.string.chat_typing),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun InputRow(state: ChatUiState, online: Boolean, onInputChange: (String) -> Unit, onSend: () -> Unit) {
    val context = LocalContext.current
    val locale = LocalConfiguration.current.locales[0]
    val speechIntent = remember(locale) {
        Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, locale.toLanguageTag())
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, locale.toLanguageTag())
            putExtra(RecognizerIntent.EXTRA_PROMPT, context.getString(R.string.chat_speech_prompt))
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
        }
    }
    val speechLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            val spoken = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
                ?.firstOrNull()?.trim().orEmpty()
            if (spoken.isNotEmpty()) {
                val existing = state.input.trim()
                val combined = if (existing.isEmpty()) spoken else "$existing $spoken"
                onInputChange(combined.take(AssistantConfig.MAX_MESSAGE_CHARS))
            }
        }
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (granted) {
            try {
                speechLauncher.launch(speechIntent)
            } catch (_: ActivityNotFoundException) {
                Toast.makeText(context, R.string.chat_speech_unavailable, Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(context, R.string.chat_speech_permission_denied, Toast.LENGTH_SHORT).show()
        }
    }
    val canSend = online && !state.sending && state.input.isNotBlank() && state.inputErrorRes == null
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        OutlinedTextField(
            value = state.input,
            onValueChange = onInputChange,
            label = { Text(stringResource(R.string.chat_input_label)) },
            isError = state.inputErrorRes != null,
            supportingText = {
                Text(
                    state.inputErrorRes?.let { stringResource(it, AssistantConfig.MAX_MESSAGE_CHARS) }
                        ?: stringResource(R.string.chat_char_count, state.input.length, AssistantConfig.MAX_MESSAGE_CHARS),
                )
            },
            maxLines = 5,
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Default),
            modifier = Modifier.weight(1f).testTag("chat_input"),
        )
        Spacer(Modifier.size(4.dp))
        FilledIconButton(
            onClick = {
                val granted = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
                if (granted) {
                    try {
                        speechLauncher.launch(speechIntent)
                    } catch (_: ActivityNotFoundException) {
                        Toast.makeText(context, R.string.chat_speech_unavailable, Toast.LENGTH_SHORT).show()
                    }
                } else {
                    permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                }
            },
            enabled = !state.sending,
            modifier = Modifier.testTag("chat_speech_to_text"),
        ) {
            Icon(painterResource(R.drawable.ic_mic), contentDescription = stringResource(R.string.chat_speech_to_text))
        }
        Spacer(Modifier.size(4.dp))
        FilledIconButton(onClick = onSend, enabled = canSend, modifier = Modifier.testTag("chat_send")) {
            Icon(painterResource(R.drawable.ic_send), contentDescription = stringResource(R.string.chat_send))
        }
    }
}

@Composable
private fun formatTime(millis: Long): String {
    val locale = LocalConfiguration.current.locales[0]
    val formatter = remember(locale) { DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT).withLocale(locale) }
    return remember(millis, formatter) { formatter.format(Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault())) }
}
