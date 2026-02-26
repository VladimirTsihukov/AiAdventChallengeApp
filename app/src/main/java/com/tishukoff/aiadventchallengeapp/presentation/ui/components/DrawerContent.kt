package com.tishukoff.aiadventchallengeapp.presentation.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.tishukoff.core.database.api.ChatRecord
import com.tishukoff.core.designsystem.AiAdventChallengeAppTheme

@Composable
fun DrawerContent(
    chats: List<ChatRecord>,
    currentChatId: Long?,
    onSettingsClick: () -> Unit,
    onChatSelect: (Long) -> Unit,
    onChatDelete: (Long) -> Unit,
    onNewChat: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Text(
            text = "Claude Chat",
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(16.dp),
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onSettingsClick)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Default.Settings,
                contentDescription = null,
            )
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = "Settings",
                style = MaterialTheme.typography.bodyLarge,
            )
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

        Text(
            text = "Chats",
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
        )

        LazyColumn(
            modifier = Modifier.weight(1f),
        ) {
            items(
                items = chats,
                key = { it.id },
            ) { chat ->
                ChatListItem(
                    chat = chat,
                    isSelected = chat.id == currentChatId,
                    onSelect = { onChatSelect(chat.id) },
                    onDelete = { onChatDelete(chat.id) },
                )
            }
        }

        HorizontalDivider()

        TextButton(
            onClick = onNewChat,
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = null,
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = "New Chat")
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun ChatListItem(
    chat: ChatRecord,
    isSelected: Boolean,
    onSelect: () -> Unit,
    onDelete: () -> Unit,
) {
    val backgroundColor = if (isSelected) {
        MaterialTheme.colorScheme.secondaryContainer
    } else {
        MaterialTheme.colorScheme.surface
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(backgroundColor)
            .clickable(onClick = onSelect)
            .padding(start = 16.dp, end = 4.dp, top = 4.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = chat.title,
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
            color = if (isSelected) {
                MaterialTheme.colorScheme.onSecondaryContainer
            } else {
                MaterialTheme.colorScheme.onSurface
            },
        )
        IconButton(onClick = onDelete) {
            Icon(
                imageVector = Icons.Default.Delete,
                contentDescription = "Delete chat",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun DrawerContentPreview() {
    AiAdventChallengeAppTheme(dynamicColor = false) {
        DrawerContent(
            chats = listOf(
                ChatRecord(id = 1, title = "Как работает Kotlin?", createdAt = 1000L),
                ChatRecord(id = 2, title = "Объясни Clean Architecture", createdAt = 2000L),
                ChatRecord(id = 3, title = "Очень длинное название чата которое не должно поместиться", createdAt = 3000L),
            ),
            currentChatId = 2,
            onSettingsClick = {},
            onChatSelect = {},
            onChatDelete = {},
            onNewChat = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun DrawerContentEmptyPreview() {
    AiAdventChallengeAppTheme(dynamicColor = false) {
        DrawerContent(
            chats = emptyList(),
            currentChatId = null,
            onSettingsClick = {},
            onChatSelect = {},
            onChatDelete = {},
            onNewChat = {},
        )
    }
}
