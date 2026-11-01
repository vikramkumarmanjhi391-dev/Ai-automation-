package com.example.service

import com.example.model.ExecutionLogItem
import com.example.model.UiNodeInfo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Global shared manager holding the active AccessibilityService reference,
 * agent lifecycle states, and live execution logs.
 */
object AutomationServiceManager {
    private val _isServiceConnected = MutableStateFlow(false)
    val isServiceConnected = _isServiceConnected.asStateFlow()

    private val _isAgentRunning = MutableStateFlow(false)
    val isAgentRunning = _isAgentRunning.asStateFlow()

    private val _currentStatus = MutableStateFlow("Service is ready. Configure API key and start automation.")
    val currentStatus = _currentStatus.asStateFlow()

    private val _logs = MutableStateFlow<List<ExecutionLogItem>>(emptyList())
    val logs = _logs.asStateFlow()

    private val _capturedNodes = MutableStateFlow<List<UiNodeInfo>>(emptyList())
    val capturedNodes = _capturedNodes.asStateFlow()

    private val _currentStep = MutableStateFlow(0)
    val currentStep = _currentStep.asStateFlow()

    private val _currentGoal = MutableStateFlow("")
    val currentGoal = _currentGoal.asStateFlow()

    private val _isOverlayActive = MutableStateFlow(false)
    val isOverlayActive = _isOverlayActive.asStateFlow()

    @Volatile
    var activeService: AiAutomationAccessibilityService? = null
        private set

    fun registerService(service: AiAutomationAccessibilityService) {
        activeService = service
        _isServiceConnected.value = true
        _currentStatus.value = "Accessibility Service connected & operational."
    }

    fun unregisterService() {
        activeService = null
        _isServiceConnected.value = false
        _isAgentRunning.value = false
        _currentStatus.value = "Accessibility Service disconnected."
    }

    fun setAgentRunning(running: Boolean, status: String = "") {
        _isAgentRunning.value = running
        if (status.isNotBlank()) {
            _currentStatus.value = status
        }
    }

    fun updateStatus(status: String) {
        _currentStatus.value = status
    }

    fun updateStep(step: Int) {
        _currentStep.value = step
    }

    fun setGoal(goal: String) {
        _currentGoal.value = goal
    }

    fun setCapturedNodes(nodes: List<UiNodeInfo>) {
        _capturedNodes.value = nodes
    }

    fun addLog(log: ExecutionLogItem) {
        _logs.value = listOf(log) + _logs.value.take(49)
    }

    fun clearLogs() {
        _logs.value = emptyList()
    }

    fun setOverlayActive(active: Boolean) {
        _isOverlayActive.value = active
    }
}
