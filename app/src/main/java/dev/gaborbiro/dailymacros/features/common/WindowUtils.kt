package dev.gaborbiro.dailymacros.features.common

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.systemBars
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity


/**
 * edgeToEdge allows our content to go underneath the status bar. The colors on our screens can
 * sometimes collide with the texts/icons in the status bar, so we use a semi-transparent overlay.
 *
 * Use LocalStatusBarOverlayManager.current.DisableStatusBarOverlay() to disable this overlay (it
 * will automatically restore itself when you leave the screen (leave composition to be precise)).
 */
@Composable
internal fun StatusBarOverlay() {
    val systemBarHeight = with(LocalDensity.current) {
        WindowInsets.systemBars.getTop(this).toDp()
    }
    val alpha = if (isSystemInDarkTheme()) .5f else .8f
    Spacer(
        modifier = Modifier
            .fillMaxWidth()
            .height(systemBarHeight)
            .background(MaterialTheme.colorScheme.background.copy(alpha = alpha))
    )
}
