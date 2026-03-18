package com.tishukoff.feature.rag.impl.presentation.ui

import android.R
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.tishukoff.feature.rag.impl.domain.model.RagMode
import com.tishukoff.feature.rag.impl.presentation.RagChatMessage
import com.tishukoff.feature.rag.impl.presentation.RagIntent
import com.tishukoff.feature.rag.impl.presentation.RagViewModel
import com.tishukoff.feature.rag.impl.presentation.SourceInfo
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RagChatScreen(
    onBack: () -> Unit,
) {
    val viewModel: RagViewModel = koinViewModel()
    val state by viewModel.uiState.collectAsState()
    val modes = RagMode.entries
    val pagerState = rememberPagerState(pageCount = { modes.size })
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(pagerState.currentPage) {
        viewModel.handleIntent(RagIntent.SwitchMode(modes[pagerState.currentPage]))
    }

    LaunchedEffect(state.currentMode) {
        val targetPage = modes.indexOf(state.currentMode)
        if (pagerState.currentPage != targetPage) {
            pagerState.animateScrollToPage(targetPage)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("RAG Search") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                        )
                    }
                },
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .imePadding(),
        ) {
            TabRow(selectedTabIndex = pagerState.currentPage) {
                modes.forEachIndexed { index, mode ->
                    val label = when (mode) {
                        RagMode.FIXED_SIZE -> "Fixed (${state.fixedSizeChunkCount})"
                        RagMode.STRUCTURAL -> "Structural (${state.structuralChunkCount})"
                        RagMode.NO_RAG -> "No RAG"
                    }
                    Tab(
                        selected = pagerState.currentPage == index,
                        onClick = {
                            coroutineScope.launch { pagerState.animateScrollToPage(index) }
                        },
                        text = { Text(label, maxLines = 1) },
                    )
                }
            }

            if (state.isIndexing) {
                IndexingProgress(progress = state.indexingProgress)
            }

            HorizontalPager(
                state = pagerState,
                modifier = Modifier.weight(1f),
            ) { page ->
                val pageMode = modes[page]
                val messages = when (pageMode) {
                    RagMode.FIXED_SIZE -> state.fixedSizeMessages
                    RagMode.STRUCTURAL -> state.structuralMessages
                    RagMode.NO_RAG -> state.noRagMessages
                }
                RagChatPage(
                    messages = messages,
                    isLoading = state.isLoading && state.currentMode == pageMode,
                    isBenchmarkRunning = state.isBenchmarkRunning && state.currentMode == pageMode,
                    benchmarkProgress = state.benchmarkProgress,
                )
            }

            state.error?.let { error ->
                Snackbar(
                    modifier = Modifier.padding(8.dp),
                    action = {
                        TextButton(onClick = { viewModel.handleIntent(RagIntent.DismissError) }) {
                            Text("OK")
                        }
                    },
                ) {
                    Text(error)
                }
            }

            BottomInputBar(
                input = state.input,
                isLoading = state.isLoading,
                isIndexing = state.isIndexing,
                isBenchmarkRunning = state.isBenchmarkRunning,
                showIndexButton = state.currentMode != RagMode.NO_RAG,
                onInputChange = { viewModel.handleIntent(RagIntent.UpdateInput(it)) },
                onSend = { viewModel.handleIntent(RagIntent.SendMessage) },
                onIndex = { viewModel.handleIntent(RagIntent.IndexDocuments) },
                onBenchmark = { viewModel.handleIntent(RagIntent.RunBenchmark) },
            )
        }
    }
}

