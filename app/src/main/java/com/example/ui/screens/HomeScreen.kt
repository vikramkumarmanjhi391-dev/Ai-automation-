package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.ListAlt
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.MainViewModel
import com.example.ui.components.ApiKeySection
import com.example.ui.components.ExecutionLogSection
import com.example.ui.components.NodeExplorerSection
import com.example.ui.components.ServiceStatusCard
import com.example.ui.components.TaskControlSection
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkBorder
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val apiKey by viewModel.apiKey.collectAsStateWithLifecycle()
    val isApiKeyVisible by viewModel.isApiKeyVisible.collectAsStateWithLifecycle()
    val selectedModel by viewModel.selectedModel.collectAsStateWithLifecycle()
    val stepDelay by viewModel.stepDelay.collectAsStateWithLifecycle()
    val maxSteps by viewModel.maxSteps.collectAsStateWithLifecycle()
    val goalInput by viewModel.goalInput.collectAsStateWithLifecycle()
    val isServiceConnected by viewModel.isServiceConnected.collectAsStateWithLifecycle()
    val isAgentRunning by viewModel.isAgentRunning.collectAsStateWithLifecycle()
    val currentStatus by viewModel.currentStatus.collectAsStateWithLifecycle()
    val currentStep by viewModel.currentStep.collectAsStateWithLifecycle()
    val logs by viewModel.logs.collectAsStateWithLifecycle()
    val capturedNodes by viewModel.capturedNodes.collectAsStateWithLifecycle()
    val isOverlayActive by viewModel.isOverlayActive.collectAsStateWithLifecycle()
    val apiTestStatus by viewModel.apiTestStatus.collectAsStateWithLifecycle()
    val isTestingApi by viewModel.isTestingApi.collectAsStateWithLifecycle()

    var selectedTabIndex by remember { mutableIntStateOf(0) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = DarkBackground,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth().padding(end = 12.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(ElectricCyan.copy(alpha = 0.2f), RoundedCornerShape(10.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.SmartToy,
                                    contentDescription = null,
                                    tint = ElectricCyan,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "AI Auto Agent",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                                Text(
                                    text = "Screen Perception & Gestures",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TextSecondary,
                                    fontSize = 10.sp
                                )
                            }
                        }

                        // Status Badge in TopBar
                        Box(
                            modifier = Modifier
                                .background(
                                    if (isServiceConnected) EmeraldSuccess.copy(alpha = 0.15f) else Color(0xFFF59E0B).copy(alpha = 0.15f),
                                    RoundedCornerShape(12.dp)
                                )
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(if (isServiceConnected) EmeraldSuccess else Color(0xFFF59E0B))
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = if (isServiceConnected) "ONLINE" else "SERVICE OFF",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isServiceConnected) EmeraldSuccess else Color(0xFFF59E0B)
                                )
                            }
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = DarkSurface
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Navigation Tabs
            TabRow(
                selectedTabIndex = selectedTabIndex,
                containerColor = DarkSurface,
                contentColor = TextPrimary,
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        Modifier.tabIndicatorOffset(tabPositions[selectedTabIndex]),
                        color = ElectricCyan
                    )
                },
                divider = { Box(Modifier.fillMaxWidth().height(1.dp).background(DarkBorder)) }
            ) {
                Tab(
                    selected = selectedTabIndex == 0,
                    onClick = { selectedTabIndex = 0 },
                    text = { Text("Agent", fontWeight = if (selectedTabIndex == 0) FontWeight.Bold else FontWeight.Normal) },
                    icon = { Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp)) },
                    selectedContentColor = ElectricCyan,
                    unselectedContentColor = TextSecondary
                )
                Tab(
                    selected = selectedTabIndex == 1,
                    onClick = { selectedTabIndex = 1 },
                    text = { Text("Logs (${logs.size})", fontWeight = if (selectedTabIndex == 1) FontWeight.Bold else FontWeight.Normal) },
                    icon = { Icon(Icons.Default.ListAlt, contentDescription = null, modifier = Modifier.size(18.dp)) },
                    selectedContentColor = ElectricCyan,
                    unselectedContentColor = TextSecondary
                )
                Tab(
                    selected = selectedTabIndex == 2,
                    onClick = { selectedTabIndex = 2 },
                    text = { Text("UI Nodes (${capturedNodes.size})", fontWeight = if (selectedTabIndex == 2) FontWeight.Bold else FontWeight.Normal) },
                    icon = { Icon(Icons.Default.Code, contentDescription = null, modifier = Modifier.size(18.dp)) },
                    selectedContentColor = ElectricCyan,
                    unselectedContentColor = TextSecondary
                )
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                contentAlignment = Alignment.TopCenter
            ) {
                Column(
                    modifier = Modifier
                        .widthIn(max = 680.dp)
                        .fillMaxWidth()
                ) {
                    when (selectedTabIndex) {
                        0 -> {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .verticalScroll(rememberScrollState())
                                    .padding(vertical = 16.dp),
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                ServiceStatusCard(
                                    isConnected = isServiceConnected,
                                    onInspectClick = { viewModel.inspectCurrentScreen() }
                                )

                                TaskControlSection(
                                    goalInput = goalInput,
                                    isAgentRunning = isAgentRunning,
                                    currentStatus = currentStatus,
                                    currentStep = currentStep,
                                    maxSteps = maxSteps,
                                    stepDelay = stepDelay,
                                    isOverlayActive = isOverlayActive,
                                    isServiceConnected = isServiceConnected,
                                    onGoalChanged = { viewModel.onGoalChanged(it) },
                                    onStartAutomation = { viewModel.startAutomation() },
                                    onStopAutomation = { viewModel.stopAutomation() },
                                    onToggleOverlay = { viewModel.toggleFloatingOverlay() },
                                    onStepDelayChanged = { viewModel.onStepDelayChanged(it) },
                                    onMaxStepsChanged = { viewModel.onMaxStepsChanged(it) }
                                )

                                ApiKeySection(
                                    apiKey = apiKey,
                                    isApiKeyVisible = isApiKeyVisible,
                                    selectedModel = selectedModel,
                                    apiTestStatus = apiTestStatus,
                                    isTestingApi = isTestingApi,
                                    onApiKeyChanged = { viewModel.onApiKeyChanged(it) },
                                    onToggleVisibility = { viewModel.toggleApiKeyVisibility() },
                                    onModelSelected = { viewModel.onModelSelected(it) },
                                    onTestClick = { viewModel.testApiKeyConnection() }
                                )

                                Spacer(modifier = Modifier.height(24.dp))
                            }
                        }

                        1 -> {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(vertical = 16.dp)
                            ) {
                                ExecutionLogSection(
                                    logs = logs,
                                    onClearLogs = { viewModel.clearLogs() }
                                )
                            }
                        }

                        2 -> {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(vertical = 16.dp)
                            ) {
                                NodeExplorerSection(
                                    nodes = capturedNodes,
                                    isServiceConnected = isServiceConnected,
                                    onInspectClick = { viewModel.inspectCurrentScreen() }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
