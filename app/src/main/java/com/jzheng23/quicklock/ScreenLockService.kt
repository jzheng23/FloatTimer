package com.jzheng23.quicklock

import android.accessibilityservice.AccessibilityService
import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.util.Log
import android.view.accessibility.AccessibilityEvent

class ScreenLockService : AccessibilityService() {

    private val lockScreenReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == "com.jzheng23.quicklock.LOCK_SCREEN") {
                Log.d("ScreenLockService", "Lock screen broadcast received")
                lockScreen()
            }
        }
    }

    @SuppressLint("InlinedApi")
    override fun onCreate() {
        super.onCreate()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(lockScreenReceiver, IntentFilter("com.jzheng23.quicklock.LOCK_SCREEN"),
                RECEIVER_NOT_EXPORTED)
        } else {
            registerReceiver(lockScreenReceiver, IntentFilter("com.jzheng23.quicklock.LOCK_SCREEN"),
                Context.RECEIVER_EXPORTED
            )
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(lockScreenReceiver)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // Not used in this example
    }

    override fun onInterrupt() {
        // Not used in this example
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == "LOCK_SCREEN") {
            lockScreen()
        }
        return super.onStartCommand(intent, flags, startId)
    }

    private fun lockScreen() {
        Log.d("ScreenLockService", "Attempting to lock screen")
        performGlobalAction(GLOBAL_ACTION_LOCK_SCREEN)
    }
}