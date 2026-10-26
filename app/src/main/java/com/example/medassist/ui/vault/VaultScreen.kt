package com.example.medassist.ui.vault

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.medassist.R
import com.example.medassist.data.model.DocumentCategory
import com.example.medassist.data.model.DocumentSort
import com.example.medassist.data.model.MedicalDocument
import com.example.medassist.domain.vault.VaultConfig
import com.example.medassist.ui.common.UiState
import com.example.medassist.ui.common.containerViewModelFactory
import com.example.medassist.ui.components.DangerButton
import com.example.medassist.ui.components.EmptyState
import com.example.medassist.ui.components.FormErrorBanner
import com.example.medassist.ui.components.IconBadge
import com.example.medassist.ui.components.MedAssistCard
import com.example.medassist.ui.components.MedAssistTextField
import com.example.medassist.ui.components.MedAssistTopBar
import com.example.medassist.ui.components.PrimaryButton
import com.example.medassist.ui.components.SecondaryButton
import com.example.medassist.ui.components.UiStateContent
import com.example.medassist.ui.theme.AquaBlue
import com.example.medassist.ui.theme.AquaDeep
import com.example.medassist.ui.theme.accessibleAquaTextColor
import com.example.medassist.ui.theme.AquaTint
import com.example.medassist.ui.theme.CardGrey
import com.example.medassist.ui.theme.DarkCharcoal
import com.example.medassist.ui.theme.Spacing

/** Medical Vault tab: the user's PDFs with search, filter, sort, upload, edit and delete. */
@Composable
fun VaultScreen(uid: String, onOpenDocument: (String) -> Unit) {
    val viewModel: VaultViewModel = viewModel(
        key = "vault:$uid",
        factory = containerViewModelFactory {
            VaultViewModel(it.vaultRepository, it.pickedPdfReader, it.connectivityObserver.isOnline)
        },
    )
    // The system file picker. Asking for application/pdf hides other files; the content is still checked.
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) viewModel.onFilePicked(uri.toString())
    }

    val list by viewModel.list.collectAsStateWithLifecycle()
    val controls by viewModel.controls.collectAsStateWithLifecycle()
    val upload by viewModel.upload.collectAsStateWithLifecycle()
    val edit by viewModel.edit.collectAsStateWithLifecycle()
    val pendingDelete by viewModel.pendingDelete.collectAsStateWithLifecycle()
    val isDeleting by viewModel.isDeleting.collectAsStateWithLifecycle()
    val isReadingFile by viewModel.isReadingFile.collectAsStateWithLifecycle()
    val message by viewModel.message.collectAsStateWithLifecycle()

    val snackbar = remember { SnackbarHostState() }
    val context = LocalContext.current
    LaunchedEffect(message) {
        message?.let {
            viewModel.consumeMessage()
            snackbar.showSnackbar(context.getString(it.messageRes))
        }
    }

    VaultContent(
        list = list,
        controls = controls,
        isReadingFile = isReadingFile,
        snackbar = snackbar,
        onUploadClick = { picker.launch(arrayOf(VaultConfig.MIME_PDF)) },
        onRetry = viewModel::listen,
        onSearchChange = viewModel::onSearchChange,
        onCategoryFilter = viewModel::onCategoryFilter,
        onSortChange = viewModel::onSortChange,
        onOpen = { onOpenDocument(it.id) },
        onEdit = viewModel::startEdit,
        onDelete = viewModel::requestDelete,
    )

    upload?.let { draft ->
        UploadDialog(
            draft = draft,
            onNameChange = viewModel::onUploadNameChange,
            onCategoryChange = viewModel::onUploadCategoryChange,
            onNotesChange = viewModel::onUploadNotesChange,
            onUpload = viewModel::startUpload,
            onCancel = viewModel::cancelUpload,
        )
    }
    edit?.let { draft ->
        EditDialog(
            draft = draft,
            onNameChange = viewModel::onEditNameChange,
            onCategoryChange = viewModel::onEditCategoryChange,
            onNotesChange = viewModel::onEditNotesChange,
            onSave = viewModel::saveEdit,
            onCancel = viewModel::cancelEdit,
        )
    }
    pendingDelete?.let { doc ->
        AlertDialog(
            onDismissRequest = viewModel::cancelDelete,
            title = { Text(stringResource(R.string.vault_delete_title)) },
            text = { Text(stringResource(R.string.vault_delete_body, doc.name)) },
            confirmButton = {
                DangerButton(
                    text = stringResource(R.string.vault_delete_confirm),
                    onClick = viewModel::confirmDelete,
                    loading = isDeleting,
                )
            },
            dismissButton = {
                TextButton(onClick = viewModel::cancelDelete, enabled = !isDeleting) {
                    Text(stringResource(R.string.action_cancel))
                }
            },
        )
    }
}

