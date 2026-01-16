package com.otzrlabs.payney.capture.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// PayNey Capture is dark-only, matching the PayNey web app's brand -- there is
// no light color scheme by design.
private val PayNeyDarkColorScheme = darkColorScheme(
    primary = PayNeyColors.Primary,
    onPrimary = PayNeyColors.OnPrimary,
    background = PayNeyColors.Background,
    onBackground = PayNeyColors.OnBackground,
    surface = PayNeyColors.Surface,
    onSurface = PayNeyColors.OnSurface,
    surfaceVariant = PayNeyColors.Surface,
    onSurfaceVariant = PayNeyColors.OnSurfaceMuted,
    outline = PayNeyColors.Outline,
    error = PayNeyColors.Error,
    onError = PayNeyColors.OnError,
)

@Composable
fun PayNeyCaptureTheme(
    // Dynamic color (Material You) would override the PayNey brand palette
    // with wallpaper-derived colors, so it defaults off to keep the app on-brand.
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val colorScheme = if (dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        dynamicDarkColorScheme(context)
    } else {
        PayNeyDarkColorScheme
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            window.navigationBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = PayNeyTypography,
        content = content,
    )
}
