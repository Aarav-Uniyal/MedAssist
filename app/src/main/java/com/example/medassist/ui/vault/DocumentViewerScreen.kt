package com.example.medassist.ui.vault

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.medassist.R
import com.example.medassist.data.local.PdfPageSource
import com.example.medassist.ui.common.containerViewModelFactory
import com.example.medassist.ui.components.ErrorState
import com.example.medassist.ui.components.LoadingState
import com.example.medassist.ui.components.MedAssistTopBar
import com.example.medassist.ui.theme.CardGrey
import com.example.medassist.ui.theme.PureWhite
import com.example.medassist.ui.theme.Spacing
import kotlinx.coroutines.launch

private val ZOOM_STEPS = listOf(1f, 1.5f, 2f, 3f)

/** Reads one vault PDF inside the app. Pages are drawn by Android's PdfRenderer (no extra library). */
@Composable
fun DocumentViewerScreen(uid: String, documentId: String, onBack: () -> Unit) {
    val viewModel: DocumentViewerViewModel = viewModel(
        key = "viewer:$uid:$documentId",
        factory = containerViewModelFactory { c ->
            DocumentViewerViewModel(
                documentId = documentId,
                repository = c.vaultRepository,
                cacheFile = { id -> c.documentCache.fileFor(uid, id) },
                sourceFactory = c.pdfPageSourceFactory,
            )
        },
    )
    val state by viewModel.state.collectAsStateWithLifecycle()
    val title = (state as? ViewerState.Ready)?.document?.name ?: stringResource(R.string.title_vault)

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = { MedAssistTopBar(title = title, onBack = onBack, narrationText = stringResource(R.string.a11y_speech_document)) },
    ) { padding ->
        Box(
            Modifier
                .padding(padding)
                .fillMaxSize(),
        ) {
            when (val s = state) {
                ViewerState.Loading -> LoadingState(Modifier.fillMaxSize(), message = stringResource(R.string.viewer_loading))
                is ViewerState.Error -> ErrorState(
                    message = stringResource(s.messageRes),
                    onRetry = viewModel::load,
                    modifier = Modifier.fillMaxSize(),
                )
                is ViewerState.Ready -> viewModel.source?.let { source ->
                    PdfPages(source = source, state = s, onPageVisible = viewModel::onPageVisible)
                }
            }
        }
    }
}

@Composable
private fun PdfPages(source: PdfPageSource, state: ViewerState.Ready, onPageVisible: (Int) -> Unit) {
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    var zoomIndex by rememberSaveable { mutableStateOf(0) }
    val zoom = ZOOM_STEPS[zoomIndex]

    LaunchedEffect(listState) {
        snapshotFlow { listState.firstVisibleItemIndex }.collect(onPageVisible)
    }

    Column(Modifier.fillMaxSize()) {
        BoxWithConstraints(
            Modifier
                .weight(1f)
                .fillMaxWidth()
                .background(CardGrey),
        ) {
            val pageWidth = maxWidth * zoom
            val pageWidthPx = with(LocalDensity.current) { pageWidth.roundToPx() }
            // When zoomed in, the whole column of pages scrolls sideways as one.
            Box(
                Modifier
                    .fillMaxHeight()
                    .horizontalScroll(rememberScrollState()),
            ) {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .width(pageWidth)
                        .fillMaxHeight()
                        .testTag("pdf_pages"),
                    contentPadding = PaddingValues(Spacing.sm),
                    verticalArrangement = Arrangement.spacedBy(Spacing.sm),
                ) {
                    items(state.pageCount) { index ->
                        PdfPage(source, index, state.pageCount, pageWidthPx)
                    }
                }
            }
        }
        HorizontalDivider(color = CardGrey)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(PureWhite)
                .padding(horizontal = Spacing.sm),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(
                onClick = { scope.launch { listState.animateScrollToItem((state.currentPage - 1).coerceAtLeast(0)) } },
                enabled = state.currentPage > 0,
            ) {
                Icon(painterResource(R.drawable.ic_chevron_left), stringResource(R.string.viewer_previous_page))
            }
            Text(
                text = stringResource(R.string.viewer_page_indicator, state.currentPage + 1, state.pageCount),
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier
                    .weight(1f)
                    .semantics { liveRegion = LiveRegionMode.Polite }
                    .testTag("page_indicator"),
            )
            IconButton(
                onClick = { scope.launch { listState.animateScrollToItem((state.currentPage + 1).coerceAtMost(state.pageCount - 1)) } },
                enabled = state.currentPage < state.pageCount - 1,
            ) {
                Icon(painterResource(R.drawable.ic_chevron_right), stringResource(R.string.viewer_next_page))
            }
            IconButton(onClick = { zoomIndex-- }, enabled = zoomIndex > 0) {
                Icon(painterResource(R.drawable.ic_zoom_out), stringResource(R.string.viewer_zoom_out))
            }
            Text(
                text = stringResource(R.string.viewer_zoom_level, (zoom * 100).toInt()),
                style = MaterialTheme.typography.bodyMedium,
            )
            IconButton(onClick = { zoomIndex++ }, enabled = zoomIndex < ZOOM_STEPS.lastIndex) {
                Icon(painterResource(R.drawable.ic_zoom_in), stringResource(R.string.viewer_zoom_in))
            }
        }
    }
}

@Composable
private fun PdfPage(source: PdfPageSource, index: Int, pageCount: Int, widthPx: Int) {
    // Rendered only while on screen; LazyColumn drops it (and its bitmap) when scrolled away.
    val bitmap by produceState<ImageBitmap?>(null, source, index, widthPx) {
        value = runCatching { source.render(index, widthPx) }.getOrNull()
    }
    val description = stringResource(R.string.viewer_page_description, index + 1, pageCount)
    val image = bitmap
    if (image == null) {
        // A4-shaped blank while the page is drawn.
        Box(
            Modifier
                .fillMaxWidth()
                .aspectRatio(1f / 1.414f)
                .background(PureWhite),
        )
    } else {
        Image(
            bitmap = image,
            contentDescription = description,
            contentScale = ContentScale.FillWidth,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(image.width.toFloat() / image.height.coerceAtLeast(1)),
        )
    }
}

