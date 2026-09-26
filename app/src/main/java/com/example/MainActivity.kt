package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.example.ui.LifeRhythmViewModel
import com.example.ui.navigation.LifeRhythmAppRoot
import com.example.ui.theme.LifeRhythmTheme

class MainActivity : ComponentActivity() {

    private val viewModel: LifeRhythmViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            LifeRhythmTheme {
                LifeRhythmAppRoot(viewModel = viewModel)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Re-check permissions and refresh upon returning from Android Settings
        viewModel.checkPermissions()
    }
}
