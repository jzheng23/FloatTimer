package com.jzheng23.floattimer

import android.app.Service
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.Process
import android.util.AttributeSet
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.TextView
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.jzheng23.floattimer.Constants.DEFAULT_BUTTON_SIZE
import java.util.Locale
import java.util.SortedMap
import java.util.Timer
import java.util.TimerTask
import java.util.TreeMap
import kotlin.math.abs

class OverlayService : Service() {
    private lateinit var windowManager: WindowManager
    private lateinit var usageStatsManager: UsageStatsManager
    private var lastAlphaUpdateTime = 0L

    private var overlayView: View? = null
    private var timerTextView: TextView? = null

    private lateinit var params: WindowManager.LayoutParams

    // Declare these properties at the class level
    private var initialX: Int = 0
    private var initialY: Int = 0
    private var initialTouchX: Float = 0f
    private var initialTouchY: Float = 0f
//    private var numberInBubble = 0
    private var buttonSize = DEFAULT_BUTTON_SIZE
    private var buttonAlpha = 0.25f
    private var rootView: FrameLayout? = null
    private var timer: Timer? = null
    private var counterValue = 0

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        usageStatsManager = getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        buttonSize = intent?.getIntExtra("BUTTON_SIZE", DEFAULT_BUTTON_SIZE) ?: DEFAULT_BUTTON_SIZE
        buttonAlpha = intent?.getFloatExtra("BUTTON_ALPHA", 1f) ?: 1f


