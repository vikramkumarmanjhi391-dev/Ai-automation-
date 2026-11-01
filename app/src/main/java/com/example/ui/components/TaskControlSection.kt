package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.CrimsonError
import com.example.ui.theme.DarkBorder
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceVariant
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.NeonViolet
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TaskControlSection(
    goalInput: String,
    isAgentRunning: Boolean,
    currentStatus: String,
    currentStep: Int,
    maxSteps: Int,
    stepDelay: Float,
    isOverlayActive: Boolean,
    isServiceConnected: Boolean,
    onGoalChanged: (String) -> Unit,
    onStartAutomation: () -> Unit,
    onStopAutomation: () -> Unit,
    onToggleOverlay: () -> Unit,
    onStepDelayChanged: (Float) -> Unit,
    onMaxStepsChanged: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    var showAdvancedSettings by remember { mutableStateOf(false) }

    val presetGoals = listOf(
        "Open Settings and tap Wi-Fi",
        "Open Chrome and tap search",
        "Check Battery settings",
        "Open YouTube and search music"
    )

    Card(
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, if (isAgentRunning) ElectricCyan.copy(alpha = 0.6f) else DarkBorder, RoundedCornerShape(16.dp))
            .testTag("task_control_card"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = null,
                    tint = NeonViolet,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "AI Automation Task",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Goal Input
            OutlinedTextField(
                value = goalInput,
                onValueChange = onGoalChanged,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("goal_input_field"),
                label = { Text("What should the AI agent do?") },
                placeholder = { Text("e.g. Open Settings, scroll down and click Display") },
                maxLines = 3,
                enabled = !isAgentRunning,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = ElectricCyan,
                    unfocusedBorderColor = DarkBorder,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary,
                    focusedContainerColor = DarkSurfaceVariant,
                    unfocusedContainerColor = DarkSurfaceVariant
                ),
                shape = RoundedCornerShape(10.dp)
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Preset Goal Chips
            Text(
                text = "Quick Task Presets:",
                style = MaterialTheme.typography.labelSmall,
                color = TextSecondary
            )
            Spacer(modifier = Modifier.height(4.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                presetGoals.forEach { preset ->
                    Box(
                        modifier = Modifier
                            .background(DarkSurfaceVariant, RoundedCornerShape(16.dp))
                            .border(1.dp, DarkBorder, RoundedCornerShape(16.dp))
                            .clickable(enabled = !isAgentRunning) { onGoalChanged(preset) }
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = preset,
                            style = MaterialTheme.typography.bodySmall,
                            color = ElectricCyan,
                            fontSize = 11.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Start / Stop Primary Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (!isAgentRunning) {
                    Button(
                        onClick = onStartAutomation,
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp)
                            .testTag("start_automation_button"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ElectricCyan,
                            contentColor = Color.Black
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "START AUTOMATION",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                } else {
                    Button(
                        onClick = onStopAutomation,
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp)
                            .testTag("stop_automation_button"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = CrimsonError,
                            contentColor = Color.White
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Stop,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "EMERGENCY STOP",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Floating Overlay Switch
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(DarkSurfaceVariant, RoundedCornerShape(10.dp))
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Layers,
                        contentDescription = null,
                        tint = ElectricCyan,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "Floating Overlay Widget",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = TextPrimary
                        )
                        Text(
                            text = "Controls agent while outside this app",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary
                        )
                    }
                }

                Switch(
                    checked = isOverlayActive,
                    onCheckedChange = { onToggleOverlay() },
                    enabled = isServiceConnected,
                    modifier = Modifier.testTag("toggle_floating_overlay_switch"),
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.Black,
                        checkedTrackColor = ElectricCyan,
                        uncheckedThumbColor = TextSecondary,
                        uncheckedTrackColor = DarkBorder
                    )
                )
            }

            // Live Status Banner
            Spacer(modifier = Modifier.height(10.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        if (isAgentRunning) ElectricCyan.copy(alpha = 0.1f) else DarkSurfaceVariant,
                        RoundedCornerShape(8.dp)
                    )
                    .border(
                        1.dp,
                        if (isAgentRunning) ElectricCyan.copy(alpha = 0.3f) else DarkBorder,
                        RoundedCornerShape(8.dp)
                    )
                    .padding(10.dp)
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (isAgentRunning) "Agent Running (Step $currentStep / $maxSteps)" else "Status",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (isAgentRunning) ElectricCyan else TextSecondary
                        )

                        Text(
                            text = if (isAgentRunning) "ACTIVE" else "READY",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (isAgentRunning) EmeraldSuccess else TextSecondary
                        )
                    }

                    if (isAgentRunning) {
                        Spacer(modifier = Modifier.height(6.dp))
                        LinearProgressIndicator(
                            progress = { (currentStep.toFloat() / maxSteps.coerceAtLeast(1)).coerceIn(0f, 1f) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(4.dp),
                            color = ElectricCyan,
                            trackColor = DarkBorder,
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = currentStatus,
                        style = MaterialTheme.typography.bodySmall,
                        color = TextPrimary
                    )
                }
            }

            // Advanced settings toggle
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = if (showAdvancedSettings) "Hide Safety & Gesture Settings ▲" else "Safety & Gesture Settings ▼",
                style = MaterialTheme.typography.labelSmall,
                color = ElectricCyan,
                fontWeight = FontWeight.Medium,
                modifier = Modifier
                    .clickable { showAdvancedSettings = !showAdvancedSettings }
                    .padding(vertical = 4.dp)
            )

            AnimatedVisibility(visible = showAdvancedSettings) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                ) {
                    Text(
                        text = "Step Delay: ${String.format(Locale.US, "%.1f", stepDelay)}s (wait between actions)",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                    Slider(
                        value = stepDelay,
                        onValueChange = onStepDelayChanged,
                        valueRange = 1.0f..4.0f,
                        steps = 5,
                        colors = SliderDefaults.colors(
                            thumbColor = ElectricCyan,
                            activeTrackColor = ElectricCyan,
                            inactiveTrackColor = DarkBorder
                        )
                    )

                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Max Step Limit: $maxSteps steps per task",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                    Slider(
                        value = maxSteps.toFloat(),
                        onValueChange = { onMaxStepsChanged(it.toInt()) },
                        valueRange = 5f..20f,
                        steps = 14,
                        colors = SliderDefaults.colors(
                            thumbColor = NeonViolet,
                            activeTrackColor = NeonViolet,
                            inactiveTrackColor = DarkBorder
                        )
                    )
                }
            }
        }
    }
}
