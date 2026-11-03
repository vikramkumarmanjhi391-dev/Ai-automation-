package com.example.api

import android.util.Log
import com.example.model.AgentAction
import com.example.model.UiNodeInfo
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.suspendCancellableCoroutine
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.net.ConnectException
import java.net.NoRouteToHostException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.util.concurrent.TimeUnit
import kotlin.random.Random
import kotlin.coroutines.resume
import java.io.InterruptedIOException

/**
 * Direct Gemini API client communicating with Google Generative Language endpoints.
 *
 * Network requests are cancellable, use bounded timeouts, and retry only transient failures.
 * API keys are sent in the x-goog-api-key header rather than in the request URL.
 */
class GeminiApiClient {
    private val client = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(90, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .callTimeout(120, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .build()

    companion object {
        private const val TAG = "GeminiApiClient"
        private const val BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models"
        private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()

        private const val MAX_RETRIES = 2
        private const val BASE_RETRY_DELAY_MS = 1_000L
        private const val MAX_RETRY_DELAY_MS = 15_000L
        private const val MAX_ERROR_BODY_BYTES = 8_192L
        private const val MAX_PROMPT_NODES = 100
        private const val MAX_HISTORY_ITEMS = 8

        private val SYSTEM_INSTRUCTION = """
            You are an Android AI Automation Agent. You are operating an Android device on behalf of the user to achieve a specific goal.
            You are given:
            1. The User's Goal.
            2. The history of actions already executed in previous steps.
            3. A structured list of visible UI nodes currently on the Android screen, including node IDs, labels, and screen coordinates.

            Analyze the screen and select the single best next action to advance toward the goal.

            Supported actions:
            - "tap": Click at coordinate (x, y). Provide action, x, y, and targetDescription.
            - "tap_node": Click a specific node ID. Provide action, nodeId, and targetDescription.
            - "swipe": Scroll or swipe. Provide action, startX, startY, endX, endY, durationMs, and targetDescription.
            - "input_text": Type into an editable input field. Provide action, nodeId, text, and targetDescription.
            - "press_back": Navigate back. Provide action and targetDescription.
            - "press_home": Return to the device home screen. Provide action and targetDescription.
            - "wait": Pause for content loading. Provide action, durationMs, and targetDescription.
            - "finish": Stop because the goal was reached or cannot proceed. Provide action, status ("success" or "failed"), and targetDescription.

            CRITICAL RULES:
            1. Output ONLY valid raw JSON. No markdown or explanations outside the JSON.
            2. The "thought" field must contain only a brief, user-facing action rationale; do not provide hidden chain-of-thought.
            3. For tap coordinates, use the exact centerX and centerY of the target node.
            4. Prefer an enabled, visible, relevant node. Do not invent node IDs or coordinates.
            5. If the goal is completed, return finish with status "success".
            6. If blocked by permissions, authentication, or an unexpected screen, do not guess; return finish with status "failed".
            7. Never complete or confirm a payment, purchase, message/post send, account/security change, deletion, permission grant, or sharing of personal data/location. Stop with finish status "failed" and clearly state that the user must review and confirm manually.
        """.trimIndent()
    }

    /**
     * Validates whether the provided API key and selected model can make a Gemini request.
     */
    suspend fun testApiKey(apiKey: String, model: String): Result<String> {
        if (apiKey.isBlank()) {
            return Result.failure(IllegalArgumentException("API key cannot be blank."))
        }

        val requestJson = JSONObject().apply {
            put("contents", JSONArray().apply {
                put(JSONObject().apply {
                    put("role", "user")
                    put("parts", JSONArray().apply {
                        put(JSONObject().apply { put("text", "Respond with OK.") })
                    })
                })
            })
            put("generationConfig", JSONObject().apply {
                put("thinkingConfig", JSONObject().put("thinkingLevel", "low"))
                put("responseMimeType", "application/json")
                put("maxOutputTokens", 512)
            })
        }

        val request = buildRequest(apiKey, model, requestJson)
        return try {
            executeWithRetry(request).use { response ->
                val body = response.body?.string().orEmpty()
                if (response.isSuccessful) {
                    Result.success("Connection successful. Model $model is responding.")
                } else {
                    Result.failure(IOException(parseErrorMessage(body, response.code)))
                }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "API key test failed ( ${e.javaClass.simpleName} ).", e)
            Result.failure(e)
        }
    }

    /**
     * Queries Gemini for one next action using a compact screen snapshot and recent action history.
     */
    suspend fun decideNextAction(
        apiKey: String,
        model: String,
        goal: String,
        currentNodes: List<UiNodeInfo>,
        actionHistory: List<String>
    ): Result<AgentAction> {
        if (apiKey.isBlank()) {
            return Result.failure(IllegalStateException("Gemini API key is not configured."))
        }

        val recentHistory = actionHistory.takeLast(MAX_HISTORY_ITEMS)
        val promptNodes = currentNodes.take(MAX_PROMPT_NODES)
        val userPrompt = buildString {
            appendLine("=== USER GOAL ===")
            appendLine(goal)
            appendLine()
            appendLine("=== RECENT ACTION HISTORY (most recent $MAX_HISTORY_ITEMS steps max) ===")
            if (recentHistory.isEmpty()) {
                appendLine("No previous actions taken yet. This is step 1.")
            } else {
                recentHistory.forEachIndexed { index, hist ->
                    appendLine("Recent step ${index + 1}: $hist")
                }
            }
            appendLine()
            appendLine("=== CURRENT VISIBLE UI NODES (${promptNodes.size} of ${currentNodes.size}) ===")
            if (promptNodes.isEmpty()) {
                appendLine("No UI nodes were detected in the active window. Consider waiting or returning to the home screen.")
            } else {
                promptNodes.forEach { node ->
                    appendLine(node.toPromptSummary())
                }
            }
            appendLine()
            appendLine("Choose exactly one safe next action. Respond with strict JSON.")
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
                put("maxOutputTokens", 512)
                put("thinkingConfig", JSONObject().put("thinkingLevel", "low"))
            })
        }

        val request = buildRequest(apiKey, model, requestJson)
        return try {
            executeWithRetry(request).use { response ->
                val body = response.body?.string().orEmpty()
                if (!response.isSuccessful) {
                    Result.failure(IOException(parseErrorMessage(body, response.code)))
                } else {
                    Result.success(parseActionFromJson(body))
                }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Gemini decision failed ( ${e.javaClass.simpleName} ).", e)
            Result.failure(e)
        }
    }

    private fun buildRequest(apiKey: String, model: String, requestJson: JSONObject): Request {
        return Request.Builder()
            .url("$BASE_URL/$model:generateContent")
            .header("x-goog-api-key", apiKey)
            .header("Accept", "application/json")
            .post(requestJson.toString().toRequestBody(JSON_MEDIA_TYPE))
            .build()
    }

    /**
     * Runs a cancellable OkHttp request. Cancelling the parent coroutine cancels the HTTP call.
     */
    private suspend fun executeCancellable(request: Request): Response =
        suspendCancellableCoroutine { continuation ->
            val call = client.newCall(request)
            continuation.invokeOnCancellation { call.cancel() }

            call.enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    if (continuation.isActive) {
                        continuation.resumeWith(Result.failure(e))
                    }
                }

                override fun onResponse(call: Call, response: Response) {
                    continuation.resume(response) { _, value, _ ->
                        value.close()
                    }
                }
            })
        }

    /**
     * Retries only transient HTTP/network failures. Authentication, invalid-model, malformed
     * request, and exhausted daily quota errors are returned immediately.
     */
    private suspend fun executeWithRetry(request: Request): Response {
        var attempt = 0
        while (true) {
            val attemptStartedAt = System.nanoTime()
            val response = try {
                executeCancellable(request)
            } catch (e: IOException) {
                if (attempt >= MAX_RETRIES || !isRetryableNetworkError(e)) {
                    Log.e(TAG, "Gemini network request failed after ${attempt + 1} attempt(s): ${e.javaClass.simpleName}")
                    throw e
                }

                val waitMs = retryDelayMillis(attempt)
                Log.w(TAG, "Transient Gemini network failure (${e.javaClass.simpleName}); retrying in ${waitMs}ms.")
                attempt += 1
                delay(waitMs)
                continue
            }

            val elapsedMs = (System.nanoTime() - attemptStartedAt) / 1_000_000L
            Log.i(TAG, "Gemini HTTP ${response.code} in ${elapsedMs}ms (attempt ${attempt + 1}).")

            val errorBody = if (response.isSuccessful) {
                ""
            } else {
                response.peekBody(MAX_ERROR_BODY_BYTES).string()
            }

            if (attempt < MAX_RETRIES && shouldRetryHttp(response.code, errorBody)) {
                val waitMs = retryDelayMillis(attempt, response)
                Log.w(TAG, "Transient Gemini HTTP ${response.code}; retrying in ${waitMs}ms.")
                response.close()
                attempt += 1
                delay(waitMs)
                continue
            }

            if (!response.isSuccessful) {
                Log.w(TAG, "Gemini returned HTTP ${response.code} after ${attempt + 1} attempt(s).")
            }
            return response
        }
    }

    private fun isRetryableHttpError(code: Int): Boolean =
        code == 408 || code == 429 || code == 500 || code == 502 || code == 503 || code == 504

    private fun shouldRetryHttp(code: Int, body: String): Boolean {
        if (!isRetryableHttpError(code)) return false
        if (code != 429) return true

        val normalized = body.lowercase()
        val dailyQuotaOrBillingError = listOf(
            "daily quota",
            "per day",
            "quota_exceeded",
            "spend cap",
            "prepay",
            "billing"
        ).any(normalized::contains)

        return !dailyQuotaOrBillingError
    }

    private fun isRetryableNetworkError(error: IOException): Boolean =
        error is SocketTimeoutException ||
            error is InterruptedIOException ||
            error is ConnectException ||
            error is UnknownHostException ||
            error is NoRouteToHostException

    private fun retryDelayMillis(attempt: Int, response: Response? = null): Long {
        val retryAfterSeconds = response?.header("Retry-After")?.trim()?.toLongOrNull()
        val serverDelay = retryAfterSeconds?.coerceIn(0L, MAX_RETRY_DELAY_MS / 1_000L)?.times(1_000L)
        val exponentialDelay = (BASE_RETRY_DELAY_MS * (1L shl attempt))
            .coerceAtMost(MAX_RETRY_DELAY_MS)
        val delay = (serverDelay ?: exponentialDelay).coerceIn(500L, MAX_RETRY_DELAY_MS)
        return delay + Random.nextLong(0L, 251L)
    }

    private fun parseActionFromJson(responseBody: String): AgentAction {
        val root = JSONObject(responseBody)
        val candidates = root.optJSONArray("candidates")
            ?: throw IllegalStateException("Gemini returned no candidates. Check whether the response was blocked.")

        if (candidates.length() == 0) {
            throw IllegalStateException("Gemini returned an empty candidates list.")
        }

        val firstCandidate = candidates.getJSONObject(0)
        val finishReason = firstCandidate.optString("finishReason", "")
        val content = firstCandidate.optJSONObject("content")
            ?: throw IllegalStateException("Gemini returned no response content (finish reason: $finishReason).")
        val parts = content.optJSONArray("parts")
            ?: throw IllegalStateException("Gemini returned no response parts (finish reason: $finishReason).")

        val rawText = (0 until parts.length())
            .asSequence()
            .mapNotNull { index -> parts.optJSONObject(index)?.optString("text") }
            .firstOrNull { it.isNotBlank() }
            ?: throw IllegalStateException("Gemini returned no JSON text (finish reason: $finishReason).")

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
