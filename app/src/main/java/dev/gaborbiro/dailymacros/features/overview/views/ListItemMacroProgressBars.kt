package dev.gaborbiro.dailymacros.features.overview.views

import android.content.res.Configuration
import android.util.Range
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import dev.gaborbiro.dailymacros.design.DailyMacrosColors
import dev.gaborbiro.dailymacros.design.DailyMacrosTheme
import dev.gaborbiro.dailymacros.design.PaddingHalf
import dev.gaborbiro.dailymacros.features.common.model.ListUIModelMacroProgress
import dev.gaborbiro.dailymacros.features.common.model.MacroProgressItem

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
                .padding(PaddingHalf)
                .align(Alignment.CenterHorizontally),
            text = model.dayTitle,
            style = MaterialTheme.typography.titleMedium,
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = PaddingHalf, vertical = PaddingHalf),
        ) {
            val rowHeight = 32.dp
            val leftColumn = model.progress.filterIndexed { index, _ -> index % 2 == 0 }
            val rightColumn = model.progress.filterIndexed { index, _ -> index % 2 == 1 }
            Column(
                horizontalAlignment = Alignment.End,
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
                leftColumn.forEachIndexed { index, item ->
                    MacroProgressBar(
                        modifier = Modifier
                            .height(rowHeight),
                        model = item,
                        rowIndex = index,
                        totalRowCount = leftColumn.size,
                    )
                }
            }
            Column(
                horizontalAlignment = Alignment.End,
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
                rightColumn.forEachIndexed { index, item ->
                    MacroProgressBar(
                        modifier = Modifier
                            .height(rowHeight),
                        model = item,
                        rowIndex = index,
                        totalRowCount = rightColumn.size,
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MacroTitleView(
    modifier: Modifier,
    model: MacroProgressItem,
) {
    Text(
        modifier = modifier
            .padding(start = PaddingHalf)
            .wrapContentHeight(),
        text = model.title,
        textAlign = TextAlign.End,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onBackground,
    )
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
                progress = listOf(
                    MacroProgressItem(
                        title = "Calories",
                        progress0to1 = .15f,
                        progressLabel = "1005kcal",
                        targetRange0to1 = Range(.84f, .88f),
                        rangeLabel = "2.1-2.2",
                        color = DailyMacrosColors.calorieColor,
                    ),
                    MacroProgressItem(
                        title = "Protein",
                        progress0to1 = .0809f,
                        progressLabel = "110g",
                        targetRange0to1 = Range(.8095f, .9047f),
                        rangeLabel = "170-190g",
                        color = DailyMacrosColors.proteinColor,
                    ),
                    MacroProgressItem(
                        title = "Fat",
                        progress0to1 = .2121f,
                        progressLabel = "30g",
                        targetRange0to1 = Range(.6818f, .9091f),
                        rangeLabel = "45-60g",
                        color = DailyMacrosColors.fatColor,
                    ),
                    MacroProgressItem(
                        title = "Carbs",
                        progress0to1 = .4818f,
                        progressLabel = "105g",
                        targetRange0to1 = Range(.6818f, .9091f),
                        rangeLabel = "150-200g",
                        color = DailyMacrosColors.carbsColor,
                    ),
                    MacroProgressItem(
                        title = "Sugar",
                        progress0to1 = .2955f,
                        progressLabel = "35g",
                        targetRange0to1 = Range(.9091f, .9091f),
                        rangeLabel = "<40g/<25g added",
                        color = DailyMacrosColors.carbsColor,
                    ),
                    MacroProgressItem(
                        title = "Salt",
                        progress0to1 = 1.2f,
                        progressLabel = "6g",
                        targetRange0to1 = Range(.9091f, .9091f),
                        rangeLabel = "<5g (≈2g Na)",
                        color = DailyMacrosColors.saltColor,
                    ),
                    MacroProgressItem(
                        title = "Fibre",
                        progress0to1 = .0f,
                        progressLabel = "0g",
                        targetRange0to1 = Range(.9091f, .9091f),
                        rangeLabel = "30-38g",
                        color = DailyMacrosColors.fibreColor,
                    ),
                )
            ),
        )
    }
}
