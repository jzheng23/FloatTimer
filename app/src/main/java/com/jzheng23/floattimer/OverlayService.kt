package com.jzheng23.floattimer

import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.IBinder
import android.util.AttributeSet
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.TextView
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.core.content.ContextCompat
import com.jzheng23.floattimer.Constants.DEFAULT_BUTTON_SIZE
import kotlin.math.abs

class OverlayService : Service() {
    private lateinit var windowManager: WindowManager
    private var overlayView: View? = null
    private var timerTextView: TextView? = null

    private lateinit var params: WindowManager.LayoutParams

    // Declare these properties at the class level
    private var initialX: Int = 0
    private var initialY: Int = 0
    private var initialTouchX: Float = 0f
    private var initialTouchY: Float = 0f
    private var numberInBubble = 0
    private var buttonSize = DEFAULT_BUTTON_SIZE
    private var buttonAlpha = 1f
    private var buttonColor = Color.Gray
    private var rootView: FrameLayout? = null

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        buttonSize = intent?.getIntExtra("BUTTON_SIZE", DEFAULT_BUTTON_SIZE) ?: DEFAULT_BUTTON_SIZE
        buttonAlpha = intent?.getFloatExtra("BUTTON_ALPHA", 1f) ?: 1f
        val buttonColorInt =
            intent?.getIntExtra("BUTTON_COLOR", Color.Gray.toArgb()) ?: Color.Gray.toArgb()
        buttonColor = Color(buttonColorInt)

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
                setTextColor(buttonColor.toArgb())
            }

            updateButtonBorder()

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
            setTextColor(buttonColor.toArgb())
        }
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
        dragHandle?.setBackgroundResource(R.drawable.round_button_teal)
        dragHandle?.background?.alpha = (buttonAlpha * 255).toInt()

        updateButtonBorder()

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
                        numberInBubble++
                        timerTextView?.text = String.format(numberInBubble.toString())
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

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
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

    private fun updateButtonBorder() {
        val backgroundView = overlayView?.findViewById<View>(R.id.backgroundView) ?: return
        val timerText = overlayView?.findViewById<TextView>(R.id.timerText) ?: return

        // Determine which drawable to use
        val backgroundResId = when (buttonColor.toArgb()) {
            ContextCompat.getColor(this, R.color.gray) -> R.drawable.round_button_gray
            ContextCompat.getColor(this, R.color.teal) -> R.drawable.round_button_teal
            ContextCompat.getColor(this, R.color.orange) -> R.drawable.round_button_orange
            ContextCompat.getColor(this, R.color.black) -> R.drawable.round_button_black
            ContextCompat.getColor(this, R.color.white) -> R.drawable.round_button_white
            else -> R.drawable.round_button_gray
        }

        // Set the background resource on the background view
        backgroundView.setBackgroundResource(backgroundResId)

        // Apply alpha to the entire background view
        backgroundView.alpha = buttonAlpha

        // CRITICAL: Explicitly clear any background from the text view
        timerText.background = null

        // Update text color
        timerText.setTextColor(buttonColor.toArgb())

        // Make sure text is on top
        timerText.bringToFront()
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