/** Stateless list screen, so UI tests can drive it without Firebase. */
@Composable
fun VaultContent(
    list: UiState<VaultList>,
    controls: VaultControls,
    isReadingFile: Boolean,
    snackbar: SnackbarHostState,
    onUploadClick: () -> Unit,
    onRetry: () -> Unit,
    onSearchChange: (String) -> Unit,
    onCategoryFilter: (DocumentCategory?) -> Unit,
    onSortChange: (DocumentSort) -> Unit,
    onOpen: (MedicalDocument) -> Unit,
    onEdit: (MedicalDocument) -> Unit,
    onDelete: (MedicalDocument) -> Unit,
) {
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = { MedAssistTopBar(title = stringResource(R.string.title_vault), narrationText = stringResource(R.string.a11y_speech_vault)) },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        Column(
            Modifier
                .padding(padding)
                .fillMaxSize(),
        ) {
            Column(Modifier.padding(horizontal = Spacing.md)) {
                PrimaryButton(
                    text = stringResource(if (isReadingFile) R.string.vault_reading_file else R.string.vault_action_upload),
                    onClick = onUploadClick,
                    loading = isReadingFile,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("vault_upload"),
                )
                Text(
                    text = stringResource(R.string.vault_limit_note, (VaultConfig.MAX_FILE_BYTES / (1024 * 1024)).toInt()),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = Spacing.xs, bottom = Spacing.sm),
                )
            }
            UiStateContent(
                state = list,
                onRetry = onRetry,
                modifier = Modifier.weight(1f),
                empty = {
                    EmptyState(
                        iconRes = R.drawable.ic_folder,
                        title = stringResource(R.string.vault_empty_title),
                        message = stringResource(R.string.vault_empty_body),
                    )
                },
            ) { data ->
                DocumentList(
                    data = data,
                    controls = controls,
                    onSearchChange = onSearchChange,
                    onCategoryFilter = onCategoryFilter,
                    onSortChange = onSortChange,
                    onOpen = onOpen,
                    onEdit = onEdit,
                    onDelete = onDelete,
                )
            }
        }
    }
}

@Composable
private fun DocumentList(
    data: VaultList,
    controls: VaultControls,
    onSearchChange: (String) -> Unit,
    onCategoryFilter: (DocumentCategory?) -> Unit,
    onSortChange: (DocumentSort) -> Unit,
    onOpen: (MedicalDocument) -> Unit,
    onEdit: (MedicalDocument) -> Unit,
    onDelete: (MedicalDocument) -> Unit,
) {
    val hasNoVisibleDocuments = data.documents.isEmpty()
    val categorySelectedWithoutSearch = controls.category != null && controls.search.isBlank()
    val trulyEmptyVault = !data.hasAnyDocuments && controls.category == null && controls.search.isBlank()
    val hasActiveFilters = controls.category != null || controls.search.isNotBlank()
    val clearFilters: (() -> Unit)? = if (hasActiveFilters) {
        {
            onSearchChange("")
            onCategoryFilter(null)
        }
    } else {
        null
    }

    Column(Modifier.fillMaxSize().testTag("vault_list_container")) {
        // Do not place EmptyState inside LazyColumn. EmptyState is vertically scrollable; nested
        // inside a lazy-list item it can be measured with an infinite height and crash Compose.
        LazyColumn(
            modifier = if (hasNoVisibleDocuments) {
                Modifier.fillMaxWidth().wrapContentHeight()
            } else {
                Modifier.fillMaxSize().testTag("vault_list")
            },
            contentPadding = PaddingValues(start = Spacing.md, end = Spacing.md, bottom = Spacing.md),
            verticalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            item {
                OutlinedTextField(
                    value = controls.search,
                    onValueChange = onSearchChange,
                    label = { Text(stringResource(R.string.vault_search_label)) },
                    leadingIcon = { Icon(painterResource(R.drawable.ic_search), contentDescription = null) },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("vault_search"),
                )
            }
            item { CategoryFilterRow(selected = controls.category, onSelect = onCategoryFilter) }
            item { SortRow(sort = controls.sort, onSortChange = onSortChange) }
            if (!hasNoVisibleDocuments) {
                items(data.documents, key = { it.id }) { doc ->
                    DocumentRow(document = doc, onOpen = onOpen, onEdit = onEdit, onDelete = onDelete)
                }
            }
        }

        if (hasNoVisibleDocuments) {
            val title = when {
                categorySelectedWithoutSearch -> stringResource(R.string.vault_category_empty_title)
                trulyEmptyVault -> stringResource(R.string.vault_empty_title)
                else -> stringResource(R.string.vault_no_results_title)
            }
            val message = when {
                categorySelectedWithoutSearch -> stringResource(R.string.vault_category_empty_body)
                trulyEmptyVault -> stringResource(R.string.vault_empty_body)
                else -> stringResource(R.string.vault_no_results_body)
            }
            EmptyState(
                iconRes = if (categorySelectedWithoutSearch || trulyEmptyVault) R.drawable.ic_folder else R.drawable.ic_search,
                title = title,
                message = message,
                modifier = Modifier.fillMaxWidth().weight(1f),
                actionLabel = if (clearFilters != null) stringResource(R.string.vault_action_clear_filters) else null,
                onAction = clearFilters,
            )
        }
    }
}

