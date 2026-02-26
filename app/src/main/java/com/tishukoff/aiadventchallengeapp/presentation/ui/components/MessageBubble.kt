package com.tishukoff.aiadventchallengeapp.presentation.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.tishukoff.core.designsystem.AiAdventChallengeAppTheme
import com.tishukoff.feature.agent.api.ChatMessage

internal val BubbleShape = RoundedCornerShape(16.dp)

@Composable
fun MessageBubble(message: ChatMessage) {
    val maxWidth = LocalConfiguration.current.screenWidthDp.dp * 0.8f

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (message.isUser) Arrangement.End else Arrangement.Start
    ) {
        Column(modifier = Modifier.widthIn(max = maxWidth)) {
            Column(
                modifier = Modifier
                    .clip(BubbleShape)
                    .background(
                        if (message.isUser)
                            MaterialTheme.colorScheme.primary
                        else
                            MaterialTheme.colorScheme.surfaceVariant
                    )
                    .padding(horizontal = 14.dp, vertical = 10.dp)
            ) {
                Text(
                    text = message.text,
                    color = if (message.isUser)
                        MaterialTheme.colorScheme.onPrimary
                    else
                        MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyLarge
                )
            }

            val metadata = message.metadataText
            if (metadata != null) {
                Text(
                    text = metadata,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    modifier = Modifier.padding(start = 8.dp, top = 4.dp)
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun MessageBubbleUserPreview() {
    AiAdventChallengeAppTheme(dynamicColor = false) {
        MessageBubble(
            message = ChatMessage(text = "Привет! Как дела?", isUser = true),
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun MessageBubbleAssistantPreview() {
    AiAdventChallengeAppTheme(dynamicColor = false) {
        MessageBubble(
            message = ChatMessage(
                text = "Привет! У меня всё отлично, спасибо за вопрос. Чем могу помочь?",
                isUser = false,
                metadataText = "claude-sonnet-4-5 | in: 8 out: 20 | 0.9s | \$0.0003",
            ),
        )
    }
}
