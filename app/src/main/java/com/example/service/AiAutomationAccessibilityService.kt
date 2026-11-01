package com.example.service

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.graphics.Path
import android.graphics.Rect
import android.os.Bundle
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.example.model.AgentAction
import com.example.model.UiNodeInfo
import kotlinx.coroutines.delay
import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.concurrent.ConcurrentHashMap
import kotlin.coroutines.resume

/**
 * Custom Accessibility Service that:
 * 1. Reads UI nodes and on-screen text from active windows without root.
 * 2. Simulates physical taps, swipes, and text inputs via dispatchGesture and node actions.
 * 3. Supports global actions (Back, Home, Recents).
 */
class AiAutomationAccessibilityService : AccessibilityService() {

    private val nodeCache = ConcurrentHashMap<Int, AccessibilityNodeInfo>()
    private var overlayManager: FloatingOverlayManager? = null

    companion object {
        private const val TAG = "AiAccessibilityService"
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        Log.i(TAG, "Accessibility Service Connected")
        overlayManager = FloatingOverlayManager(this)
        AutomationServiceManager.registerService(this)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // Accessibility events are tracked as needed
    }

    override fun onInterrupt() {
        Log.w(TAG, "Accessibility Service Interrupted")
    }

    override fun onDestroy() {
        super.onDestroy()
        overlayManager?.hide()
        overlayManager = null
        AutomationServiceManager.unregisterService()
        clearNodeCache()
        Log.i(TAG, "Accessibility Service Destroyed")
    }

    fun showOverlay() {
        overlayManager?.show()
    }

    fun hideOverlay() {
        overlayManager?.hide()
    }

    /**
     * Recursively traverses root window to harvest interactive and text-bearing UI nodes.
     */
    fun captureScreenNodes(): List<UiNodeInfo> {
        clearNodeCache()
        val root = rootInActiveWindow ?: return emptyList()
        val result = mutableListOf<UiNodeInfo>()
        var counter = 1

        fun traverse(node: AccessibilityNodeInfo?, depth: Int) {
            if (node == null || depth > 20 || result.size >= 150) return

            val rect = Rect()
            node.getBoundsInScreen(rect)

            val text = node.text?.toString()?.trim()
            val contentDesc = node.contentDescription?.toString()?.trim()
            val viewId = node.viewIdResourceName
            val className = node.className?.toString() ?: ""

            val hasMeaningfulContent = !text.isNullOrEmpty() ||
                    !contentDesc.isNullOrEmpty() ||
                    node.isClickable ||
                    node.isEditable ||
                    node.isScrollable

            val isVisible = rect.width() > 0 && rect.height() > 0 && rect.bottom > 0

            if (hasMeaningfulContent && isVisible) {
                val nodeId = counter++
                nodeCache[nodeId] = node

                result.add(
                    UiNodeInfo(
                        id = nodeId,
                        className = className,
                        text = text,
                        contentDescription = contentDesc,
                        viewId = viewId,
                        left = rect.left,
                        top = rect.top,
                        right = rect.right,
                        bottom = rect.bottom,
                        centerX = rect.centerX(),
                        centerY = rect.centerY(),
                        width = rect.width(),
                        height = rect.height(),
                        isClickable = node.isClickable,
                        isEditable = node.isEditable,
                        isScrollable = node.isScrollable,
                        isCheckable = node.isCheckable,
                        isChecked = node.isChecked,
                        isEnabled = node.isEnabled,
                        depth = depth
                    )
                )
            }

            for (i in 0 until node.childCount) {
                traverse(node.getChild(i), depth + 1)
            }
        }

        try {
            traverse(root, 0)
        } catch (e: Exception) {
            Log.e(TAG, "Error while inspecting UI nodes", e)
        }

        AutomationServiceManager.setCapturedNodes(result)
        return result
    }

