package com.tishukoff.aiadventchallengeapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import com.tishukoff.aiadventchallengeapp.presentation.ChatViewModel
import com.tishukoff.aiadventchallengeapp.presentation.CompareViewModel
import com.tishukoff.aiadventchallengeapp.presentation.ui.components.ChatScreen
import com.tishukoff.aiadventchallengeapp.presentation.ui.components.CompareScreen
import com.tishukoff.aiadventchallengeapp.presentation.ui.components.SettingsDrawer
import com.tishukoff.aiadventchallengeapp.presentation.ui.models.ChatIntent
import com.tishukoff.aiadventchallengeapp.presentation.ui.models.Screen
import com.tishukoff.aiadventchallengeapp.presentation.ui.theme.AiAdventChallengeAppTheme
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AiAdventChallengeAppTheme {
                var currentScreen by remember { mutableStateOf(Screen.CHAT) }

                when (currentScreen) {
                    Screen.CHAT -> ChatScreenWithDrawer(
                        onNavigateToCompare = { currentScreen = Screen.COMPARE }
                    )
                    Screen.COMPARE -> CompareScreenWithTopBar(
                        onBack = { currentScreen = Screen.CHAT }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreenWithDrawer(
    viewModel: ChatViewModel = koinViewModel(),
    onNavigateToCompare: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            SettingsDrawer(
                settings = state.settings,
                onSave = { settings ->
                    viewModel.handleIntent(ChatIntent.SaveSettings(settings))
                    scope.launch { drawerState.close() }
                }
            )
        }
    ) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            topBar = {
                TopAppBar(
                    title = { Text("Claude Chat") },
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = "Settings"
                            )
                        }
                    },
                    actions = {
                        IconButton(onClick = onNavigateToCompare) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_compare),
                                contentDescription = "Compare Models"
                            )
                        }
                    }
                )
            }
        ) { innerPadding ->
            ChatScreen(
                modifier = Modifier.padding(innerPadding),
                viewModel = viewModel
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CompareScreenWithTopBar(
    viewModel: CompareViewModel = koinViewModel(),
    onBack: () -> Unit
) {
    BackHandler(onBack = onBack)

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text("Compare Models") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        CompareScreen(
            modifier = Modifier.padding(innerPadding),
            viewModel = viewModel
        )
    }
}