        if (overlayView == null) {
            showOverlay()
        } else {
            val sizeInPixels = Constants.dpToPx(this, buttonSize)
            params.width = sizeInPixels
            params.height = sizeInPixels
            // Update root view size
            rootView?.layoutParams = FrameLayout.LayoutParams(sizeInPixels, sizeInPixels)

            overlayView?.findViewById<DraggableFrameLayout>(R.id.dragHandle)?.apply {
                layoutParams = FrameLayout.LayoutParams(sizeInPixels, sizeInPixels)
                alpha = buttonAlpha // Add this line to update alpha
            }

            // Update text size and alpha
            timerTextView?.apply {
                textSize = Constants.calculateTextSize(buttonSize)
                alpha = buttonAlpha // Add this line to update alpha
                setTextColor(getColor(R.color.gray))
            }

//            updateButtonBorder()

            // Update in window manager
            rootView?.let { view ->
                windowManager.updateViewLayout(view, params)
            }
        }
        return START_STICKY
    }

    private fun showOverlay() {
        val sizeInPixels = Constants.dpToPx(this, buttonSize)

        val newRootView = FrameLayout(this).apply {
            layoutParams = FrameLayout.LayoutParams(sizeInPixels, sizeInPixels)
        }

        overlayView = LayoutInflater.from(this).inflate(R.layout.overlay_button, newRootView, true)

        val dragHandle = overlayView?.findViewById<DraggableFrameLayout>(R.id.dragHandle)?.apply {
            layoutParams = FrameLayout.LayoutParams(sizeInPixels, sizeInPixels)
            alpha = buttonAlpha
        }

        timerTextView = overlayView?.findViewById<TextView>(R.id.timerText)?.apply {
            textSize = Constants.calculateTextSize(buttonSize)
            alpha = buttonAlpha
            setTextColor(getColor(R.color.gray))
        }

        // Add this after setting up timerTextView
        timerTextView?.text = "0" // Start with 0
        startCounter()

        params = WindowManager.LayoutParams(
            sizeInPixels,
            sizeInPixels,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 100
            y = 100
        }

        rootView = newRootView
        overlayView?.setBackgroundColor(Color.Transparent.toArgb())
        dragHandle?.setBackgroundResource(R.drawable.round_button_gray)
        dragHandle?.background?.alpha = (buttonAlpha * 255).toInt()

//        updateButtonBorder()

        var isMoved = false

        dragHandle?.setOnTouchListener { view, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialX = params.x
                    initialY = params.y
                    initialTouchX = event.rawX
                    initialTouchY = event.rawY
                    isMoved = false
                    true
                }

                MotionEvent.ACTION_MOVE -> {
                    val deltaX = (event.rawX - initialTouchX).toInt()
                    val deltaY = (event.rawY - initialTouchY).toInt()
                    if (abs(deltaX) > 5 || abs(deltaY) > 5) {
                        isMoved = true
                        params.x = initialX + deltaX
                        params.y = initialY + deltaY
                        windowManager.updateViewLayout(rootView, params)
                    }
                    true
                }

                MotionEvent.ACTION_UP -> {
                    if (!isMoved) {
                        view.performClick()
                        makeFullyOpaqueTemporarily()
//                        numberInBubble++
//                        timerTextView?.text = String.format(numberInBubble.toString())
                    }
                    true
                }

                else -> false
            }
        }

        try {
            windowManager.addView(rootView, params)
        } catch (e: IllegalStateException) {
            e.printStackTrace()
        }
    }

    private fun makeFullyOpaqueTemporarily() {
        // Save the current alpha value
        val originalAlpha = buttonAlpha

        // Make button fully opaque
        buttonAlpha = 1f

        // Get the background view and change its background resource
        val backgroundView = overlayView?.findViewById<View>(R.id.backgroundView)
        // Save the original background drawable
//        val originalBackground = backgroundView?.background
        // Change to black background
        backgroundView?.setBackgroundResource(R.drawable.round_button_black)

        // Update the UI to show the alpha change
        overlayView?.findViewById<DraggableFrameLayout>(R.id.dragHandle)?.apply {
            alpha = 1f
        }

        timerTextView?.apply {
            alpha = 1f
        }

        // Schedule a return to original transparency and background after 2 seconds
        Handler(Looper.getMainLooper()).postDelayed({
            // Restore original alpha
            buttonAlpha = originalAlpha

            overlayView?.findViewById<DraggableFrameLayout>(R.id.dragHandle)?.apply {
                alpha = originalAlpha
            }

            timerTextView?.apply {
                alpha = originalAlpha
            }

            // Restore original background
            backgroundView?.setBackgroundResource(R.drawable.round_button_gray)
        }, 2000) // 2000 milliseconds = 2 seconds
    }

    private fun startCounter() {
        // Cancel any existing timer
        timer?.cancel()

        // Reset counter to 0
        counterValue = 0
        timerTextView?.text = String.format(Locale.getDefault(), "%d", counterValue)
        // Create new timer that schedules the next task after each completion
        timer = Timer()
        scheduleNextTimer()
    }


    // Add this method to check for permission
    private fun hasUsageStatsPermission(): Boolean {
        val appOps = getSystemService(Context.APP_OPS_SERVICE) as android.app.AppOpsManager
        val mode = appOps.checkOpNoThrow(
            android.app.AppOpsManager.OPSTR_GET_USAGE_STATS,
            Process.myUid(), packageName
        )
        return mode == android.app.AppOpsManager.MODE_ALLOWED
    }

    // Get the current foreground app package name
    private var lastDetectedApp: String? = null
    private var lastChangeTime: Long = 0

    private fun getForegroundApp(): String? {
        if (!hasUsageStatsPermission()) return null

        val time = System.currentTimeMillis()
        val stats = usageStatsManager.queryUsageStats(
            UsageStatsManager.INTERVAL_BEST, time - 60000, time
        )

        if (stats.isEmpty()) return null

        val launcherPackages = listOf(
            "com.google.android.apps.nexuslauncher",
        )

        val currentApp = stats
            .filter { it.packageName !in launcherPackages }
            .maxByOrNull { it.lastTimeUsed }
            ?.packageName

        // If detected app is different from last one and not a launcher
        if (currentApp != null && currentApp != lastDetectedApp && currentApp !in launcherPackages) {
            lastDetectedApp = currentApp
            lastChangeTime = time
        }

        return lastDetectedApp
    }

    private fun scheduleNextTimer() {
        timer?.schedule(object : TimerTask() {
            override fun run() {
                // Increment counter
                counterValue++

                // Calculate new alpha based on time
                // This will gradually increase opacity (decrease transparency)
                // You can adjust the formula to control the rate of change
                val currentApp = getForegroundApp()
                Log.d("ForegroundApp", "Current app: $currentApp")

                // Calculate new alpha based on which app is in foreground
                val newAlpha = when (currentApp) {
                    "com.google.android.gm" -> { // Gmail
                        // Gradually increase transparency (decrease alpha)
                        maxOf(0.1f, buttonAlpha - 0.2f)
                    }
                    "com.twitter.android", "com.twitter.android.lite", "com.x.android" -> { // X (Twitter)
                        // Gradually decrease transparency (increase alpha)
                        minOf(1f, buttonAlpha + 0.2f)
                    }
                    else -> {
                        // Keep current transparency for other apps
                        buttonAlpha
                    }
                }

                // Update UI on main thread
                Handler(Looper.getMainLooper()).post {
                    timerTextView?.text = String.format(Locale.getDefault(),"%d", counterValue)

                    // Apply the new alpha to the views
                    overlayView?.findViewById<DraggableFrameLayout>(R.id.dragHandle)?.apply {
                        alpha = newAlpha
                    }

                    timerTextView?.apply {
                        alpha = newAlpha
                    }

                    // Update the buttonAlpha property to keep track of current alpha
                    buttonAlpha = newAlpha

                    // Schedule next update after this one completes
                    scheduleNextTimer()
                }
            }
        }, 2000) // Run after 2 seconds
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        timer?.cancel()
        timer = null
        super.onDestroy()
        removeOverlay()
    }

    private fun removeOverlay() {
        overlayView?.let {
            try {
                windowManager.removeView(it)
            } catch (e: IllegalArgumentException) {
                // View is not attached, log the error or handle it as needed
                e.printStackTrace()
            }
            overlayView = null
        }
    }

}

class DraggableFrameLayout : FrameLayout {
    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    )

    override fun performClick(): Boolean {
        super.performClick()
        return true
    }
}