@Composable
private fun CategoryFilterRow(selected: DocumentCategory?, onSelect: (DocumentCategory?) -> Unit) {
    val label = stringResource(R.string.vault_filter_label)
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
        modifier = Modifier.semantics { contentDescription = label },
    ) {
        item {
            CategoryChip(stringResource(R.string.vault_filter_all), selected == null) { onSelect(null) }
        }
        items(DocumentCategory.entries) { category ->
            CategoryChip(stringResource(category.labelRes()), selected == category) { onSelect(category) }
        }
    }
}

@Composable
private fun CategoryChip(label: String, selected: Boolean, onClick: () -> Unit) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label) },
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = AquaTint,
            selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
        ),
    )
}

@Composable
private fun SortRow(sort: DocumentSort, onSortChange: (DocumentSort) -> Unit) {
    var open by remember { mutableStateOf(false) }
    Box {
        TextButton(onClick = { open = true }) {
            Icon(painterResource(R.drawable.ic_sort), contentDescription = null, tint = accessibleAquaTextColor())
            Spacer(Modifier.width(Spacing.sm))
            Text(stringResource(R.string.vault_sort_label, stringResource(sort.labelRes())), color = accessibleAquaTextColor())
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            DocumentSort.entries.forEach { option ->
                DropdownMenuItem(
                    text = { Text(stringResource(option.labelRes())) },
                    onClick = {
                        open = false
                        onSortChange(option)
                    },
                )
            }
        }
    }
}

@Composable
private fun DocumentRow(
    document: MedicalDocument,
    onOpen: (MedicalDocument) -> Unit,
    onEdit: (MedicalDocument) -> Unit,
    onDelete: (MedicalDocument) -> Unit,
) {
    var menuOpen by remember { mutableStateOf(false) }
    MedAssistCard(onClick = { onOpen(document) }, modifier = Modifier.fillMaxWidth()) {
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
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Box {
                IconButton(onClick = { menuOpen = true }) {
                    Icon(
                        painterResource(R.drawable.ic_more_vert),
                        contentDescription = stringResource(R.string.vault_doc_actions, document.name),
                    )
                }
                DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.vault_action_open)) },
                        leadingIcon = { Icon(painterResource(R.drawable.ic_description), null) },
                        onClick = { menuOpen = false; onOpen(document) },
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.vault_action_edit)) },
                        leadingIcon = { Icon(painterResource(R.drawable.ic_edit), null) },
                        onClick = { menuOpen = false; onEdit(document) },
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.vault_action_delete)) },
                        leadingIcon = { Icon(painterResource(R.drawable.ic_delete), null) },
                        onClick = { menuOpen = false; onDelete(document) },
                    )
                }
            }
        }
    }
}

