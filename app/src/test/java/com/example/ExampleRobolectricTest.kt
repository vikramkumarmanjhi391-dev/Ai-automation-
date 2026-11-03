package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.PreferencesManager
import com.example.model.AgentAction
import com.example.model.UiNodeInfo
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @Test
    fun readStringFromContext() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("AI Auto Agent", appName)
    }

    @Test
    fun defaultModelIsGemini38Flash() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val prefs = PreferencesManager(context)
        assertEquals("gemini-3.8-flash", PreferencesManager.DEFAULT_MODEL)
        assertTrue(PreferencesManager.AVAILABLE_MODELS.contains("gemini-3.8-flash"))
    }

    @Test
    fun deprecatedModelAutoMigration() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val rawPrefs = context.getSharedPreferences("ai_agent_preferences", Context.MODE_PRIVATE)
        rawPrefs.edit().putString("selected_model", "gemini-2.5-flash").commit()

        val prefs = PreferencesManager(context)
        assertEquals("gemini-3.8-flash", prefs.selectedModel)
    }

    @Test
    fun uiNodePromptSummaryFormatting() {
        val node = UiNodeInfo(
            id = 5,
            className = "android.widget.Button",
            text = "Settings",
            contentDescription = "System Settings",
            viewId = "com.android.settings:id/settings_button",
            left = 100,
            top = 200,
            right = 300,
            bottom = 260,
            centerX = 200,
            centerY = 230,
            width = 200,
            height = 60,
            isClickable = true,
            isEditable = false,
            isScrollable = false,
            isCheckable = false,
            isChecked = false,
            isEnabled = true,
            depth = 2
        )

        val summary = node.toPromptSummary()
        assertTrue(summary.contains("Node #5"))
        assertTrue(summary.contains("\"Settings\""))
        assertTrue(summary.contains("center=(200, 230)"))
        assertTrue(summary.contains("clickable"))
    }


    @Test
    fun editableFieldValueIsNotIncludedInGeminiPrompt() {
        val node = UiNodeInfo(
            id = 9,
            className = "android.widget.EditText",
            text = "private-user-entered-value",
            contentDescription = "Search",
            viewId = "com.example:id/search_field",
            left = 0,
            top = 0,
            right = 400,
            bottom = 80,
            centerX = 200,
            centerY = 40,
            width = 400,
            height = 80,
            isClickable = true,
            isEditable = true,
            isScrollable = false,
            isCheckable = false,
            isChecked = false,
            isEnabled = true,
            depth = 1
        )

        val summary = node.toPromptSummary()
        assertFalse(summary.contains("private-user-entered-value"))
        assertTrue(summary.contains("editable field"))
        assertTrue(summary.contains("Search"))
    }

    @Test
    fun agentActionCreation() {
        val action = AgentAction(
            action = "tap",
            thought = "Clicking the settings button to access Wi-Fi",
            targetDescription = "Settings",
            x = 200f,
            y = 230f
        )
        assertEquals("tap", action.action)
        assertEquals(200f, action.x)
        assertEquals(230f, action.y)
    }
}
