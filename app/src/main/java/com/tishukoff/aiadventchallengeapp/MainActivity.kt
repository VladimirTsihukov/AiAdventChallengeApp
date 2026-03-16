package com.tishukoff.aiadventchallengeapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.TextButton
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.dropUnlessResumed
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import com.tishukoff.aiadventchallengeapp.presentation.ChatViewModel
import com.tishukoff.aiadventchallengeapp.presentation.ui.components.ChatScreen
import com.tishukoff.aiadventchallengeapp.presentation.ui.components.DrawerContent
import com.tishukoff.aiadventchallengeapp.presentation.ui.models.ChatIntent
import com.tishukoff.aiadventchallengeapp.presentation.ui.models.ChatRoute
import com.tishukoff.core.designsystem.AiAdventChallengeAppTheme
import com.tishukoff.feature.memory.api.MemoryRoute
import com.tishukoff.feature.memory.impl.presentation.ui.MemoryScreen
import com.tishukoff.feature.profile.api.ProfileRoute
import com.tishukoff.feature.profile.impl.presentation.ui.ProfileScreen
import com.tishukoff.feature.invariant.api.InvariantRoute
import com.tishukoff.feature.invariant.impl.presentation.ui.InvariantScreen
import com.tishukoff.feature.mcp.api.McpOrchestrationRoute
import com.tishukoff.feature.mcp.api.McpPipelineRoute
import com.tishukoff.feature.mcp.api.McpRoute
import com.tishukoff.feature.mcp.api.McpSchedulerRoute
import com.tishukoff.feature.mcp.impl.presentation.orchestration.ui.OrchestrationScreen
import com.tishukoff.feature.mcp.impl.presentation.pipeline.ui.PipelineScreen
import com.tishukoff.feature.mcp.impl.presentation.ui.McpScreen
import com.tishukoff.feature.mcp.impl.presentation.scheduler.ui.SchedulerScreen
import com.tishukoff.feature.setting.api.SettingRoute
import com.tishukoff.feature.setting.impl.presentation.ui.SettingScreen
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AiAdventChallengeAppTheme {
                AppNavigation()
            }
        }
    }
}

@Composable
fun AppNavigation() {
    val backStack = rememberNavBackStack(ChatRoute)

    NavDisplay(
        backStack = backStack,
        onBack = { backStack.removeLastOrNull() },
        entryDecorators = listOf(
            rememberSaveableStateHolderNavEntryDecorator(),
            rememberViewModelStoreNavEntryDecorator(),
        ),
        entryProvider = entryProvider {
            entry<ChatRoute> {
                ChatScreenWithDrawer(
                    onNavigateToSettings = dropUnlessResumed {
                        backStack.add(SettingRoute)
                    },
                    onNavigateToMemory = dropUnlessResumed {
                        backStack.add(MemoryRoute)
                    },
                    onNavigateToProfile = dropUnlessResumed {
                        backStack.add(ProfileRoute)
                    },
                    onNavigateToInvariants = dropUnlessResumed {
                        backStack.add(InvariantRoute)
                    },
                    onNavigateToMcp = dropUnlessResumed {
                        backStack.add(McpRoute)
                    },
                )
            }

            entry<SettingRoute> {
                SettingScreen(
                    onBack = dropUnlessResumed {
                        backStack.removeLastOrNull()
                    },
                )
            }

            entry<MemoryRoute> {
                MemoryScreen(
                    onBack = dropUnlessResumed {
                        backStack.removeLastOrNull()
                    },
                )
            }

            entry<ProfileRoute> {
                ProfileScreen(
                    onBack = dropUnlessResumed {
                        backStack.removeLastOrNull()
                    },
                )
            }

            entry<InvariantRoute> {
                InvariantScreen(
                    onBack = dropUnlessResumed {
                        backStack.removeLastOrNull()
                    },
                )
            }

            entry<McpRoute> {
                McpScreen(
                    onBack = dropUnlessResumed {
                        backStack.removeLastOrNull()
                    },
                    onNavigateToScheduler = dropUnlessResumed {
                        backStack.add(McpSchedulerRoute)
                    },
                    onNavigateToPipeline = dropUnlessResumed {
                        backStack.add(McpPipelineRoute)
                    },
                    onNavigateToOrchestration = dropUnlessResumed {
                        backStack.add(McpOrchestrationRoute)
                    },
                )
            }

            entry<McpOrchestrationRoute> {
                OrchestrationScreen(
                    onBack = dropUnlessResumed {
                        backStack.removeLastOrNull()
                    },
                )
            }

            entry<McpSchedulerRoute> {
                SchedulerScreen(
                    onBack = dropUnlessResumed {
                        backStack.removeLastOrNull()
                    },
                )
            }

            entry<McpPipelineRoute> {
                PipelineScreen(
                    onBack = dropUnlessResumed {
                        backStack.removeLastOrNull()
                    },
                )
            }
        },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreenWithDrawer(
    onNavigateToSettings: () -> Unit,
    onNavigateToMemory: () -> Unit,
    onNavigateToProfile: () -> Unit,
    onNavigateToInvariants: () -> Unit,
    onNavigateToMcp: () -> Unit,
    viewModel: ChatViewModel = koinViewModel(),
) {
    val state by viewModel.uiState.collectAsState()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                DrawerContent(
                    chats = state.chats,
                    currentChatId = state.currentChatId,
                    onSettingsClick = {
                        scope.launch { drawerState.close() }
                        onNavigateToSettings()
                    },
                    onProfileClick = {
                        scope.launch { drawerState.close() }
                        onNavigateToProfile()
                    },
                    onInvariantsClick = {
                        scope.launch { drawerState.close() }
                        onNavigateToInvariants()
                    },
                    onMcpClick = {
                        scope.launch { drawerState.close() }
                        onNavigateToMcp()
                    },
                    onChatSelect = { chatId ->
                        viewModel.handleIntent(ChatIntent.SelectChat(chatId))
                        scope.launch { drawerState.close() }
                    },
                    onChatDelete = { chatId ->
                        viewModel.handleIntent(ChatIntent.DeleteChat(chatId))
                    },
                    onNewChat = {
                        viewModel.handleIntent(ChatIntent.NewChat)
                        scope.launch { drawerState.close() }
                    },
                )
            }
        },
    ) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            topBar = {
                TopAppBar(
                    title = { Text("Claude Chat") },
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(
                                imageVector = Icons.Default.Menu,
                                contentDescription = "Menu",
                            )
                        }
                    },
                    actions = {
                        TextButton(onClick = onNavigateToMemory) {
                            Text("Memory")
                        }
                        if (state.messages.isNotEmpty() && !state.isLoading) {
                            IconButton(
                                onClick = { viewModel.handleIntent(ChatIntent.ClearHistory) }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Clear history",
                                )
                            }
                        }
                    },
                )
            },
        ) { innerPadding ->
            ChatScreen(
                modifier = Modifier.padding(innerPadding),
                state = viewModel.uiState.collectAsState(),
                onIntent = { viewModel.handleIntent(it) },
            )
        }
    }
}
