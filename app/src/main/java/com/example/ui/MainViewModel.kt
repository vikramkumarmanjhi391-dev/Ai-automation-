package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.agent.AgentOrchestrator
import com.example.api.GeminiApiClient
import com.example.data.PreferencesManager
import com.example.model.ExecutionLogItem
import com.example.model.UiNodeInfo
import com.example.service.AutomationServiceManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel managing UI interactions, API configuration, screen inspection,
 * and automation agent lifecycle.
 */
class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val prefs = PreferencesManager(application)
    private val geminiClient = GeminiApiClient()
    private val orchestrator = AgentOrchestrator(geminiClient)

    private val _apiKey = MutableStateFlow(prefs.apiKey)
    val apiKey: StateFlow<String> = _apiKey.asStateFlow()

    private val _isApiKeyVisible = MutableStateFlow(false)
    val isApiKeyVisible: StateFlow<Boolean> = _isApiKeyVisible.asStateFlow()

    private val _selectedModel = MutableStateFlow(prefs.selectedModel)
    val selectedModel: StateFlow<String> = _selectedModel.asStateFlow()

    private val _stepDelay = MutableStateFlow(prefs.stepDelaySeconds)
    val stepDelay: StateFlow<Float> = _stepDelay.asStateFlow()

    private val _maxSteps = MutableStateFlow(prefs.maxSteps)
    val maxSteps: StateFlow<Int> = _maxSteps.asStateFlow()

    private val _goalInput = MutableStateFlow("Open Settings and find Wi-Fi networks")
    val goalInput: StateFlow<String> = _goalInput.asStateFlow()

    private val _apiTestStatus = MutableStateFlow<String?>(null)
    val apiTestStatus: StateFlow<String?> = _apiTestStatus.asStateFlow()

    private val _isTestingApi = MutableStateFlow(false)
    val isTestingApi: StateFlow<Boolean> = _isTestingApi.asStateFlow()

    val isServiceConnected: StateFlow<Boolean> = AutomationServiceManager.isServiceConnected
    val isAgentRunning: StateFlow<Boolean> = AutomationServiceManager.isAgentRunning
    val currentStatus: StateFlow<String> = AutomationServiceManager.currentStatus
    val currentStep: StateFlow<Int> = AutomationServiceManager.currentStep
    val logs: StateFlow<List<ExecutionLogItem>> = AutomationServiceManager.logs
    val capturedNodes: StateFlow<List<UiNodeInfo>> = AutomationServiceManager.capturedNodes
    val isOverlayActive: StateFlow<Boolean> = AutomationServiceManager.isOverlayActive

    fun onApiKeyChanged(key: String) {
        _apiKey.value = key
        prefs.apiKey = key
        _apiTestStatus.value = null
    }

    fun toggleApiKeyVisibility() {
        _isApiKeyVisible.value = !_isApiKeyVisible.value
    }

    fun onModelSelected(model: String) {
        _selectedModel.value = model
        prefs.selectedModel = model
        _apiTestStatus.value = null
    }

    fun onStepDelayChanged(delay: Float) {
        _stepDelay.value = delay
        prefs.stepDelaySeconds = delay
    }

    fun onMaxStepsChanged(steps: Int) {
        _maxSteps.value = steps
        prefs.maxSteps = steps
    }

    fun onGoalChanged(goal: String) {
        _goalInput.value = goal
    }

    fun testApiKeyConnection() {
        val key = _apiKey.value.trim()
        if (key.isBlank()) {
            _apiTestStatus.value = "Error: Please enter an API key first."
            return
        }

        viewModelScope.launch {
            _isTestingApi.value = true
            _apiTestStatus.value = "Testing connection to Gemini..."
            val result = geminiClient.testApiKey(key, _selectedModel.value)
            if (result.isSuccess) {
                _apiTestStatus.value = "✓ " + result.getOrThrow()
            } else {
                val error = result.exceptionOrNull()?.localizedMessage ?: "Connection failed"
                _apiTestStatus.value = "✗ $error"
            }
            _isTestingApi.value = false
        }
    }

    fun startAutomation() {
        val key = _apiKey.value.trim()
        val model = _selectedModel.value
        val goal = _goalInput.value.trim()
        val delayMs = (_stepDelay.value * 1000).toLong()

        orchestrator.startAutomation(
            apiKey = key,
            model = model,
            goal = goal,
            maxSteps = _maxSteps.value,
            delayBetweenStepsMs = delayMs
        )
    }

    fun stopAutomation() {
        orchestrator.stopAutomation()
    }

    fun toggleFloatingOverlay() {
        val service = AutomationServiceManager.activeService
        if (service == null) {
            AutomationServiceManager.updateStatus("Enable Accessibility Service first to use the floating overlay.")
            return
        }

        if (isOverlayActive.value) {
            service.hideOverlay()
            prefs.isFloatingOverlayEnabled = false
        } else {
            service.showOverlay()
            prefs.isFloatingOverlayEnabled = true
        }
    }

    fun inspectCurrentScreen() {
        val service = AutomationServiceManager.activeService
        if (service == null) {
            AutomationServiceManager.updateStatus("Accessibility Service is not enabled.")
            return
        }
        val nodes = service.captureScreenNodes()
        AutomationServiceManager.updateStatus("Inspected screen: ${nodes.size} UI elements identified.")
    }

    fun clearLogs() {
        AutomationServiceManager.clearLogs()
    }
}