    /**
     * Executes the AgentAction received from Gemini.
     */
    suspend fun executeAgentAction(action: AgentAction): Boolean {
        return when (action.action.lowercase()) {
            "tap" -> {
                val x = action.x
                val y = action.y
                if (x != null && y != null) {
                    dispatchTap(x, y)
                } else if (action.nodeId != null) {
                    tapNodeById(action.nodeId)
                } else {
                    false
                }
            }
            "tap_node" -> {
                val nodeId = action.nodeId ?: return false
                tapNodeById(nodeId)
            }
            "swipe" -> {
                val startX = action.startX ?: 500f
                val startY = action.startY ?: 1200f
                val endX = action.endX ?: 500f
                val endY = action.endY ?: 400f
                val duration = action.durationMs ?: 400L
                dispatchSwipe(startX, startY, endX, endY, duration)
            }
            "input_text" -> {
                val text = action.text ?: ""
                val nodeId = action.nodeId
                if (nodeId != null) {
                    inputTextIntoNode(nodeId, text)
                } else {
                    false
                }
            }
            "press_back" -> {
                performGlobalAction(GLOBAL_ACTION_BACK)
            }
            "press_home" -> {
                performGlobalAction(GLOBAL_ACTION_HOME)
            }
            "wait" -> {
                val waitTime = (action.durationMs ?: 1500L).coerceIn(500L, 5000L)
                delay(waitTime)
                true
            }
            "finish" -> {
                true
            }
            else -> {
                Log.w(TAG, "Unknown action: ${action.action}")
                false
            }
        }
    }

    /**
     * Simulates a physical tap at coordinates (x, y) without root.
     */
    suspend fun dispatchTap(x: Float, y: Float): Boolean = suspendCancellableCoroutine { cont ->
        val path = Path().apply { moveTo(x, y) }
        val stroke = GestureDescription.StrokeDescription(path, 0, 100)
        val gesture = GestureDescription.Builder().addStroke(stroke).build()

        val callback = object : GestureResultCallback() {
            override fun onCompleted(gestureDescription: GestureDescription?) {
                if (cont.isActive) cont.resume(true)
            }

            override fun onCancelled(gestureDescription: GestureDescription?) {
                if (cont.isActive) cont.resume(false)
            }
        }

        val dispatched = dispatchGesture(gesture, callback, null)
        if (!dispatched && cont.isActive) {
            cont.resume(false)
        }
    }

    /**
     * Simulates a swipe/drag gesture without root.
     */
    suspend fun dispatchSwipe(
        startX: Float,
        startY: Float,
        endX: Float,
        endY: Float,
        durationMs: Long
    ): Boolean = suspendCancellableCoroutine { cont ->
        val path = Path().apply {
            moveTo(startX, startY)
            lineTo(endX, endY)
        }
        val safeDuration = durationMs.coerceIn(100L, 1500L)
        val stroke = GestureDescription.StrokeDescription(path, 0, safeDuration)
        val gesture = GestureDescription.Builder().addStroke(stroke).build()

        val callback = object : GestureResultCallback() {
            override fun onCompleted(gestureDescription: GestureDescription?) {
                if (cont.isActive) cont.resume(true)
            }

            override fun onCancelled(gestureDescription: GestureDescription?) {
                if (cont.isActive) cont.resume(false)
            }
        }

        val dispatched = dispatchGesture(gesture, callback, null)
        if (!dispatched && cont.isActive) {
            cont.resume(false)
        }
    }

    private suspend fun tapNodeById(nodeId: Int): Boolean {
        val node = nodeCache[nodeId]
        if (node != null) {
            val rect = Rect()
            node.getBoundsInScreen(rect)
            if (rect.width() > 0 && rect.height() > 0) {
                // Clicking at the physical screen coordinate center is most reliable
                return dispatchTap(rect.centerX().toFloat(), rect.centerY().toFloat())
            }
            // Fallback to accessibility action click
            return node.performAction(AccessibilityNodeInfo.ACTION_CLICK)
        }
        return false
    }

    private suspend fun inputTextIntoNode(nodeId: Int, text: String): Boolean {
        val node = nodeCache[nodeId] ?: return false
        val rect = Rect()
        node.getBoundsInScreen(rect)

        // First tap the node to focus the input field
        dispatchTap(rect.centerX().toFloat(), rect.centerY().toFloat())
        delay(300)

        val arguments = Bundle().apply {
            putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, text)
        }
        return node.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, arguments)
    }

    private fun clearNodeCache() {
        nodeCache.clear()
    }
}
