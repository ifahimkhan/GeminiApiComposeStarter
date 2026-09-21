package com.fahim.geminiApiComposeStarter

import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.*
import coil3.compose.AsyncImage
import com.fahim.geminiApiComposeStarter.ui.theme.GeminiApiComposeStarterTheme

class GalleryActivity : ComponentActivity() {

    private var selectedImageUri by mutableStateOf<Uri?>(null)

    private val galleryLauncher =
        registerForActivityResult(
            ActivityResultContracts.GetContent()
        ) { uri: Uri? ->

            selectedImageUri = uri
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            GeminiApiComposeStarterTheme {

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp)
                ) {

                    Text("Gallery")

                    Spacer(
                        modifier = Modifier.height(20.dp)
                    )

                    selectedImageUri?.let { uri ->

                        AsyncImage(
                            model = uri,
                            contentDescription = "Selected image",
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
                            galleryLauncher.launch("image/*")
                        }
                    ) {
                        Text("Choose Image")
                    }
                }
            }
        }
    }
}