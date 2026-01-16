package com.otzrlabs.payney.capture.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * Brand tokens shared with the PayNey web app. Keep these in sync with the
 * web design system's palette -- do not scatter hex literals elsewhere.
 */
object PayNeyColors {
    val Background = Color(0xFF0A0A0D)
    val Surface = Color(0xFF16171C)
    val Outline = Color(0xFF2C2E36)
    val Primary = Color(0xFF222FA8)
    val OnPrimary = Color(0xFFF2F4F7)

    // Derived tokens: not part of the web brand spec, but needed to fill out
    // Material3's color roles sensibly against the same dark background.
    val OnBackground = OnPrimary
    val OnSurface = OnPrimary
    val OnSurfaceMuted = Color(0xFF9A9CA8)
    val Error = Color(0xFFE5484D)
    val OnError = OnPrimary
}
