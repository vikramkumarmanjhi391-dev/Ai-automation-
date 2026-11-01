package com.example.agent

import android.util.Log
import com.example.api.GeminiApiClient
import com.example.model.AgentAction
import com.example.model.ExecutionLogItem
import com.example.service.AutomationServiceManager
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.UUID

/**
 * High-level automation coordinator that executes multi-step goals:
 * 1. Captures UI nodes from screen.
 * 2. Asks Gemini for the next action.
 * 3. Dispatches physical tap, swipe, or keyboard gestures via AccessibilityService.
 * 4. Repeats until finished or stopped.
 */
class AgentOrchestrator(
    private val geminiClient: GeminiApiClient = GeminiApiClient()
) {
    private val scope = CoroutineScope(Dispatchers.Default)
    private var activeJob: Job? = null

    companion object {
        private const val TAG = "AgentOrchestrator"
    }

    fun startAutomation(
        apiKey: String,
        model: String,
        goal: String,
        maxSteps: Int = 12,
        delayBetweenStepsMs: Long = 2000L
    ) {
        if (apiKey.isBlank()) {
            AutomationServiceManager.updateStatus("Please enter your Gemini API Key in the settings.")
            return
        }

        val service = AutomationServiceManager.activeService
        if (service == null) {
            AutomationServiceManager.updateStatus("Accessibility Service is not enabled. Please enable it in Settings.")
            return
        }

        if (goal.isBlank()) {
            AutomationServiceManager.updateStatus("Please provide a goal for the AI agent.")
            return
        }

        stopAutomation()

        activeJob = scope.launch {
            AutomationServiceManager.setAgentRunning(true, "Starting AI Automation...")
            AutomationServiceManager.setGoal(goal)
            AutomationServiceManager.updateStep(0)

            val actionHistory = mutableListOf<String>()

            try {
                for (step in 1..maxSteps) {
                    if (!AutomationServiceManager.isAgentRunning.value) {
                        Log.i(TAG, "Agent execution cancelled by user")
                        break
                    }

                    AutomationServiceManager.updateStep(step)
                    AutomationServiceManager.updateStatus("Step $step/$maxSteps: Inspecting screen nodes...")

                    // 1. Capture screen nodes
                    val nodes = service.captureScreenNodes()
                    Log.d(TAG, "Captured ${nodes.size} UI nodes on screen")

                    AutomationServiceManager.updateStatus("Step $step/$maxSteps: Consulting Gemini AI...")

                    // 2. Query Gemini
                    val decisionResult = geminiClient.decideNextAction(
                        apiKey = apiKey,
                        model = model,
                        goal = goal,
                        currentNodes = nodes,
                        actionHistory = actionHistory
                    )

                    if (decisionResult.isFailure) {
                        val error = decisionResult.exceptionOrNull()
                        val errorMsg = error?.localizedMessage ?: "Unknown API failure"
                        Log.e(TAG, "Gemini decision failed", error)

                        AutomationServiceManager.addLog(
                            ExecutionLogItem(
                                id = UUID.randomUUID().toString(),
                                stepNumber = step,
                                timestamp = System.currentTimeMillis(),
                                actionType = "ERROR",
                                thought = "API Call Failed",
                                target = "Gemini",
                                details = errorMsg,
                                isSuccess = false
                            )
                        )
                        AutomationServiceManager.setAgentRunning(false, "Failed at step $step: $errorMsg")
                        break
                    }

                    val action = decisionResult.getOrThrow()
                    Log.i(TAG, "Gemini decision: ${action.action} (${action.targetDescription})")

                    // 3. Format and record log
                    val detailsSummary = buildActionDetails(action)
                    AutomationServiceManager.addLog(
                        ExecutionLogItem(
                            id = UUID.randomUUID().toString(),
                            stepNumber = step,
                            timestamp = System.currentTimeMillis(),
                            actionType = action.action.uppercase(),
                            thought = action.thought,
                            target = action.targetDescription.ifEmpty { "Screen Element" },
                            details = detailsSummary,
                            isSuccess = true
                        )
                    )

                    // 4. Handle finish condition
                    if (action.action.equals("finish", ignoreCase = true)) {
                        val status = action.status ?: "success"
                        val finishMsg = if (status == "success") {
                            "Goal Accomplished! ${action.targetDescription}"
                        } else {
                            "Task stopped: ${action.targetDescription}"
                        }
                        AutomationServiceManager.setAgentRunning(false, finishMsg)
                        break
                    }

                    // 5. Execute action via AccessibilityService
                    AutomationServiceManager.updateStatus("Step $step/$maxSteps: Executing ${action.action} (${action.targetDescription})...")
                    val executed = service.executeAgentAction(action)

                    if (!executed) {
                        Log.w(TAG, "Action execution returned false")
                    }

                    actionHistory.add("${action.action} -> ${action.targetDescription}")

                    // 6. Pause to let target app / screen settle
                    delay(delayBetweenStepsMs)
                }

                if (AutomationServiceManager.isAgentRunning.value) {
                    AutomationServiceManager.setAgentRunning(false, "Completed maximum allowed steps ($maxSteps).")
                }

            } catch (e: CancellationException) {
                AutomationServiceManager.setAgentRunning(false, "Automation cancelled.")
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Error in automation loop", e)
                AutomationServiceManager.setAgentRunning(false, "Error: ${e.localizedMessage}")
            }
        }
    }

    fun stopAutomation() {
        activeJob?.cancel()
        activeJob = null
        AutomationServiceManager.setAgentRunning(false, "Automation stopped.")
    }

    private fun buildActionDetails(action: AgentAction): String {
        return when (action.action.lowercase()) {
            "tap" -> "Tap at (${action.x?.toInt()}, ${action.y?.toInt()})"
            "tap_node" -> "Tap node #${action.nodeId}"
            "swipe" -> "Swipe from (${action.startX?.toInt()}, ${action.startY?.toInt()}) to (${action.endX?.toInt()}, ${action.endY?.toInt()})"
            "input_text" -> "Type \"${action.text}\" into node #${action.nodeId}"
            "press_back" -> "Press Back Button"
            "press_home" -> "Press Home Button"
            "wait" -> "Wait for ${action.durationMs ?: 2000}ms"
            "finish" -> "Finish task with status: ${action.status ?: "success"}"
            else -> action.action
        }
    }
}
