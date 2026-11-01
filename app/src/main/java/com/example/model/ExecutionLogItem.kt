package com.example.model

/**
 * Log entry capturing a single step in the agent's reasoning and execution.
 */
data class ExecutionLogItem(
    val id: String,
    val stepNumber: Int,
    val timestamp: Long,
    val actionType: String,
    val thought: String,
    val target: String,
    val details: String,
    val isSuccess: Boolean
)
