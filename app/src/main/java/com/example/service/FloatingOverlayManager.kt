package com.example.service

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.TypedValue
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import com.example.MainActivity

/**
 * Manages an on-screen floating overlay widget using TYPE_ACCESSIBILITY_OVERLAY.
 * Allows users to control the AI agent and monitor actions outside the app.
 */
class FloatingOverlayManager(private val context: Context) {
    private val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private var overlayView: View? = null
    private var isExpanded = false
    private val mainHandler = Handler(Looper.getMainLooper())

    @SuppressLint("ClickableViewAccessibility")
    fun show() {
        if (overlayView != null) return

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 40
            y = 200
        }

        val rootLayout = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dpToPx(8), dpToPx(8), dpToPx(8), dpToPx(8))
        }

        // Header / Draggable Pill
        val pillLayout = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dpToPx(12), dpToPx(8), dpToPx(12), dpToPx(8))
            background = GradientDrawable().apply {
                setColor(Color.parseColor("#0F172A"))
                cornerRadius = dpToPx(24).toFloat()
                setStroke(dpToPx(2), Color.parseColor("#06B6D4"))
            }
        }

        val iconText = TextView(context).apply {
            text = "🤖"
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 18f)
        }

        val statusText = TextView(context).apply {
            text = " AI Agent"
            setTextColor(Color.WHITE)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
            setPadding(dpToPx(4), 0, dpToPx(4), 0)
        }

        val toggleExpandBtn = TextView(context).apply {
            text = " ▼"
            setTextColor(Color.parseColor("#06B6D4"))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
        }

        pillLayout.addView(iconText)
        pillLayout.addView(statusText)
        pillLayout.addView(toggleExpandBtn)

        // Expandable control card
        val cardLayout = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            visibility = View.GONE
            setPadding(dpToPx(12), dpToPx(12), dpToPx(12), dpToPx(12))
            val cardBg = GradientDrawable().apply {
                setColor(Color.parseColor("#1E293B"))
                cornerRadius = dpToPx(16).toFloat()
                setStroke(dpToPx(1), Color.parseColor("#334155"))
            }
            background = cardBg
            val cardParams = LinearLayout.LayoutParams(
                dpToPx(220),
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = dpToPx(6)
            }
            layoutParams = cardParams
        }

        val liveActionText = TextView(context).apply {
            text = "Idle"
            setTextColor(Color.parseColor("#94A3B8"))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
            maxLines = 3
        }

        val actionButtonsRow = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, dpToPx(8), 0, 0)
        }

        val stopButton = Button(context).apply {
            text = "Stop"
            setTextColor(Color.WHITE)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f)
            background = GradientDrawable().apply {
                setColor(Color.parseColor("#DC2626"))
                cornerRadius = dpToPx(8).toFloat()
            }
            setOnClickListener {
                AutomationServiceManager.setAgentRunning(false, "Stopped from overlay.")
            }
            layoutParams = LinearLayout.LayoutParams(0, dpToPx(36), 1f).apply {
                marginEnd = dpToPx(4)
            }
        }

        val openAppButton = Button(context).apply {
            text = "Open App"
            setTextColor(Color.WHITE)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f)
            background = GradientDrawable().apply {
                setColor(Color.parseColor("#0284C7"))
                cornerRadius = dpToPx(8).toFloat()
            }
            setOnClickListener {
                val intent = Intent(context, MainActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                }
                context.startActivity(intent)
            }
            layoutParams = LinearLayout.LayoutParams(0, dpToPx(36), 1f)
        }

        actionButtonsRow.addView(stopButton)
        actionButtonsRow.addView(openAppButton)

        cardLayout.addView(liveActionText)
        cardLayout.addView(actionButtonsRow)

        rootLayout.addView(pillLayout)
        rootLayout.addView(cardLayout)

        // Touch & Drag handler
        var initialX = 0
        var initialY = 0
        var initialTouchX = 0f
        var initialTouchY = 0f
        var isClick = false

        pillLayout.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialX = params.x
                    initialY = params.y
                    initialTouchX = event.rawX
                    initialTouchY = event.rawY
                    isClick = true
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = (event.rawX - initialTouchX).toInt()
                    val dy = (event.rawY - initialTouchY).toInt()
                    if (Math.abs(dx) > 10 || Math.abs(dy) > 10) {
                        isClick = false
                    }
                    params.x = initialX + dx
                    params.y = initialY + dy
                    windowManager.updateViewLayout(rootLayout, params)
                    true
                }
                MotionEvent.ACTION_UP -> {
                    if (isClick) {
                        isExpanded = !isExpanded
                        cardLayout.visibility = if (isExpanded) View.VISIBLE else View.GONE
                        toggleExpandBtn.text = if (isExpanded) " ▲" else " ▼"
                    }
                    true
                }
                else -> false
            }
        }

        try {
            windowManager.addView(rootLayout, params)
            overlayView = rootLayout
            AutomationServiceManager.setOverlayActive(true)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun hide() {
        overlayView?.let { view ->
            try {
                windowManager.removeView(view)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        overlayView = null
        AutomationServiceManager.setOverlayActive(false)
    }

    private fun dpToPx(dp: Int): Int {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            dp.toFloat(),
            context.resources.displayMetrics
        ).toInt()
    }
}
