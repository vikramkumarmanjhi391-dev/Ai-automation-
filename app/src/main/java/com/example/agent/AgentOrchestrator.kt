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
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.UUID

/**
 * High-level automation coordinator:
 * 1. Captures visible UI nodes.
 * 2. Asks Gemini for exactly one next action.
 * 3. Executes that action through the user's enabled Accessibility Service.
 * 4. Records the real execution outcome and repeats.
 */
class AgentOrchestrator(
    private val geminiClient: GeminiApiClient = GeminiApiClient()
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
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

        // Cancel the previous run and wait for its cancellation handler before starting a new one.
        val previousJob = activeJob
        previousJob?.cancel()

        activeJob = scope.launch {
            previousJob?.join()
            AutomationServiceManager.setAgentRunning(true, "Starting AI Automation...")
            AutomationServiceManager.setGoal(goal)
            AutomationServiceManager.updateStep(0)

            val actionHistory = mutableListOf<String>()

            try {
                for (step in 1..maxSteps.coerceAtLeast(1)) {
                    if (!AutomationServiceManager.isAgentRunning.value) {
                        Log.i(TAG, "Agent execution cancelled by user")
                        break
                    }

                    AutomationServiceManager.updateStep(step)
                    AutomationServiceManager.updateStatus("Step $step/$maxSteps: Inspecting screen nodes...")

                    val nodes = service.captureScreenNodes()
                    Log.d(TAG, "Captured ${nodes.size} UI nodes on screen")

                    AutomationServiceManager.updateStatus("Step $step/$maxSteps: Consulting Gemini AI...")
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
                                thought = "Gemini request failed",
                                target = "Gemini API",
                                details = errorMsg,
                                isSuccess = false
                            )
                        )
                        AutomationServiceManager.setAgentRunning(false, "Gemini request failed at step $step: $errorMsg")
                        break
                    }

                    val action = decisionResult.getOrThrow()
                    Log.i(TAG, "Gemini decision: ${action.action} (${action.targetDescription})")
                    val detailsSummary = buildActionDetails(action)

                    if (action.action.equals("finish", ignoreCase = true)) {
                        val status = action.status ?: "success"
                        val succeeded = status.equals("success", ignoreCase = true)
                        AutomationServiceManager.addLog(
                            ExecutionLogItem(
                                id = UUID.randomUUID().toString(),
                                stepNumber = step,
                                timestamp = System.currentTimeMillis(),
                                actionType = "FINISH",
                                thought = action.thought,
                                target = action.targetDescription.ifEmpty { "Task result" },
                                details = detailsSummary,
                                isSuccess = succeeded
                            )
                        )

                        val finishMsg = if (succeeded) {
                            "Goal Accomplished! ${action.targetDescription}"
                        } else {
                            "Task stopped: ${action.targetDescription}"
                        }
                        AutomationServiceManager.setAgentRunning(false, finishMsg)
                        break
                    }

                    AutomationServiceManager.updateStatus(
                        "Step $step/$maxSteps: Executing ${action.action} (${action.targetDescription})..."
                    )
                    val executed = service.executeAgentAction(action)

                    // Store the outcome, not merely the model's proposed action.
                    val outcome = if (executed) "executed" else "execution failed"
                    AutomationServiceManager.addLog(
                        ExecutionLogItem(
                            id = UUID.randomUUID().toString(),
                            stepNumber = step,
                            timestamp = System.currentTimeMillis(),
                            actionType = action.action.uppercase(),
                            thought = if (action.action.equals("input_text", ignoreCase = true)) "Text entry action (content hidden)" else action.thought,
                            target = action.targetDescription.ifEmpty { "Screen Element" },
                            details = if (executed) detailsSummary else "$detailsSummary (not completed)",
                            isSuccess = executed
                        )
                    )

                    if (!executed) {
                        Log.w(TAG, "Action execution returned false; requesting a new decision from the current screen.")
                        AutomationServiceManager.updateStatus(
                            "Step $step/$maxSteps: Action did not complete. Rechecking the screen..."
                        )
                    }

                    actionHistory.add("${action.action} -> ${action.targetDescription} ($outcome)")
                    delay(delayBetweenStepsMs.coerceAtLeast(0L))
                }

                if (AutomationServiceManager.isAgentRunning.value) {
                    AutomationServiceManager.setAgentRunning(
                        false,
                        "Reached the maximum allowed steps (${maxSteps.coerceAtLeast(1)})."
                    )
                }
            } catch (e: CancellationException) {
                if (AutomationServiceManager.isAgentRunning.value) {
                    AutomationServiceManager.setAgentRunning(false, "Automation cancelled.")
                }
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Error in automation loop", e)
                AutomationServiceManager.setAgentRunning(false, "Automation error: ${e.localizedMessage ?: "Unknown error"}")
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
            // Do not retain the actual entered text in the execution log.
            "input_text" -> "Enter text into node #${action.nodeId} (content hidden)"
            "press_back" -> "Press Back Button"
            "press_home" -> "Press Home Button"
            "wait" -> "Wait for ${action.durationMs ?: 2000}ms"
            "finish" -> "Finish task with status: ${action.status ?: "success"}"
            else -> action.action
        }
    }
}
