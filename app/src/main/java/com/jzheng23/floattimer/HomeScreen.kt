package com.jzheng23.floattimer


import android.app.Activity
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
import com.jzheng23.floattimer.Constants.DEFAULT_BUTTON_SIZE
import com.jzheng23.floattimer.Constants.MAX_BUTTON_SIZE
import com.jzheng23.floattimer.Constants.MIN_BUTTON_SIZE


@Composable
fun HomeScreen() {
    val context = LocalContext.current
    var overlayPermissionGranted by remember { mutableStateOf(Settings.canDrawOverlays(context)) }
    var buttonSize by remember { mutableFloatStateOf(DEFAULT_BUTTON_SIZE.toFloat()) }


    val overlayPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        overlayPermissionGranted = Settings.canDrawOverlays(context)
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

        Spacer(modifier = Modifier.height(32.dp))

        Text(
            "Button size: ${buttonSize.toInt()}dp",
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.padding(top = 16.dp)
        )

        Slider(
            value = buttonSize,
            onValueChange = { buttonSize = it },
            valueRange = MIN_BUTTON_SIZE.toFloat()..MAX_BUTTON_SIZE.toFloat(),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        )

        OverlayButtonPreview(buttonSize = buttonSize.toInt())

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = {
                if (overlayPermissionGranted) {
                    val intent = Intent(context, OverlayService::class.java).apply {
                        putExtra("BUTTON_SIZE", buttonSize.toInt())
                    }
                    context.startService(intent)
                    // You might want to finish the activity here if needed
//                    val activity = context as? Activity
//                    activity?.moveTaskToBack(true)
                }
            },
            enabled = overlayPermissionGranted
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
fun OverlayButtonPreview(buttonSize: Int = Constants.DEFAULT_BUTTON_SIZE) {
    Column(
        modifier = Modifier.padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text("Light background:", style = MaterialTheme.typography.labelMedium)
                Box(
                    modifier = Modifier
                        .size(120.dp)
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
                                    color = Color(0x33888888),
                                    shape = CircleShape
                                )
                                .border(
                                    width = 1.dp,
                                    color = Color(0x44888888),
                                    shape = CircleShape
                                )
                        )
                        Text(
                            "99",
                            modifier = Modifier.align(Alignment.Center),
                            color = Color(0x88888888),
                            fontSize = Constants.calculateTextSize(buttonSize).sp
                        )
                    }
                }
            }

            Column {
                Text("Dark background:", style = MaterialTheme.typography.labelMedium)
                Box(
                    modifier = Modifier
                        .size(120.dp)
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
                                    color = Color(0x33888888),
                                    shape = CircleShape
                                )
                                .border(
                                    width = 1.dp,
                                    color = Color(0x44888888),
                                    shape = CircleShape
                                )
                        )
                        Text(
                            "99",
                            modifier = Modifier.align(Alignment.Center),
                            color = Color(0x88888888),
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
