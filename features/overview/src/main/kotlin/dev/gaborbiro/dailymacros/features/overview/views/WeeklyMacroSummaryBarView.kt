package dev.gaborbiro.dailymacros.features.overview.views

import android.content.res.Configuration
import android.util.Range
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import dev.gaborbiro.dailymacros.design.LocalExtraColorScheme
import dev.gaborbiro.dailymacros.design.PaddingHalf
import dev.gaborbiro.dailymacros.features.common.views.ViewPreviewContext
import dev.gaborbiro.dailymacros.features.overview.model.ChangeDirection
import dev.gaborbiro.dailymacros.features.overview.model.ChangeIndicator
import dev.gaborbiro.dailymacros.features.overview.model.NutrientSummaryStatEntry
import kotlinx.coroutines.delay

@Composable
internal fun WeeklyMacroSummaryBarView(
    modifier: Modifier,
    model: NutrientSummaryStatEntry,
    rowIndex: Int,
    totalRowCount: Int,
) {
    val animated = remember { Animatable(0f) }

    val initialPause = 500 // ms before first row
    val stagger = 300 // ms per row
    val baseDuration = 400 // ms for the last row's animation

    // Total time for the whole wave (pause + stagger + last row anim)
    val totalAnimDuration =
        remember(totalRowCount) { initialPause + baseDuration + (totalRowCount - 1) * stagger }

    LaunchedEffect(model.progress0to1, totalRowCount) {
        // The bar itself never draws past 100% width (see ProgressView), so animate only up to
        // that cap -- otherwise a well-over-target value keeps this animation running for its
        // full duration while the bar sits visually maxed out well before that, making it look
        // like it snapped to full almost instantly compared to bars that stay under 100%.
        val animationTarget = model.progress0to1.coerceAtMost(1f)

        // Delay for this row = pause + rowIndex * stagger
        delay(initialPause + rowIndex * stagger.toLong())

        // Remaining time until the global end
        val timeLeft = totalAnimDuration - (initialPause + rowIndex * stagger)
        if (timeLeft > 0) {
            animated.animateTo(
                targetValue = animationTarget,
                animationSpec = tween(
                    durationMillis = timeLeft,
                    easing = OvershootInterpolatorEasing
                )
            )
        } else {
            animated.snapTo(animationTarget)
        }
    }

    val progress = animated.value
    val onBackground = MaterialTheme.colorScheme.onBackground
    val extraColors = LocalExtraColorScheme.current

    val (barColor, trackColor) = remember(progress) {
        progressColors(progress0to1 = progress, base = model.color(extraColors), onBackground = onBackground)
    }

    Column(
        modifier = modifier
            .padding(horizontal = PaddingHalf)
            .padding(top = 7.dp),
    ) {
        ProgressView(
            modifier = Modifier.fillMaxWidth(),
            progress0to1 = progress,
            min0to1 = model.targetRange0to1.lower,
            max0to1 = model.targetRange0to1.upper,
            progressColor = barColor,
            trackColor = trackColor,
        )

        Row(
            modifier = Modifier
                .padding(top = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            val marks = severityMarks(model.progress0to1)
            Text(
                modifier = Modifier.weight(1f),
                text = if (marks.isEmpty()) model.progressLabel else "${model.progressLabel} $marks",
                style = MaterialTheme.typography.labelSmall,
                color = if (model.progress0to1 > 1f) Color.Red else Color.Unspecified,
            )
            ChangeIndicatorView(
                changeIndicator = model.changeIndicator,
                showColor = false,
                textStyle = MaterialTheme.typography.labelSmall,
            )
        }
    }
}

@Preview
@Composable
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
private fun WeeklyMacroSummaryBarViewPreview() {
    val macro1 = NutrientSummaryStatEntry(
        title = "Calories",
        progress0to1 = .15f,
        progressLabel = "1005kcal",
        targetRange0to1 = Range(.84f, 1f),
        changeIndicator = ChangeIndicator(
            direction = ChangeDirection.UP,
            value = "+50kcal",
        ),
        color = { it.caloriesColor },
    )
    val macro2 = NutrientSummaryStatEntry(
        title = "Salt",
        progress0to1 = 1.5f,
        progressLabel = "110g",
        targetRange0to1 = Range(.8095f, 1f),
        changeIndicator = ChangeIndicator(
            direction = ChangeDirection.DOWN,
            value = "-50kcal",
        ),
        color = { it.proteinColor },
    )
    ViewPreviewContext {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = PaddingHalf),
        ) {
            val rowHeight = 32.dp
            Column(
                modifier = Modifier
                    .wrapContentWidth()
            ) {
                WeeklySummaryTitleView(
                    modifier = Modifier
                        .height(rowHeight),
                    model = macro1,
                )
            }
            Column(
                modifier = Modifier
                    .weight(.5f)
            ) {
                WeeklyMacroSummaryBarView(
                    modifier = Modifier
                        .height(rowHeight),
                    model = macro1,
                    rowIndex = 0,
                    totalRowCount = 2,
                )
            }
            Column(
                modifier = Modifier
                    .wrapContentWidth()
            ) {
                WeeklySummaryTitleView(
                    modifier = Modifier
                        .height(rowHeight),
                    model = macro2,
                )
            }
            Column(
                modifier = Modifier
                    .weight(.5f)
            ) {
                WeeklyMacroSummaryBarView(
                    modifier = Modifier
                        .height(rowHeight),
                    model = macro2,
                    rowIndex = 1,
                    totalRowCount = 2,
                )
            }
        }
    }
}
