package com.verumomnis.contradiction.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.verumomnis.contradiction.ui.theme.VerumOmnisTheme

/**
 * Main Activity for Verum Omnis Contradiction Engine
 *
 * Provides the entry point for the forensic analysis application.
 */
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            VerumOmnisTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    VerumOmnisApp()
                }
            }
        }
    }
}
