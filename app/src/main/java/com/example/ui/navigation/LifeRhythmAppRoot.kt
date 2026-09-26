package com.example.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoGraph
import androidx.compose.material.icons.filled.DirectionsWalk
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.LifeRhythmViewModel
import com.example.ui.screens.activity.ActivityScreen
import com.example.ui.screens.home.HomeScreen
import com.example.ui.screens.onboarding.OnboardingScreen
import com.example.ui.screens.settings.SettingsScreen
import com.example.ui.screens.timeline.TimelineScreen
import com.example.ui.screens.trends.TrendsScreen

enum class NavDestination(val label: String, val icon: ImageVector) {
    HOME("Home", Icons.Default.Home),
    TIMELINE("Timeline", Icons.Default.Timeline),
    TRENDS("Trends", Icons.Default.AutoGraph),
    ACTIVITY("Activity", Icons.Default.DirectionsWalk),
    SETTINGS("Settings", Icons.Default.Settings)
}

@Composable
fun LifeRhythmAppRoot(
    viewModel: LifeRhythmViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var currentDestination by remember { mutableStateOf(NavDestination.HOME) }

    LaunchedEffect(uiState.userNotice) {
        uiState.userNotice?.let { notice ->
            snackbarHostState.showSnackbar(notice)
            viewModel.clearUserNotice()
        }
    }

    if (!uiState.isOnboardingCompleted) {
        OnboardingScreen(
            hasUsageAccess = uiState.hasUsageAccess,
            onOpenUsageSettings = { viewModel.openUsageSettings() },
            onFinishOnboarding = { viewModel.completeOnboarding() },
            isHealthConnectEnabled = uiState.isHealthConnectEnabled,
            onToggleHealthConnect = { viewModel.setHealthConnectEnabled(it) },
            isEveningDigestEnabled = uiState.isEveningDigestEnabled,
            onToggleEveningDigest = { viewModel.setEveningDigestEnabled(it) }
        )
    } else {
        Scaffold(
            modifier = modifier.fillMaxSize(),
            contentWindowInsets = WindowInsets.safeDrawing,
            snackbarHost = { SnackbarHost(snackbarHostState) },
            bottomBar = {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    tonalElevation = 4.dp
                ) {
                    NavDestination.entries.forEach { destination ->
                        val isSelected = currentDestination == destination
                        NavigationBarItem(
                            selected = isSelected,
                            onClick = { currentDestination = destination },
                            icon = {
                                Icon(
                                    imageVector = destination.icon,
                                    contentDescription = destination.label
                                )
                            },
                            label = { Text(destination.label) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                selectedTextColor = MaterialTheme.colorScheme.primary,
                                indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                                unselectedIconColor = MaterialTheme.colorScheme.outline,
                                unselectedTextColor = MaterialTheme.colorScheme.outline
                            ),
                            modifier = Modifier.testTag("nav_${destination.name.lowercase()}")
                        )
                    }
                }
            }
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                when (currentDestination) {
                    NavDestination.HOME -> HomeScreen(
                        uiState = uiState,
                        onRefresh = { viewModel.refreshData() },
                        onOpenUsageSettings = { viewModel.openUsageSettings() },
                        onNavigateToTimeline = { currentDestination = NavDestination.TIMELINE },
                        onNavigateToActivity = { currentDestination = NavDestination.ACTIVITY }
                    )
                    NavDestination.TIMELINE -> TimelineScreen(
                        summary = uiState.summary
                    )
                    NavDestination.TRENDS -> TrendsScreen(
                        youVsYouMetrics = uiState.youVsYouMetrics,
                        observedPatterns = uiState.observedPatterns,
                        history = uiState.history
                    )
                    NavDestination.ACTIVITY -> ActivityScreen(
                        summary = uiState.summary,
                        isHealthConnectEnabled = uiState.isHealthConnectEnabled,
                        onToggleHealthConnect = { viewModel.setHealthConnectEnabled(it) },
                        onOpenHealthConnect = { viewModel.openHealthConnect() }
                    )
                    NavDestination.SETTINGS -> SettingsScreen(
                        uiState = uiState,
                        onOpenUsageSettings = { viewModel.openUsageSettings() },
                        onToggleHealthConnect = { viewModel.setHealthConnectEnabled(it) },
                        onToggleEveningDigest = { viewModel.setEveningDigestEnabled(it) },
                        onExportJson = { viewModel.exportJson() },
                        onExportCsv = { viewModel.exportCsv() },
                        onDeleteAllData = {
                            viewModel.deleteAllData {
                                currentDestination = NavDestination.HOME
                            }
                        },
                        onShowNotice = { viewModel.showNotice(it) }
                    )
                }
            }
        }
    }
}
