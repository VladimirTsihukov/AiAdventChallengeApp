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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.tishukoff.feature.rag.impl.domain.model.ChunkingStrategy
import com.tishukoff.feature.rag.impl.presentation.RagChatMessage
import com.tishukoff.feature.rag.impl.presentation.RagIntent
import com.tishukoff.feature.rag.impl.presentation.RagViewModel
import com.tishukoff.feature.rag.impl.presentation.SourceInfo
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RagChatScreen(
    onBack: () -> Unit,
) {
    val viewModel: RagViewModel = koinViewModel()
    val state by viewModel.uiState.collectAsState()
    val listState = rememberLazyListState()

    LaunchedEffect(state.messages.size) {
        if (state.messages.isNotEmpty()) {
            listState.animateScrollToItem(state.messages.lastIndex)
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
            StrategySelector(
                currentStrategy = state.currentStrategy,
                fixedCount = state.fixedSizeChunkCount,
                structuralCount = state.structuralChunkCount,
                onStrategySelect = { viewModel.handleIntent(RagIntent.SwitchStrategy(it)) },
            )

            if (state.isIndexing) {
                IndexingProgress(progress = state.indexingProgress)
            }

            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(vertical = 12.dp),
            ) {
                if (state.messages.isEmpty() && !state.isLoading) {
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

                items(state.messages) { message ->
                    RagMessageBubble(message = message)
                }

                if (state.isLoading) {
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
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    strokeWidth = 2.dp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }
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
                onInputChange = { viewModel.handleIntent(RagIntent.UpdateInput(it)) },
                onSend = { viewModel.handleIntent(RagIntent.SendMessage) },
                onIndex = { viewModel.handleIntent(RagIntent.IndexDocuments) },
            )
        }
    }
}

@Composable
private fun StrategySelector(
    currentStrategy: ChunkingStrategy,
    fixedCount: Int,
    structuralCount: Int,
    onStrategySelect: (ChunkingStrategy) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        FilterChip(
            selected = currentStrategy == ChunkingStrategy.FIXED_SIZE,
            onClick = { onStrategySelect(ChunkingStrategy.FIXED_SIZE) },
            label = { Text("Fixed ($fixedCount)") },
        )
        FilterChip(
            selected = currentStrategy == ChunkingStrategy.STRUCTURAL,
            onClick = { onStrategySelect(ChunkingStrategy.STRUCTURAL) },
            label = { Text("Structural ($structuralCount)") },
        )
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
    onInputChange: (String) -> Unit,
    onSend: () -> Unit,
    onIndex: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp),
    ) {
        Button(
            onClick = onIndex,
            enabled = !isIndexing && !isLoading,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.tertiary,
            ),
        ) {
            Text(
                text = if (isIndexing) "Индексация..." else "Индексировать документы",
                color = MaterialTheme.colorScheme.onTertiary,
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        OutlinedTextField(
            value = input,
            onValueChange = onInputChange,
            placeholder = { Text("Задайте вопрос по документам...") },
            modifier = Modifier.fillMaxWidth(),
            enabled = !isLoading && !isIndexing,
            trailingIcon = {
                if (!isLoading) {
                    IconButton(
                        onClick = onSend,
                        enabled = input.isNotBlank(),
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
