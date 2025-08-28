package dev.gaborbiro.dailymacros.features.overview.views

import android.content.res.Configuration
import android.util.Range
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProgressIndicatorDefaults
import androidx.compose.material3.ProgressIndicatorDefaults.drawStopIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import dev.gaborbiro.dailymacros.design.DailyMacrosColors
import dev.gaborbiro.dailymacros.design.DailyMacrosTheme
import dev.gaborbiro.dailymacros.design.PaddingHalf
import dev.gaborbiro.dailymacros.features.common.model.ListUIModelMacroProgress
import dev.gaborbiro.dailymacros.features.common.model.MacroProgressItem
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ListItemMacroProgressBars(
    modifier: Modifier = Modifier,
    model: ListUIModelMacroProgress,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
    ) {
        Text(
            modifier = Modifier
                .padding(8.dp)
                .align(Alignment.CenterHorizontally),
            text = model.dayTitle
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = PaddingHalf),
        ) {
            val rowHeight = 32.dp
            val leftColumn = model.macros.subList(0, (model.macros.size / 2f).roundToInt())
            val rightColumn = model.macros.subList((model.macros.size / 2), model.macros.size)
            Column(
                modifier = Modifier
                    .wrapContentWidth()
            ) {
                leftColumn.forEach {
                    MacroTitleView(
                        modifier = Modifier
                            .height(rowHeight),
                        model = it,
                    )
                }
            }
            Column(
                modifier = Modifier
                    .weight(.5f)
            ) {
                leftColumn.forEach {
                    MacroProgressView(
                        modifier = Modifier
                            .height(rowHeight),
                        model = it
                    )
                }
            }
            Column(
                modifier = Modifier
                    .wrapContentWidth()
            ) {
                rightColumn.forEach {
                    MacroTitleView(
                        modifier = Modifier
                            .height(rowHeight),
                        model = it,
                    )
                }
            }
            Column(
                modifier = Modifier
                    .weight(.5f)
            ) {
                rightColumn.forEach {
                    MacroProgressView(
                        modifier = Modifier
                            .height(rowHeight),
                        model = it
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MacroTitleView(
    modifier: Modifier,
    model: MacroProgressItem,
) {
    Column(
        modifier,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            modifier = modifier
                .padding(start = PaddingHalf)
                .wrapContentHeight(),
            text = model.title,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onBackground,
        )
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MacroProgressView(
    modifier: Modifier,
    model: MacroProgressItem,
) {
    Column(
        modifier = modifier
            .padding(horizontal = PaddingHalf)
            .padding(vertical = 7.dp),
    ) {
        LinearProgressIndicator(
            progress = { model.progress },
            color = model.color,
            trackColor = MaterialTheme.colorScheme.onBackground.copy(alpha = .09f),
            strokeCap = StrokeCap.Round,
            gapSize = 0.dp,
            drawStopIndicator = {
                if (model.progress >= 1f) {
                    drawStopIndicator(
                        drawScope = this,
                        stopSize = ProgressIndicatorDefaults.LinearTrackStopIndicatorSize,
                        color = Color.Red,
                        strokeCap = StrokeCap.Square,
                    )
                }
            }
        )
        Row {
            Text(
                modifier = Modifier
                    .weight(1f),
                text = model.progressLabel,
                style = MaterialTheme.typography.labelSmall,
                color = if (model.progress >= 1f) Color.Red else Color.Unspecified,
            )
            Text(
                modifier = Modifier,
                text = model.rangeLabel,
                style = MaterialTheme.typography.labelSmall,
            )
        }
    }
}

@Preview
@Composable
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
private fun ListItemMacroProgressBarsPreview() {
    DailyMacrosTheme {
        ListItemMacroProgressBars(
            modifier = Modifier
                .background(MaterialTheme.colorScheme.background),
            model = ListUIModelMacroProgress(
                listItemId = 1L,
                dayTitle = "Yesterday",
                macros = listOf(
                    MacroProgressItem(
                        title = "Calories",
                        progress = .15f,
                        progressLabel = "1005 cal",
                        range = Range(.84f, .88f),
                        rangeLabel = "2.1-2.2kcal",
                        color = DailyMacrosColors.calorieColor,
                    ),
                    MacroProgressItem(
                        title = "Protein",
                        progress = .0809f,
                        progressLabel = "110g",
                        range = Range(.8095f, .9047f),
                        rangeLabel = "170-190g",
                        color = DailyMacrosColors.proteinColor,
                    ),
                    MacroProgressItem(
                        title = "Fat",
                        progress = .2121f,
                        progressLabel = "30g",
                        range = Range(.6818f, .9091f),
                        rangeLabel = "45-60g",
                        color = DailyMacrosColors.fatColor,
                    ),
                    MacroProgressItem(
                        title = "Carbs",
                        progress = .1818f,
                        progressLabel = "105g",
                        range = Range(.6818f, .9091f),
                        rangeLabel = "150-200g",
                        color = DailyMacrosColors.carbsColor,
                    ),
                    MacroProgressItem(
                        title = "Sugar",
                        progress = .2955f,
                        progressLabel = "35g",
                        range = Range(.9091f, .9091f),
                        rangeLabel = "<40g ttl., <25g",
                        color = DailyMacrosColors.carbsColor,
                    ),
                    MacroProgressItem(
                        title = "Salt",
                        progress = .0f,
                        progressLabel = "0g",
                        range = Range(.9091f, .9091f),
                        rangeLabel = "<5g (≈2g Na)",
                        color = DailyMacrosColors.saltColor,
                    ),
                    MacroProgressItem(
                        title = "Fibre",
                        progress = .0f,
                        progressLabel = "0g",
                        range = Range(.9091f, .9091f),
                        rangeLabel = "30-38g",
                        color = DailyMacrosColors.fibreColor,
                    ),
                )
            ),
        )
    }
}
