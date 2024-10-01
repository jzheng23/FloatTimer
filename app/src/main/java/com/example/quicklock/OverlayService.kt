package com.example.quicklock

import android.app.Service
import android.content.Intent
import android.graphics.PixelFormat
import android.os.IBinder
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.widget.Button

class OverlayService : Service() {
    private lateinit var windowManager: WindowManager
    private var overlayView: View? = null

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (overlayView == null) {
            showOverlay()
        }
        return START_STICKY
    }

    private fun showOverlay() {
        // Create the overlay view using LayoutInflater
        overlayView = LayoutInflater.from(this).inflate(R.layout.overlay_button, null)

        // Set up the lock button click listener
        overlayView?.findViewById<Button>(R.id.lockButton)?.setOnClickListener {
            lockScreen()
        }

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 100
            y = 100
        }

        try {
            windowManager.addView(overlayView, params)
        } catch (e: IllegalStateException) {
            // View is already added, log the error or handle it as needed
            e.printStackTrace()
        }
    }

    private fun lockScreen() {
        val intent = Intent("com.example.quicklock.LOCK_SCREEN")
        intent.setPackage(packageName)
        sendBroadcast(intent)
        Log.d("OverlayService", "Lock screen broadcast sent")
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
}