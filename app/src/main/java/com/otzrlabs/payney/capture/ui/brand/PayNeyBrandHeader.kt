package com.otzrlabs.payney.capture.ui.brand

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.otzrlabs.payney.capture.ui.theme.PayNeyColors

/**
 * Persistent "PayNey" wordmark pinned to the top-left of every screen, matching
 * the brand's blue (see PayNeyColors.Primary / the web app's logomark). Callers
 * place this above their screen content -- see PayNeyNavHost -- rather than each
 * screen drawing its own, so it never scrolls away or gets duplicated.
 */
@Composable
fun PayNeyBrandHeader(modifier: Modifier = Modifier) {
    Text(
        text = "PayNey",
        modifier = modifier.padding(horizontal = 24.dp, vertical = 16.dp),
        color = PayNeyColors.Primary,
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 22.sp,
        letterSpacing = 0.2.sp,
    )
}