@Composable
private fun UploadDialog(
    draft: UploadDraft,
    onNameChange: (String) -> Unit,
    onCategoryChange: (DocumentCategory) -> Unit,
    onNotesChange: (String) -> Unit,
    onUpload: () -> Unit,
    onCancel: () -> Unit,
) {
    AlertDialog(
        // Tapping outside must not lose a running upload; Cancel is explicit.
        onDismissRequest = { if (!draft.isUploading) onCancel() },
        title = { Text(stringResource(R.string.vault_upload_title)) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                Text(
                    stringResource(R.string.vault_upload_file_size, fileSize(draft.sizeBytes)),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(Spacing.sm))
                DocumentFormFields(
                    form = draft.form,
                    enabled = !draft.isUploading,
                    onNameChange = onNameChange,
                    onCategoryChange = onCategoryChange,
                    onNotesChange = onNotesChange,
                )
                if (draft.isUploading) {
                    val percent = (draft.progress * 100).toInt()
                    Spacer(Modifier.height(Spacing.md))
                    LinearProgressIndicator(
                        progress = { draft.progress },
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("upload_progress"),
                    )
                    Text(
                        text = stringResource(R.string.vault_uploading, percent),
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier
                            .padding(top = Spacing.xs)
                            .semantics { liveRegion = LiveRegionMode.Polite },
                    )
                }
                draft.errorRes?.let {
                    Spacer(Modifier.height(Spacing.md))
                    FormErrorBanner(stringResource(it))
                }
            }
        },
        confirmButton = {
            PrimaryButton(
                text = stringResource(if (draft.failed) R.string.vault_action_retry_upload else R.string.vault_action_start_upload),
                onClick = onUpload,
                loading = draft.isUploading,
            )
        },
        dismissButton = {
            TextButton(onClick = onCancel) { Text(stringResource(R.string.action_cancel)) }
        },
    )
}

@Composable
private fun EditDialog(
    draft: EditDraft,
    onNameChange: (String) -> Unit,
    onCategoryChange: (DocumentCategory) -> Unit,
    onNotesChange: (String) -> Unit,
    onSave: () -> Unit,
    onCancel: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onCancel,
        title = { Text(stringResource(R.string.vault_edit_title)) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                DocumentFormFields(
                    form = draft.form,
                    enabled = !draft.isSaving,
                    onNameChange = onNameChange,
                    onCategoryChange = onCategoryChange,
                    onNotesChange = onNotesChange,
                )
                draft.errorRes?.let {
                    Spacer(Modifier.height(Spacing.md))
                    FormErrorBanner(stringResource(it))
                }
            }
        },
        confirmButton = {
            PrimaryButton(text = stringResource(R.string.action_save), onClick = onSave, loading = draft.isSaving)
        },
        dismissButton = {
            SecondaryButton(text = stringResource(R.string.action_cancel), onClick = onCancel, enabled = !draft.isSaving)
        },
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DocumentFormFields(
    form: DocumentForm,
    enabled: Boolean,
    onNameChange: (String) -> Unit,
    onCategoryChange: (DocumentCategory) -> Unit,
    onNotesChange: (String) -> Unit,
) {
    MedAssistTextField(
        value = form.name,
        onValueChange = onNameChange,
        label = stringResource(R.string.field_document_name),
        error = form.nameErrorRes?.let { stringResource(it) },
        enabled = enabled,
        modifier = Modifier.testTag("doc_name"),
    )
    Spacer(Modifier.height(Spacing.md))
    Text(
        stringResource(R.string.field_category),
        style = MaterialTheme.typography.titleSmall,
        modifier = Modifier.semantics { heading() },
    )
    FlowRow(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
        DocumentCategory.entries.forEach { category ->
            FilterChip(
                selected = form.category == category,
                onClick = { onCategoryChange(category) },
                enabled = enabled,
                label = { Text(stringResource(category.labelRes())) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = AquaTint,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                ),
            )
        }
    }
    Spacer(Modifier.height(Spacing.md))
    MedAssistTextField(
        value = form.notes,
        onValueChange = onNotesChange,
        label = stringResource(R.string.field_notes),
        error = form.notesErrorRes?.let { stringResource(it) },
        enabled = enabled,
        imeAction = ImeAction.Done,
        supportingText = stringResource(R.string.vault_notes_counter, form.notes.length, VaultConfig.NOTES_MAX_LENGTH),
    )
}

