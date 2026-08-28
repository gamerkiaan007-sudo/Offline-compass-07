package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.example.model.CompassState
import com.example.ui.CompassScreen
import com.example.ui.CompassViewModel
import com.example.ui.components.CompassDial
import com.example.ui.components.CompassHeader
import com.example.ui.theme.OfflineCompassTheme

class MainActivity : ComponentActivity() {
    private val viewModel: CompassViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            CompassScreen(viewModel = viewModel)
        }
    }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    // Kept for backward compatibility with unit tests
    androidx.compose.material3.Text(text = "Hello $name!", modifier = modifier)
}

@Preview(showBackground = true)
@Composable
fun CompassScreenPreview() {
    OfflineCompassTheme(nightMode = false) {
        val dummyState = CompassState(
            azimuth = 42f,
            pitch = 1.2f,
            roll = -0.8f,
            magneticField = 48.5f,
            declination = 3.5f,
            useTrueNorth = true
        )
        CompassDial(state = dummyState)
    }
}
