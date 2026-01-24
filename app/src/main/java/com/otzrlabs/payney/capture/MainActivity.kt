package com.otzrlabs.payney.capture

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.otzrlabs.payney.capture.ui.brand.PayNeyBrandHeader
import com.otzrlabs.payney.capture.ui.nav.PayNeyNavHost
import com.otzrlabs.payney.capture.ui.theme.PayNeyCaptureTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            PayNeyCaptureTheme {
                // Surface fills the whole window edge-to-edge (background bleeds
                // behind the system bars); the Column inside is padded to the
                // safe-drawing insets so the brand header and screen content
                // never sit underneath the status bar or gesture/nav bar.
                Surface(modifier = Modifier.fillMaxSize()) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .windowInsetsPadding(WindowInsets.safeDrawing),
                    ) {
                        PayNeyBrandHeader()
                        PayNeyNavHost(modifier = Modifier.weight(1f).fillMaxWidth())
                    }
                }
            }
        }
    }
}
