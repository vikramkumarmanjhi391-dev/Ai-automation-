package com.example.model

/**
 * Structured action returned by the Gemini model to be executed
 * by the AccessibilityService.
 */
data class AgentAction(
    val action: String, // "tap", "tap_node", "swipe", "input_text", "press_back", "press_home", "wait", "finish"
    val thought: String = "",
    val targetDescription: String = "",
    val nodeId: Int? = null,
    val x: Float? = null,
    val y: Float? = null,
    val startX: Float? = null,
    val startY: Float? = null,
    val endX: Float? = null,
    val endY: Float? = null,
    val text: String? = null,
    val durationMs: Long? = null,
    val status: String? = null // For "finish" ("success", "failed")
)
