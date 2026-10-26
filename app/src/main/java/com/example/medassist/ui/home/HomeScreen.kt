package com.example.medassist.ui.home

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import com.example.medassist.R
import com.example.medassist.navigation.Screen
import com.example.medassist.ui.components.CardAccent
import com.example.medassist.ui.components.QuickAccessCard
import com.example.medassist.ui.components.ScreenListenButton
import com.example.medassist.ui.theme.Spacing
import com.example.medassist.utils.TimeOfDay
import java.time.LocalTime
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.width
import androidx.compose.material3.TextButton
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.style.TextOverflow
import com.example.medassist.data.model.MedicalDocument
import com.example.medassist.ui.common.UiState
import com.example.medassist.ui.components.IconBadge
import com.example.medassist.ui.components.LoadingState
import com.example.medassist.ui.components.MedAssistCard
import com.example.medassist.ui.theme.AquaDeep
import com.example.medassist.ui.theme.accessibleAquaTextColor
import com.example.medassist.ui.theme.AquaTint
import com.example.medassist.ui.vault.documentMeta

/**
 * Home dashboard: greeting, shortcuts and recent documents.
 * Today's step progress arrives in Phase 6.
 */
@Composable
fun HomeScreen(
    userName: String,
    onOpenTab: (Screen) -> Unit,
    onOpenAnalyzer: () -> Unit,
    modifier: Modifier = Modifier,
    recentDocuments: UiState<List<MedicalDocument>> = UiState.Empty,
    onOpenDocument: (String) -> Unit = {},
) {
    val timeOfDay = remember { TimeOfDay.fromHour(LocalTime.now().hour) }
    @StringRes val greetingRes = when (timeOfDay) {
        TimeOfDay.MORNING -> R.string.greeting_morning
        TimeOfDay.AFTERNOON -> R.string.greeting_afternoon
        TimeOfDay.EVENING -> R.string.greeting_evening
        TimeOfDay.NIGHT -> R.string.greeting_night
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(Spacing.md),
        verticalArrangement = Arrangement.spacedBy(Spacing.md),
    ) {
        item {
            val greeting = stringResource(greetingRes)
            Row(verticalAlignment = Alignment.Top, modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.weight(1f)) {
                    Text(
                        // "Good morning, Asha". Without a saved name we show just the greeting.
                        text = if (userName.isBlank()) greeting else stringResource(R.string.greeting_named, greeting, userName),
                        style = MaterialTheme.typography.headlineMedium,
                        modifier = Modifier.semantics { heading() },
                    )
                    Text(
                        text = stringResource(R.string.home_subtitle),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                ScreenListenButton(narrationText = stringResource(R.string.a11y_speech_home))
            }
        }

        item {
            Text(
                text = stringResource(R.string.home_quick_access),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.semantics { heading() },
            )
        }

        item {
            // IntrinsicSize.Min + fillMaxHeight makes both cards in a row the same height.
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(IntrinsicSize.Min),
                horizontalArrangement = Arrangement.spacedBy(Spacing.md),
            ) {
                QuickAccessCard(
                    title = stringResource(R.string.home_card_assistant_title),
                    description = stringResource(R.string.home_card_assistant_desc),
                    iconRes = R.drawable.ic_chat,
                    onClick = { onOpenTab(Screen.Assistant) },
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                )
                QuickAccessCard(
                    title = stringResource(R.string.home_card_vault_title),
                    description = stringResource(R.string.home_card_vault_desc),
                    iconRes = R.drawable.ic_folder,
                    onClick = { onOpenTab(Screen.Vault) },
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                )
            }
        }

        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(IntrinsicSize.Min),
                horizontalArrangement = Arrangement.spacedBy(Spacing.md),
            ) {
                // Orange marks the one shortcut we want to stand out.
                QuickAccessCard(
                    title = stringResource(R.string.home_card_analyzer_title),
                    description = stringResource(R.string.home_card_analyzer_desc),
                    iconRes = R.drawable.ic_report,
                    onClick = onOpenAnalyzer,
                    accent = CardAccent.Orange,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                )
                QuickAccessCard(
                    title = stringResource(R.string.home_card_profile_title),
                    description = stringResource(R.string.home_card_profile_desc),
                    iconRes = R.drawable.ic_person,
                    onClick = { onOpenTab(Screen.Profile) },
                    accent = CardAccent.Orange,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                )
            }
        }

        item {
            RecentDocumentsSection(
                state = recentDocuments,
                onOpenDocument = onOpenDocument,
                onSeeAll = { onOpenTab(Screen.Vault) },
            )
        }

        item {
            Spacer(Modifier.height(Spacing.sm))
            Text(
                text = stringResource(R.string.home_disclaimer),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun RecentDocumentsSection(
    state: UiState<List<MedicalDocument>>,
    onOpenDocument: (String) -> Unit,
    onSeeAll: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = stringResource(R.string.home_recent_documents),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier
                    .weight(1f)
                    .semantics { heading() },
            )
            TextButton(onClick = onSeeAll) { Text(stringResource(R.string.home_see_all), color = MaterialTheme.colorScheme.primary) }
        }
        when (state) {
            UiState.Loading -> LoadingState()
            UiState.Empty -> HomeNote(stringResource(R.string.home_recent_empty))
            is UiState.Error -> HomeNote(stringResource(R.string.home_recent_error))
            is UiState.Success -> state.data.forEach { doc ->
                MedAssistCard(onClick = { onOpenDocument(doc.id) }, modifier = Modifier.fillMaxWidth()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconBadge(iconRes = R.drawable.ic_description, background = AquaTint, tint = AquaDeep)
                        Spacer(Modifier.width(Spacing.md))
                        Column(Modifier.weight(1f)) {
                            Text(doc.name, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(
                                documentMeta(doc),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HomeNote(text: String) {
    Text(text = text, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
}
