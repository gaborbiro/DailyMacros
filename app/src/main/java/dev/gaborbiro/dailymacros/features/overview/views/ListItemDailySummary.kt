package dev.gaborbiro.dailymacros.features.overview.views

import android.content.res.Configuration
import android.util.Range
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import dev.gaborbiro.dailymacros.design.PaddingDouble
import dev.gaborbiro.dailymacros.design.PaddingHalf
import dev.gaborbiro.dailymacros.features.common.views.ViewPreviewContext
import dev.gaborbiro.dailymacros.features.common.model.DailySummaryEntry
import dev.gaborbiro.dailymacros.features.common.model.ListUiModelDailySummary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ListItemDailySummary(
    modifier: Modifier = Modifier,
    model: ListUiModelDailySummary,
    showTopPadding: Boolean,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
    ) {
        Text(
            modifier = Modifier
                .padding(horizontal = PaddingHalf)
                .let {
                    if (showTopPadding) {
                        it.padding(top = PaddingDouble)
                    } else {
                        it
                    }
                }
                .align(Alignment.CenterHorizontally),
            text = model.dayTitle,
            style = MaterialTheme.typography.titleMedium,
        )
        model.infoMessage?.let {
            Text(
                modifier = Modifier
                    .padding(PaddingHalf)
                    .align(Alignment.CenterHorizontally),
                text = it,
                style = MaterialTheme.typography.bodyMedium,
            )
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = PaddingHalf, vertical = PaddingHalf),
        ) {
            val rowHeight = 32.dp
            val leftColumn = model.entries.filterIndexed { index, _ -> index % 2 == 0 }
            val rightColumn = model.entries.filterIndexed { index, _ -> index % 2 == 1 }
            Column(
                horizontalAlignment = Alignment.End,
            ) {
                leftColumn.forEach {
                    DailyMacroTitleView(
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
                    DailyMacroBarView(
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
                    DailyMacroTitleView(
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
                    DailyMacroBarView(
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

@Preview
@Composable
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
private fun ListItemDailySummaryPreview() {
    ViewPreviewContext {
        ListItemDailySummary(
            modifier = Modifier
                .background(MaterialTheme.colorScheme.background),
            model = ListUiModelDailySummary(
                listItemId = 1L,
                dayTitle = "Yesterday",
                infoMessage = "\uD83D\uDCA1Due to timezone change, you now have 6 hours left of this day. To help your stomach adjust, consider smaller meals, spread out evenly.",
                entries = listOf(
                    DailySummaryEntry(
                        title = "Calories",
                        progress0to1 = .15f,
                        progressLabel = "1005kcal",
                        targetRange0to1 = Range(.84f, .88f),
                        targetRangeLabel = "2.1-2.2",
                        color = { it.calorieColor },
                    ),
                    DailySummaryEntry(
                        title = "Protein",
                        progress0to1 = .0809f,
                        progressLabel = "110g",
                        targetRange0to1 = Range(.8095f, .9047f),
                        targetRangeLabel = "170-190g",
                        color = { it.proteinColor },
                    ),
                    DailySummaryEntry(
                        title = "Fat",
                        progress0to1 = .2121f,
                        progressLabel = "30g",
                        targetRange0to1 = Range(.6818f, .9091f),
                        targetRangeLabel = "45-60g",
                        color = { it.fatColor },
                    ),
                    DailySummaryEntry(
                        title = "Carbs",
                        progress0to1 = .4818f,
                        progressLabel = "105g",
                        targetRange0to1 = Range(.6818f, .9091f),
                        targetRangeLabel = "150-200g",
                        color = { it.carbsColor },
                    ),
                    DailySummaryEntry(
                        title = "Sugar",
                        progress0to1 = .2955f,
                        progressLabel = "35g",
                        targetRange0to1 = Range(.9091f, .9091f),
                        targetRangeLabel = "<40g/<25g added",
                        color = { it.carbsColor },
                    ),
                    DailySummaryEntry(
                        title = "Salt",
                        progress0to1 = 1.2f,
                        progressLabel = "6g",
                        targetRange0to1 = Range(.9091f, .9091f),
                        targetRangeLabel = "<5g (≈2g Na)",
                        color = { it.saltColor },
                    ),
                    DailySummaryEntry(
                        title = "Fibre",
                        progress0to1 = .0f,
                        progressLabel = "0g",
                        targetRange0to1 = Range(.9091f, .9091f),
                        targetRangeLabel = "30-38g",
                        color = { it.fibreColor },
                    ),
                ),
            ),
            showTopPadding = false,
        )
    }
}
