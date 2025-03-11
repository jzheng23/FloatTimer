package com.jzheng23.floattimer

import android.app.AppOpsManager
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Process
import android.provider.Settings
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val TAG = "ForegroundAppDetector"

@RequiresApi(Build.VERSION_CODES.Q)
fun checkUsageStatsPermission(context: Context): Boolean {
    val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
    val mode = appOps.unsafeCheckOpNoThrow(
        AppOpsManager.OPSTR_GET_USAGE_STATS,
        Process.myUid(),
        context.packageName
    )
    return mode == AppOpsManager.MODE_ALLOWED
}

fun requestUsageStatsPermission(context: Context) {
    val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
    context.startActivity(intent)
}

fun getForegroundPackageName(context: Context): String? {
    val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
    val time = System.currentTimeMillis()

    // Get usage stats for the last 10 seconds
    val stats = usageStatsManager.queryUsageStats(
        UsageStatsManager.INTERVAL_DAILY,
        time - 10000,
        time
    )

    if (stats.isNullOrEmpty()) {
        Log.e(TAG, "No usage stats found, permission might be missing")
        return null
    }

    // Find the app with the most recent time used
    var recentApp = stats[0]
    for (usageStats in stats) {
        if (usageStats.lastTimeUsed > recentApp.lastTimeUsed) {
            recentApp = usageStats
        }
    }

    val packageName = recentApp.packageName
    Log.d(TAG, "Current foreground app: $packageName")
    return packageName
}

@RequiresApi(Build.VERSION_CODES.Q)
@Composable
fun ForegroundAppMonitor(context: Context) {
    val hasPermission by remember { mutableStateOf(checkUsageStatsPermission(context)) }
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(hasPermission) {
        if (!hasPermission) {
            requestUsageStatsPermission(context)
        }
    }

    DisposableEffect(hasPermission) {
        val job = coroutineScope.launch {
            if (hasPermission) {
                while (isActive) {
                    withContext(Dispatchers.IO) {
                        getForegroundPackageName(context)
                    }
                    delay(2000) // Check every 2 seconds
                }
            }
        }

        onDispose {
            job.cancel()
        }
    }
}