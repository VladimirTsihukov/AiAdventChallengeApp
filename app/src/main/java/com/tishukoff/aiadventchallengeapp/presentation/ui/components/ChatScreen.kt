package com.tishukoff.aiadventchallengeapp.presentation.ui.components

import android.R
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.tishukoff.aiadventchallengeapp.presentation.ui.models.ChatIntent
import com.tishukoff.aiadventchallengeapp.presentation.ui.models.ChatUiState
import com.tishukoff.core.designsystem.AiAdventChallengeAppTheme
import com.tishukoff.feature.agent.api.ChatMessage
import com.tishukoff.feature.agent.api.TokenStats

@Composable
fun ChatScreen(
    modifier: Modifier = Modifier,
    state: State<ChatUiState>,
    onIntent: (ChatIntent) -> Unit,
) {
    val listState = rememberLazyListState()
    val stateValue = state.value
    // Auto-scroll to the latest message
    LaunchedEffect(stateValue.messages.size) {
        if (stateValue.messages.isNotEmpty()) {
            listState.animateScrollToItem(stateValue.messages.lastIndex)
        }
    }

    Column(modifier = modifier.fillMaxSize().imePadding()) {
        // Messages list
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(vertical = 12.dp)
        ) {
            if (stateValue.messages.isEmpty() && !stateValue.isLoading) {
                item {
                    Box(
                        modifier = Modifier
                            .fillParentMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Ask Claude anything...",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            items(stateValue.messages) { message ->
                MessageBubble(message = message)
            }

            if (stateValue.isLoading) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Start
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(BubbleShape)
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .padding(horizontal = 16.dp, vertical = 12.dp)
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }

        // Token stats panel
        TokenStatsPanel(
            stats = stateValue.tokenStats,
            compressionStats = stateValue.compressionStats,
            modifier = Modifier.padding(bottom = 4.dp),
        )

        // Input field at the bottom
        OutlinedTextField(
            value = stateValue.input,
            onValueChange = { onIntent(ChatIntent.UpdateInput(it)) },
            placeholder = { Text("Message") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            trailingIcon = {
                if (stateValue.isLoading.not()) {
                    IconButton(
                        onClick = { onIntent(ChatIntent.SendMessage) },
                        enabled = stateValue.input.isNotBlank(),
                        colors = IconButtonDefaults.iconButtonColors(
                            contentColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_menu_send),
                            contentDescription = "Send"
                        )
                    }
                }
            }
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun ChatScreenEmptyPreview() {
    AiAdventChallengeAppTheme(dynamicColor = false) {
        val state = remember {
            mutableStateOf(ChatUiState())
        }
        ChatScreen(state = state, onIntent = {})
    }
}

@Preview(showBackground = true)
@Composable
private fun ChatScreenWithMessagesPreview() {
    AiAdventChallengeAppTheme(dynamicColor = false) {
        val state = remember {
            mutableStateOf(
                ChatUiState(
                    messages = listOf(
                        ChatMessage(text = "Привет! Как работает Kotlin?", isUser = true),
                        ChatMessage(
                            text = "Kotlin — это современный язык программирования, разработанный JetBrains.",
                            isUser = false,
                            metadataText = "claude-sonnet-4-5 | in: 12 out: 24 | 1.2s | \$0.0004",
                        ),
                    ),
                    input = "Расскажи подробнее",
                    tokenStats = TokenStats(
                        totalInputTokens = 1250,
                        totalOutputTokens = 3400,
                        totalCostUsd = 0.0548,
                        requestCount = 5,
                        contextWindow = 200_000,
                        lastRequestInputTokens = 800,
                        lastRequestOutputTokens = 920,
                        lastRequestCostUsd = 0.0162,
                    )
                )
            )
        }
        ChatScreen(state = state, onIntent = {})
    }
}

@Preview(showBackground = true)
@Composable
private fun ChatScreenLoadingPreview() {
    AiAdventChallengeAppTheme(dynamicColor = false) {
        val state = remember {
            mutableStateOf(
                ChatUiState(
                    messages = listOf(
                        ChatMessage(text = "Что такое корутины?", isUser = true),
                    ),
                    isLoading = true,
                )
            )
        }
        ChatScreen(state = state, onIntent = {})
    }
}
