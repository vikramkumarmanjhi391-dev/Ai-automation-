package com.example.api

import android.util.Log
import com.example.model.AgentAction
import com.example.model.UiNodeInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * Direct Gemini API client communicating with Google Generative Language endpoints.
 * Handles structured JSON decision making and API key validation.
 */
class GeminiApiClient {
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    companion object {
        private const val TAG = "GeminiApiClient"
        private const val BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models"
        private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()

        private val SYSTEM_INSTRUCTION = """
            You are an Android AI Automation Agent. You are operating an Android device on behalf of the user to achieve a specific goal.
            You are given:
            1. The User's Goal.
            2. The history of actions already executed in previous steps.
            3. A structured list of all visible UI nodes currently on the Android screen, including their Node ID, type, text label/description, and exact screen coordinates (bounds and center (x, y)).

            Analyze the screen and select the single best next action to advance toward the goal.
            
            Supported actions:
            - "tap": Click at coordinate (x, y). Best for clicking buttons, icons, or items.
              Provide: "action": "tap", "x": <int>, "y": <int>, "targetDescription": "<label>"
            - "tap_node": Click a specific node ID.
              Provide: "action": "tap_node", "nodeId": <id>, "targetDescription": "<label>"
            - "swipe": Swipe or scroll the screen.
              Provide: "action": "swipe", "startX": <int>, "startY": <int>, "endX": <int>, "endY": <int>, "durationMs": 350, "targetDescription": "<reason>"
              (e.g., scroll down: startY=1400, endY=500. Scroll up: startY=500, endY=1400)
            - "input_text": Type text into an editable input field.
              Provide: "action": "input_text", "nodeId": <id>, "text": "<string to type>", "targetDescription": "<field name>"
            - "press_back": Navigate back.
              Provide: "action": "press_back", "targetDescription": "<reason>"
            - "press_home": Return to device home screen.
              Provide: "action": "press_home", "targetDescription": "<reason>"
            - "wait": Pause for page or content loading.
              Provide: "action": "wait", "durationMs": 2000, "targetDescription": "<reason>"
            - "finish": Stop because the goal has been successfully reached, or cannot proceed.
              Provide: "action": "finish", "status": "success" or "failed", "targetDescription": "<final outcome message>"

            CRITICAL RULES:
            1. Output ONLY valid raw JSON conforming to this schema. No markdown formatting, no explanations outside the JSON.
            2. In the "thought" field, briefly explain what you see on the screen and why you chose this action.
            3. Use the exact center coordinates (centerX, centerY) of the target node when using "tap".
            4. If the goal is completed, return "action": "finish" with "status": "success".
            5. If stuck or the screen shows an unexpected blocker, try an alternative action or return "finish" with "status": "failed".
        """.trimIndent()
    }