@Composable
private fun RagChatPage(
    messages: List<RagChatMessage>,
    isLoading: Boolean,
    isBenchmarkRunning: Boolean,
    benchmarkProgress: String,
) {
    val listState = rememberLazyListState()

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.lastIndex)
        }
    }

    LazyColumn(
        state = listState,
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(vertical = 12.dp),
    ) {
        if (messages.isEmpty() && !isLoading && !isBenchmarkRunning) {
            item {
                Box(
                    modifier = Modifier.fillParentMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "RAG Document Search",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Нажмите \"Индексировать\" для начала,\nзатем задайте вопрос по документам",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        )
                    }
                }
            }
        }

        items(messages) { message ->
            RagMessageBubble(message = message)
        }

        if (isLoading || isBenchmarkRunning) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Start,
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            if (isBenchmarkRunning && benchmarkProgress.isNotEmpty()) {
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = benchmarkProgress,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun IndexingProgress(progress: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp),
    ) {
        LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = progress,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun RagMessageBubble(message: RagChatMessage) {
    val alignment = if (message.isUser) Arrangement.End else Arrangement.Start
    val backgroundColor = if (message.isUser) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        MaterialTheme.colorScheme.surfaceVariant
    }
    val textColor = if (message.isUser) {
        MaterialTheme.colorScheme.onPrimaryContainer
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = alignment,
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .background(backgroundColor)
                    .padding(horizontal = 16.dp, vertical = 10.dp)
                    .fillMaxWidth(if (message.isUser) 0.8f else 0.95f),
            ) {
                Text(
                    text = message.text,
                    color = textColor,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }

        if (message.sources.isNotEmpty()) {
            Spacer(modifier = Modifier.height(4.dp))
            SourcesList(sources = message.sources)
        }
    }
}

@Composable
private fun SourcesList(sources: List<SourceInfo>) {
    var expanded by remember { mutableStateOf(false) }

    Column(modifier = Modifier.padding(start = 8.dp)) {
        Text(
            text = if (expanded) "Скрыть источники" else "Показать источники (${sources.size})",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier
                .clickable { expanded = !expanded }
                .padding(4.dp),
        )

        AnimatedVisibility(visible = expanded) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                sources.forEach { source ->
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                        ),
                    ) {
                        Column(modifier = Modifier.padding(8.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                Text(
                                    text = source.fileName,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f),
                                )
                                Text(
                                    text = "${"%.0f".format(source.score * 100)}%",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.primary,
                                )
                            }
                            Text(
                                text = source.section,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = source.chunkPreview,
                                style = MaterialTheme.typography.bodySmall,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BottomInputBar(
    input: String,
    isLoading: Boolean,
    isIndexing: Boolean,
    isBenchmarkRunning: Boolean,
    showIndexButton: Boolean,
    onInputChange: (String) -> Unit,
    onSend: () -> Unit,
    onIndex: () -> Unit,
    onBenchmark: () -> Unit,
) {
    val isBusy = isLoading || isIndexing || isBenchmarkRunning

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (showIndexButton) {
                Button(
                    onClick = onIndex,
                    enabled = !isBusy,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.tertiary,
                    ),
                ) {
                    Text(
                        text = if (isIndexing) "Индексация..." else "Индексировать",
                        color = MaterialTheme.colorScheme.onTertiary,
                    )
                }
            }

            Button(
                onClick = onBenchmark,
                enabled = !isBusy,
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.secondary,
                ),
            ) {
                Text(
                    text = if (isBenchmarkRunning) "Тест..." else "Тест (10)",
                    color = MaterialTheme.colorScheme.onSecondary,
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        OutlinedTextField(
            value = input,
            onValueChange = onInputChange,
            placeholder = { Text("Задайте вопрос по документам...") },
            modifier = Modifier.fillMaxWidth(),
            enabled = !isBusy,
            trailingIcon = {
                if (!isLoading) {
                    IconButton(
                        onClick = onSend,
                        enabled = input.isNotBlank() && !isBusy,
                        colors = IconButtonDefaults.iconButtonColors(
                            contentColor = MaterialTheme.colorScheme.primary,
                        ),
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_menu_send),
                            contentDescription = "Send",
                        )
                    }
                }
            },
        )
    }
}
