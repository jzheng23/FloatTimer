package com.jzheng23.floattimer

import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.jzheng23.floattimer.Constants.DEFAULT_BUTTON_SIZE
import com.jzheng23.floattimer.Constants.MAX_BUTTON_SIZE
import com.jzheng23.floattimer.Constants.MIN_BUTTON_SIZE

// First, move the function outside the composable
private fun hasUsageStatsPermission(context: Context): Boolean {
    return try {
        val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val time = System.currentTimeMillis()
        // Query for usage stats in the last minute
        val stats = usageStatsManager.queryUsageStats(
            UsageStatsManager.INTERVAL_DAILY,
            time - 60000, time
        )
        // If we can get stats, permission is granted
        stats != null && stats.isNotEmpty()
    } catch (e: SecurityException) {
        false
    }
}
@Composable
fun HomeScreen() {
    val context = LocalContext.current
    var overlayPermissionGranted by remember { mutableStateOf(Settings.canDrawOverlays(context)) }
    var usageStatsPermissionGranted by remember { mutableStateOf(hasUsageStatsPermission(context)) }
    var buttonSize by remember { mutableFloatStateOf(DEFAULT_BUTTON_SIZE.toFloat()) }
    var buttonAlpha by remember { mutableFloatStateOf(0.25f) }
//    var buttonColor by remember { mutableStateOf(Color.Transparent) }

    val overlayPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        overlayPermissionGranted = Settings.canDrawOverlays(context)
    }

    val usageStatsPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        usageStatsPermissionGranted = hasUsageStatsPermission(context)
    }

    fun startOverlayService() {
        if (overlayPermissionGranted) {
            val intent = Intent(context, OverlayService::class.java).apply {
                putExtra("BUTTON_SIZE", buttonSize.toInt())
                putExtra("BUTTON_ALPHA", buttonAlpha)
//                putExtra("BUTTON_COLOR", buttonColor.toArgb())
            }
            context.startService(intent)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Float Timer",
            style = MaterialTheme.typography.headlineMedium
        )

        Spacer(modifier = Modifier.height(32.dp))

        PermissionSwitch(
            text = "Overlay Permission",
            checked = overlayPermissionGranted,
            onCheckedChange = { checked ->
                if (checked) {
                    val intent = Intent(
                        Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:${context.packageName}")
                    )
                    overlayPermissionLauncher.launch(intent)
                }
            }
        )

        // Add this to your UI
        PermissionSwitch(
            text = "Usage Stats Permission",
            checked = usageStatsPermissionGranted,
            onCheckedChange = { checked ->
                if (checked) {
                    val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
                    usageStatsPermissionLauncher.launch(intent)
                }
            }
        )

        Spacer(modifier = Modifier.height(32.dp))

//        ColorPicker(
//            selectedColor = buttonColor,
//            onColorSelected = { color ->
//                buttonColor = color
//                startOverlayService()
//            }
//        )

        Text(
            "Button size: ${buttonSize.toInt()}dp",
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.padding(top = 16.dp)
        )

        Slider(
            value = buttonSize,
            onValueChange = {
                buttonSize = it
                startOverlayService()
            },
            valueRange = MIN_BUTTON_SIZE.toFloat()..MAX_BUTTON_SIZE.toFloat(),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        )

        // Transparency slider
        Text(
            "Button transparency: ${((1 - buttonAlpha) * 100).toInt()}%",
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.padding(top = 16.dp)
        )
        Slider(
            value = 1 - buttonAlpha,
            onValueChange = {
                buttonAlpha = 1 - it
                startOverlayService() // Update when slider changes
            },
            valueRange = 0f..1f, // From 10% visible to 100% visible
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        )

        OverlayButtonPreview(
            buttonSize = buttonSize.toInt(),
            buttonAlpha = buttonAlpha,
//            buttonColor = buttonColor
        )

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = { startOverlayService() },
            enabled = overlayPermissionGranted && usageStatsPermissionGranted
        ) {
            Text("Start the floating button")
        }
    }
}

@Composable
fun PermissionSwitch(
    text: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text)
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}

@Composable
fun OverlayButtonPreview(
    buttonSize: Int = DEFAULT_BUTTON_SIZE,
    buttonAlpha: Float = 1f,
//    buttonColor: Color = Color.Transparent
) {
    // Inside your composable or where you have context available
    val context = LocalContext.current
    val textColor = Color(ContextCompat.getColor(context, R.color.gray))
    val backgroundColor = Color(ContextCompat.getColor(context, R.color.background_color))

//        Color(ContextCompat.getColor(context, R.color.background_color))
    val borderColor = Color(ContextCompat.getColor(context, R.color.gray))
    Column(
        modifier = Modifier.padding(vertical = 16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center
        ) {
            Column {
                Text("Light background:", style = MaterialTheme.typography.labelMedium)
                Box(
                    modifier = Modifier
                        .size(150.dp)
                        .background(Color.White)
                ) {
                    Box(
                        modifier = Modifier
                            .size(buttonSize.dp)
                            .align(Alignment.Center)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    color = when {
                                        backgroundColor == Color.White || backgroundColor == Color.Black ->
                                            backgroundColor.copy(alpha = buttonAlpha)

                                        else ->
                                            backgroundColor.copy(alpha = 0.2f * buttonAlpha)
                                    },
                                    shape = CircleShape
                                )
                                .border(
                                    width = 1.dp,
                                    color = borderColor.copy(alpha = 0.4f * buttonAlpha),
                                    shape = CircleShape
                                )
                        )
                        Text(
                            "99",
                            modifier = Modifier.align(Alignment.Center),
                            color = textColor.copy(alpha = buttonAlpha),
                            fontSize = Constants.calculateTextSize(buttonSize).sp
                        )
                    }
                }
            }

            Column {
                Text("Dark background:", style = MaterialTheme.typography.labelMedium)
                Box(
                    modifier = Modifier
                        .size(150.dp)
                        .background(Color.Black)
                ) {
                    Box(
                        modifier = Modifier
                            .size(buttonSize.dp)
                            .align(Alignment.Center)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    color = when {
                                        backgroundColor == Color.White || backgroundColor == Color.Black ->
                                            backgroundColor.copy(alpha = buttonAlpha)

                                        else ->
                                            backgroundColor.copy(alpha = 0.2f * buttonAlpha)
                                    },
                                    shape = CircleShape
                                )
                                .border(
                                    width = 1.dp,
                                    color = borderColor.copy(alpha = 0.4f * buttonAlpha),
                                    shape = CircleShape
                                )
                        )
                        Text(
                            "99",
                            modifier = Modifier.align(Alignment.Center),
                            color = textColor.copy(alpha = buttonAlpha),
                            fontSize = Constants.calculateTextSize(buttonSize).sp
                        )
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun HomeScreenPreview() {
    HomeScreen()
}
