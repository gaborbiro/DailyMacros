package dev.gaborbiro.dailymacros.features.modal.views

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import dev.gaborbiro.dailymacros.design.PaddingDefault
import dev.gaborbiro.dailymacros.features.common.verticalScrollWithBar
import kotlin.math.min


@Composable
internal fun ScrollableContentDialog(
    onDismissRequested: () -> Unit,
    content: @Composable ColumnScope.() -> Unit,
    buttons: @Composable () -> Unit,
) {
    Dialog(
        onDismissRequest = onDismissRequested,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = false,   // <-- we will handle outside taps
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false,
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
        ) {
            // Scrim/outside area: only dismiss on TAP (no drag)
            Box(
                Modifier
                    .matchParentSize()
                    .pointerInput(Unit) {
                        detectTapGestures(onTap = { onDismissRequested() })
                    }
            )
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxSize()
                    .windowInsetsPadding(WindowInsets.systemBars.union(WindowInsets.ime))
            ) {
                val max = this.maxHeight
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = max)
                        .padding(PaddingDefault),
                    shape = MaterialTheme.shapes.medium,
//                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    shadowElevation = 6.dp,
                ) {
                    Column {
                        Box(
                            modifier = Modifier
                                .weight(1f, fill = false)
                        ) {
                            val scrollState = rememberScrollState()
                            Column(
                                modifier = Modifier
                                    .verticalScrollWithBar(
                                        scrollState = scrollState,
                                        autoFade = false,
                                    )
                            ) {
                                content()
                            }

                            val shadowHeight: Dp = 12.dp
                            val fadeDistance: Dp = 32.dp // px range over which the shadow ramps up/down
                            val maxShadowAlpha = 0.16f
                            val fadePx = with(LocalDensity.current) { fadeDistance.toPx() }

                            val targetBottom = remember {
                                derivedStateOf {
                                    val remaining =
                                        (scrollState.maxValue - scrollState.value).toFloat().coerceAtLeast(0f)
                                    val d = min(remaining, fadePx)
                                    (d / fadePx) * maxShadowAlpha
                                }
                            }

                            Box(
                                Modifier
                                    .fillMaxWidth()
                                    .align(Alignment.BottomCenter)
                                    .height(shadowHeight)
                                    .graphicsLayer { alpha = targetBottom.value }
                                    .background(
                                        Brush.verticalGradient(
                                            0f to Color.Transparent,
                                            1f to Color.Black // final opacity controlled by graphicsLayer alpha
                                        )
                                    )
                            )
                        }

                        buttons()
                    }
                }
            }
        }
    }
}
