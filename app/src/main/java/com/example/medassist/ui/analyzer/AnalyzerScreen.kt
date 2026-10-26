package com.example.medassist.ui.analyzer

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.medassist.R
import com.example.medassist.data.model.MedicalDocument
import com.example.medassist.data.model.ReportTestValue
import com.example.medassist.ui.common.UiState
import com.example.medassist.ui.components.IconBadge
import com.example.medassist.ui.components.LoadingState
import com.example.medassist.ui.components.MedAssistCard
import com.example.medassist.ui.components.MedAssistTopBar
import com.example.medassist.ui.components.OfflineBanner
import com.example.medassist.ui.components.PrimaryButton
import com.example.medassist.ui.components.SecondaryButton
import com.example.medassist.ui.theme.AquaBlue
import com.example.medassist.ui.theme.AquaDeep
import com.example.medassist.ui.theme.accessibleAquaTextColor
import com.example.medassist.ui.theme.AquaTint
import com.example.medassist.ui.theme.OrangeDeep
import com.example.medassist.ui.theme.OrangeTint
import com.example.medassist.ui.theme.Spacing
import com.example.medassist.ui.vault.documentMeta
import com.example.medassist.ui.vault.labelRes

@Composable
fun AnalyzerScreen(
    viewModel: ReportAnalyzerViewModel,
    onBack: () -> Unit,
    onOpenVault: () -> Unit,
    onOpenDocument: (String) -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val isOnline by viewModel.isOnline.collectAsStateWithLifecycle()
    val busy = state.work is AnalyzerWorkState.Working

    Scaffold(
        topBar = { MedAssistTopBar(title = stringResource(R.string.title_analyzer), onBack = onBack, narrationText = stringResource(R.string.a11y_speech_analyzer)) },
    ) { innerPadding ->
        // Use one bounded vertical scroller for the complete screen. This avoids nested
        // scrollable children in the empty/loading states, which can trigger Compose's
        // infinite-height measurement crash on some device/configuration combinations.
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(Spacing.md),
        ) {
            MedAssistCard(modifier = Modifier.fillMaxWidth()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconBadge(iconRes = R.drawable.ic_report, background = AquaTint, tint = AquaDeep)
                    Spacer(Modifier.width(Spacing.md))
                    Text(
                        text = stringResource(R.string.analyzer_intro_title),
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.semantics { heading() },
                    )
                }
                Spacer(Modifier.height(Spacing.sm))
                Text(
                    text = stringResource(R.string.analyzer_intro_body),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            if (!isOnline) OfflineBanner(visible = true)

            Text(
                text = stringResource(R.string.analyzer_choose_report),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.semantics { heading() },
            )

            when (val docs = state.documents) {
                UiState.Loading -> LoadingState(message = stringResource(R.string.analyzer_loading_documents))
                UiState.Empty -> AnalyzerEmptyDocumentsCard(onOpenVault = onOpenVault)
                is UiState.Error -> AnalyzerInlineErrorCard(
                    message = stringResource(docs.messageRes),
                    onRetry = viewModel::reloadDocuments,
                )
                is UiState.Success -> docs.data.forEach { document ->
                    AnalyzerDocumentCard(
                        document = document,
                        enabled = !busy && isOnline,
                        onAnalyze = { viewModel.requestAnalysis(document) },
                    )
                }
            }

            when (val work = state.work) {
                AnalyzerWorkState.Idle -> Unit
                is AnalyzerWorkState.Working -> AnalyzerProgressCard(work.stage)
                is AnalyzerWorkState.Failed -> AnalyzerInlineErrorCard(
                    message = stringResource(work.messageRes),
                    onRetry = if (work.retryable) viewModel::retry else null,
                )
                is AnalyzerWorkState.Completed -> AnalysisResultContent(
                    document = state.selectedDocument,
                    result = work.result,
                    onOpenOriginal = onOpenDocument,
                )
            }

            MedAssistCard(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = stringResource(R.string.analyzer_disclaimer_title),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                )
                Spacer(Modifier.height(Spacing.xs))
                Text(
                    text = stringResource(R.string.analyzer_disclaimer_body),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }

    state.consentDocument?.let { document ->
        AlertDialog(
            onDismissRequest = viewModel::cancelConsent,
            title = { Text(stringResource(R.string.analyzer_consent_title)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                    Text(stringResource(R.string.analyzer_consent_document, document.name))
                    Text(stringResource(R.string.analyzer_consent_body))
                    Text(
                        text = stringResource(R.string.analyzer_consent_privacy),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = viewModel::confirmConsent) {
                    Text(stringResource(R.string.analyzer_consent_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = viewModel::cancelConsent) {
                    Text(stringResource(R.string.action_cancel))
                }
            },
        )
    }
}

@Composable
private fun AnalyzerEmptyDocumentsCard(onOpenVault: () -> Unit) {
    MedAssistCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            IconBadge(iconRes = R.drawable.ic_folder, background = AquaTint, tint = AquaDeep, badgeSize = 64.dp)
            Text(
                text = stringResource(R.string.analyzer_no_documents_title),
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center,
            )
            Text(
                text = stringResource(R.string.analyzer_no_documents_body),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            PrimaryButton(
                text = stringResource(R.string.analyzer_open_vault),
                onClick = onOpenVault,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun AnalyzerInlineErrorCard(message: String, onRetry: (() -> Unit)?) {
    MedAssistCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            IconBadge(iconRes = R.drawable.ic_warning, background = OrangeTint, tint = OrangeDeep, badgeSize = 56.dp)
            Text(
                text = stringResource(R.string.common_error_title),
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center,
            )
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            if (onRetry != null) {
                PrimaryButton(
                    text = stringResource(R.string.common_retry),
                    onClick = onRetry,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

@Composable
private fun AnalyzerDocumentCard(
    document: MedicalDocument,
    enabled: Boolean,
    onAnalyze: () -> Unit,
) {
    MedAssistCard(modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconBadge(iconRes = R.drawable.ic_description, background = AquaTint, tint = AquaDeep)
            Spacer(Modifier.width(Spacing.md))
            Column(Modifier.weight(1f)) {
                Text(
                    text = document.name,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = stringResource(document.category.labelRes()),
                    style = MaterialTheme.typography.bodyMedium,
                    color = accessibleAquaTextColor(),
                )
                Text(
                    text = documentMeta(document),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Spacer(Modifier.height(Spacing.sm))
        SecondaryButton(
            text = stringResource(R.string.analyzer_action_analyze),
            onClick = onAnalyze,
            modifier = Modifier.fillMaxWidth(),
            enabled = enabled,
        )
    }
}

@Composable
private fun AnalyzerProgressCard(stage: AnalyzerStage) {
    val messageRes = when (stage) {
        AnalyzerStage.DOWNLOADING -> R.string.analyzer_stage_downloading
        AnalyzerStage.EXTRACTING -> R.string.analyzer_stage_extracting
        AnalyzerStage.ANALYZING -> R.string.analyzer_stage_analyzing
    }
    MedAssistCard(modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            CircularProgressIndicator(modifier = Modifier.size(28.dp), color = MaterialTheme.colorScheme.primary, strokeWidth = 3.dp)
            Spacer(Modifier.width(Spacing.md))
            Column {
                Text(stringResource(R.string.analyzer_progress_title), style = MaterialTheme.typography.titleMedium)
                Text(stringResource(messageRes), style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

@Composable
private fun AnalysisResultContent(
    document: MedicalDocument?,
    result: AnalyzerDisplayResult,
    onOpenOriginal: (String) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
        Text(
            text = stringResource(R.string.analyzer_result_title),
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.semantics { heading() },
        )
        Text(
            text = stringResource(R.string.analyzer_result_ai_label),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        document?.let { analyzedDocument ->
            Text(
                text = stringResource(R.string.analyzer_result_for, analyzedDocument.name),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            SecondaryButton(
                text = stringResource(R.string.analyzer_open_original),
                onClick = { onOpenOriginal(analyzedDocument.id) },
                modifier = Modifier.fillMaxWidth(),
            )
        }
        if (result.textTruncated) {
            MedAssistCard(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = stringResource(R.string.analyzer_text_truncated, result.pagesRead, result.totalPages),
                    style = MaterialTheme.typography.bodyMedium,
                    color = OrangeDeep,
                )
            }
        }

        ReportSectionCard(title = stringResource(R.string.analyzer_section_overview)) {
            Text(
                text = result.analysis.overview.ifBlank { stringResource(R.string.analyzer_section_not_available) },
                style = MaterialTheme.typography.bodyMedium,
            )
        }
        ReportSectionCard(title = stringResource(R.string.analyzer_section_findings)) {
            BulletList(result.analysis.importantFindings)
        }
        ReportSectionCard(title = stringResource(R.string.analyzer_section_terms)) {
            BulletList(result.analysis.medicalTerms)
        }
        ReportSectionCard(title = stringResource(R.string.analyzer_section_values)) {
            if (result.analysis.testValues.isEmpty()) {
                Text(stringResource(R.string.analyzer_no_values), style = MaterialTheme.typography.bodyMedium)
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                    result.analysis.testValues.forEachIndexed { index, testValue ->
                        TestValueItem(testValue)
                        if (index < result.analysis.testValues.lastIndex) HorizontalDivider()
                    }
                }
            }
        }
        ReportSectionCard(title = stringResource(R.string.analyzer_section_questions)) {
            BulletList(result.analysis.questionsForDoctor)
        }
        ReportSectionCard(title = stringResource(R.string.analyzer_section_limitations)) {
            BulletList(result.analysis.limitations)
        }

        var showSourceText by remember(result.extractedSourceText) { mutableStateOf(false) }
        MedAssistCard(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = stringResource(R.string.analyzer_source_title),
                style = MaterialTheme.typography.titleSmall,
            )
            TextButton(onClick = { showSourceText = !showSourceText }) {
                Text(stringResource(if (showSourceText) R.string.analyzer_source_hide else R.string.analyzer_source_show))
            }
            AnimatedVisibility(visible = showSourceText) {
                Text(
                    text = result.extractedSourceText,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

@Composable
private fun ReportSectionCard(title: String, content: @Composable () -> Unit) {
    MedAssistCard(modifier = Modifier.fillMaxWidth()) {
        Text(text = title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.semantics { heading() })
        Spacer(Modifier.height(Spacing.sm))
        content()
    }
}

@Composable
private fun BulletList(items: List<String>) {
    if (items.isEmpty()) {
        Text(stringResource(R.string.analyzer_section_not_available), style = MaterialTheme.typography.bodyMedium)
    } else {
        Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
            items.forEach { item ->
                Text(text = "• $item", style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

@Composable
private fun TestValueItem(value: ReportTestValue) {
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
        Text(
            text = value.testName.ifBlank { stringResource(R.string.analyzer_test_unknown) },
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
        )
        val displayedValue = listOf(value.value, value.unit).filter(String::isNotBlank).joinToString(" ")
        if (displayedValue.isNotBlank()) Text(
            text = stringResource(R.string.analyzer_test_value, displayedValue),
            style = MaterialTheme.typography.bodyMedium,
        )
        if (value.referenceRange.isNotBlank()) Text(
            text = stringResource(R.string.analyzer_reference_range, value.referenceRange),
            style = MaterialTheme.typography.bodyMedium,
        )
        if (value.reportFlag.isNotBlank()) Text(
            text = stringResource(R.string.analyzer_report_flag, value.reportFlag),
            style = MaterialTheme.typography.bodyMedium,
            color = if (value.reportFlag.contains("high", ignoreCase = true) ||
                value.reportFlag.contains("low", ignoreCase = true) ||
                value.reportFlag.contains("उच्च") || value.reportFlag.contains("कम")
            ) OrangeDeep else MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
