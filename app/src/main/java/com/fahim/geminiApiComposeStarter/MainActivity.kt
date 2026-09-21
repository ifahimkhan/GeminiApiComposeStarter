package com.fahim.geminiApiComposeStarter

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.fahim.geminiApiComposeStarter.ui.theme.GeminiApiComposeStarterTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            GeminiApiComposeStarterTheme {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically)
                ) {
                    Text(
                        text = "MAD Experiment 6",
                        style = MaterialTheme.typography.headlineMedium
                    )

                    NavButton("Camera", CameraActivity::class.java)
                    NavButton("Gallery", GalleryActivity::class.java)
                    NavButton("Glide / Coil", ImageLoaderActivity::class.java)
                    NavButton("Caching & Preloading", CacheDemoActivity::class.java)
                    NavButton("Gemini Chat", ChatActivity::class.java)
                }
            }
        }
    }
}

@Composable
private fun NavButton(label: String, target: Class<*>) {
    val context: Context = LocalContext.current
    Button(onClick = { context.startActivity(Intent(context, target)) }) {
        Text(label)
    }
}