    /**
     * Validates if the provided API key works with Gemini.
     */
    suspend fun testApiKey(apiKey: String, model: String): Result<String> = withContext(Dispatchers.IO) {
        if (apiKey.isBlank()) {
            return@withContext Result.failure(IllegalArgumentException("API Key cannot be blank"))
        }

        val url = "$BASE_URL/$model:generateContent?key=$apiKey"
        val requestJson = JSONObject().apply {
            put("contents", JSONArray().apply {
                put(JSONObject().apply {
                    put("role", "user")
                    put("parts", JSONArray().apply {
                        put(JSONObject().apply { put("text", "Respond with 'OK'") })
                    })
                })
            })
        }

        val request = Request.Builder()
            .url(url)
            .post(requestJson.toString().toRequestBody(JSON_MEDIA_TYPE))
            .build()

        try {
            client.newCall(request).execute().use { response ->
                val body = response.body?.string().orEmpty()
                if (response.isSuccessful) {
                    Result.success("Connection successful! Model $model is active and ready.")
                } else {
                    val errorMessage = parseErrorMessage(body, response.code)
                    Result.failure(IOException(errorMessage))
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "testApiKey error", e)
            Result.failure(e)
        }
    }

    /**
     * Queries Gemini with the current screen node layout and user goal
     * to determine the next physical or navigation action.
     */
    suspend fun decideNextAction(
        apiKey: String,
        model: String,
        goal: String,
        currentNodes: List<UiNodeInfo>,
        actionHistory: List<String>
    ): Result<AgentAction> = withContext(Dispatchers.IO) {
        if (apiKey.isBlank()) {
            return@withContext Result.failure(IllegalStateException("Gemini API key is not configured."))
        }

        val url = "$BASE_URL/$model:generateContent?key=$apiKey"

        val userPrompt = buildString {
            appendLine("=== USER GOAL ===")
            appendLine(goal)
            appendLine()
            appendLine("=== ACTION HISTORY (PREVIOUS STEPS) ===")
            if (actionHistory.isEmpty()) {
                appendLine("No previous actions taken yet. This is step 1.")
            } else {
                actionHistory.forEachIndexed { index, hist ->
                    appendLine("Step ${index + 1}: $hist")
                }
            }
            appendLine()
            appendLine("=== CURRENT VISIBLE ON-SCREEN UI NODES (${currentNodes.size} nodes found) ===")
            if (currentNodes.isEmpty()) {
                appendLine("Warning: No UI nodes were detected in the active window. You may need to wait or press home.")
            } else {
                currentNodes.take(120).forEach { node ->
                    appendLine(node.toPromptSummary())
                }
            }
            appendLine()
            appendLine("Decide the single next action to take. Respond with strict JSON.")
        }

        val requestJson = JSONObject().apply {
            put("system_instruction", JSONObject().apply {
                put("parts", JSONArray().apply {
                    put(JSONObject().apply { put("text", SYSTEM_INSTRUCTION) })
                })
            })
            put("contents", JSONArray().apply {
                put(JSONObject().apply {
                    put("role", "user")
                    put("parts", JSONArray().apply {
                        put(JSONObject().apply { put("text", userPrompt) })
                    })
                })
            })
            put("generationConfig", JSONObject().apply {
                put("responseMimeType", "application/json")
                put("temperature", 0.2)
            })
        }

        val request = Request.Builder()
            .url(url)
            .post(requestJson.toString().toRequestBody(JSON_MEDIA_TYPE))
            .build()

        try {
            client.newCall(request).execute().use { response ->
                val body = response.body?.string().orEmpty()
                if (!response.isSuccessful) {
                    val err = parseErrorMessage(body, response.code)
                    return@withContext Result.failure(IOException(err))
                }

                val action = parseActionFromJson(body)
                Result.success(action)
            }
        } catch (e: Exception) {
            Log.e(TAG, "decideNextAction failed", e)
            Result.failure(e)
        }
    }

    private fun parseActionFromJson(responseBody: String): AgentAction {
        val root = JSONObject(responseBody)
        val candidates = root.optJSONArray("candidates")
            ?: throw IllegalStateException("No candidates returned from Gemini.")

        if (candidates.length() == 0) {
            throw IllegalStateException("Empty candidates list from Gemini.")
        }

        val firstCandidate = candidates.getJSONObject(0)
        val content = firstCandidate.getJSONObject("content")
        val parts = content.getJSONArray("parts")
        val rawText = parts.getJSONObject(0).getString("text")

        val cleanJson = rawText.trim()
            .removePrefix("```json")
            .removePrefix("```")
            .removeSuffix("```")
            .trim()

        val actionObj = JSONObject(cleanJson)
        val actionType = actionObj.optString("action", "wait").lowercase()
        val thought = actionObj.optString("thought", "")
        val targetDesc = actionObj.optString("targetDescription", "")
        val nodeId = if (actionObj.has("nodeId")) actionObj.optInt("nodeId") else null
        val x = if (actionObj.has("x")) actionObj.optDouble("x").toFloat() else null
        val y = if (actionObj.has("y")) actionObj.optDouble("y").toFloat() else null
        val startX = if (actionObj.has("startX")) actionObj.optDouble("startX").toFloat() else null
        val startY = if (actionObj.has("startY")) actionObj.optDouble("startY").toFloat() else null
        val endX = if (actionObj.has("endX")) actionObj.optDouble("endX").toFloat() else null
        val endY = if (actionObj.has("endY")) actionObj.optDouble("endY").toFloat() else null
        val text = actionObj.optString("text", null)
        val durationMs = if (actionObj.has("durationMs")) actionObj.optLong("durationMs") else null
        val status = actionObj.optString("status", null)

        return AgentAction(
            action = actionType,
            thought = thought,
            targetDescription = targetDesc,
            nodeId = nodeId,
            x = x,
            y = y,
            startX = startX,
            startY = startY,
            endX = endX,
            endY = endY,
            text = text,
            durationMs = durationMs,
            status = status
        )
    }

    private fun parseErrorMessage(responseBody: String, httpCode: Int): String {
        return try {
            val json = JSONObject(responseBody)
            val errorObj = json.optJSONObject("error")
            val message = errorObj?.optString("message") ?: "HTTP error $httpCode"
            "Gemini API Error ($httpCode): $message"
        } catch (_: Exception) {
            "Gemini API returned error code $httpCode: ${responseBody.take(150)}"
        }
    }
}
