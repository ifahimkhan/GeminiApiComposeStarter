package com.fahim.geminiApiComposeStarter

import android.graphics.Bitmap
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.unit.dp
import com.fahim.geminiApiComposeStarter.ui.theme.GeminiApiComposeStarterTheme

class CameraActivity : ComponentActivity() {

    private val cameraLauncher =
        registerForActivityResult(
            ActivityResultContracts.TakePicturePreview()
        ) { bitmap: Bitmap? ->

            bitmap?.let {
                capturedBitmap = it
            }
        }

    private var capturedBitmap by mutableStateOf<Bitmap?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            GeminiApiComposeStarterTheme {

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp)
                ) {

                    Text("Camera")

                    Spacer(
                        modifier = Modifier.height(20.dp)
                    )

                    capturedBitmap?.let {
                        Image(
                            bitmap = it.asImageBitmap(),
                            contentDescription = "Captured image",
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(300.dp)
                        )
                    }

                    Spacer(
                        modifier = Modifier.height(20.dp)
                    )

                    Button(
                        onClick = {
                            cameraLauncher.launch(null)
                        }
                    ) {
                        Text("Capture Image")
                    }
                }
            }
        }
    }
}