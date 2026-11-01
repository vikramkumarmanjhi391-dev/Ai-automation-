package com.example.data

import android.content.Context
import android.content.SharedPreferences

/**
 * Manages persistent configuration settings including Gemini API Key,
 * selected model, execution delay, and floating overlay preferences.
 */
class PreferencesManager(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    var apiKey: String
        get() = prefs.getString(KEY_API_KEY, "").orEmpty()
        set(value) = prefs.edit().putString(KEY_API_KEY, value.trim()).apply()

    var selectedModel: String
        get() {
            val stored = prefs.getString(KEY_MODEL, DEFAULT_MODEL).orEmpty()
            return if (stored in AVAILABLE_MODELS) {
                stored
            } else {
                // Automatically migrate deprecated model names to gemini-3.8-flash
                DEFAULT_MODEL.also { selectedModel = it }
            }
        }
        set(value) = prefs.edit().putString(KEY_MODEL, value).apply()

    var stepDelaySeconds: Float
        get() = prefs.getFloat(KEY_STEP_DELAY, 2.0f)
        set(value) = prefs.edit().putFloat(KEY_STEP_DELAY, value).apply()

    var maxSteps: Int
        get() = prefs.getInt(KEY_MAX_STEPS, 12)
        set(value) = prefs.edit().putInt(KEY_MAX_STEPS, value).apply()

    var isFloatingOverlayEnabled: Boolean
        get() = prefs.getBoolean(KEY_FLOATING_OVERLAY, false)
        set(value) = prefs.edit().putBoolean(KEY_FLOATING_OVERLAY, value).apply()

    companion object {
        private const val PREFS_NAME = "ai_agent_preferences"
        private const val KEY_API_KEY = "gemini_api_key"
        private const val KEY_MODEL = "selected_model"
        private const val KEY_STEP_DELAY = "step_delay_seconds"
        private const val KEY_MAX_STEPS = "max_steps"
        private const val KEY_FLOATING_OVERLAY = "floating_overlay_enabled"

        const val DEFAULT_MODEL = "gemini-3.8-flash"
        val AVAILABLE_MODELS = listOf(
            "gemini-3.8-flash",
            "gemini-3.5-flash-lite",
            "gemini-4-argon"
        )
    }
}
