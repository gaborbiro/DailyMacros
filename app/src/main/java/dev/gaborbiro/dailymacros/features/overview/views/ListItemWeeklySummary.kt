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
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import dev.gaborbiro.dailymacros.design.AppTheme
import dev.gaborbiro.dailymacros.design.ExtraColors
import dev.gaborbiro.dailymacros.design.PaddingHalf
import dev.gaborbiro.dailymacros.features.common.model.ListUIModelWeeklyReport
import dev.gaborbiro.dailymacros.features.common.model.MacroProgressItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ListItemWeeklySummary(
    modifier: Modifier = Modifier,
    model: ListUIModelWeeklyReport,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
    ) {
        Text(
            modifier = Modifier
                .padding(horizontal = PaddingHalf)
                .padding(top = PaddingHalf)
                .align(Alignment.CenterHorizontally),
            text = "Weekly summary:",
            style = MaterialTheme.typography.titleLarge.copy(textDecoration = TextDecoration.Underline),
        )
        Text(
            modifier = Modifier
                .padding(horizontal = PaddingHalf)
                .padding(top = PaddingHalf)
                .align(Alignment.CenterHorizontally),
            text = "Adherence: ${model.averageAdherence100Percentage}%",
            style = MaterialTheme.typography.titleMedium,
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = PaddingHalf, vertical = PaddingHalf),
        ) {
            val rowHeight = 32.dp
            val leftColumn = model.weeklyProgress.filterIndexed { index, _ -> index % 2 == 0 }
            val rightColumn = model.weeklyProgress.filterIndexed { index, _ -> index % 2 == 1 }
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

@Preview
@Composable
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
private fun ListItemWeeklySummaryPreview() {
    AppTheme {
        ListItemWeeklySummary(
            modifier = Modifier
                .background(MaterialTheme.colorScheme.background),
            model = ListUIModelWeeklyReport(
                listItemId = 1L,
                weeklyProgress = listOf(
                    MacroProgressItem(
                        title = "Calories",
                        progress0to1 = .15f,
                        progressLabel = "1005kcal",
                        targetRange0to1 = Range(.84f, .88f),
                        targetRangeLabel = "2.1-2.2",
                        color = ExtraColors.calorieColor,
                    ),
                    MacroProgressItem(
                        title = "Protein",
                        progress0to1 = .0809f,
                        progressLabel = "110g",
                        targetRange0to1 = Range(.8095f, .9047f),
                        targetRangeLabel = "170-190g",
                        color = ExtraColors.proteinColor,
                    ),
                    MacroProgressItem(
                        title = "Fat",
                        progress0to1 = .2121f,
                        progressLabel = "30g",
                        targetRange0to1 = Range(.6818f, .9091f),
                        targetRangeLabel = "45-60g",
                        color = ExtraColors.fatColor,
                    ),
                    MacroProgressItem(
                        title = "Carbs",
                        progress0to1 = .4818f,
                        progressLabel = "105g",
                        targetRange0to1 = Range(.6818f, .9091f),
                        targetRangeLabel = "150-200g",
                        color = ExtraColors.carbsColor,
                    ),
                    MacroProgressItem(
                        title = "Sugar",
                        progress0to1 = .2955f,
                        progressLabel = "35g",
                        targetRange0to1 = Range(.9091f, .9091f),
                        targetRangeLabel = "<40g/<25g added",
                        color = ExtraColors.carbsColor,
                    ),
                    MacroProgressItem(
                        title = "Salt",
                        progress0to1 = 1.2f,
                        progressLabel = "6g",
                        targetRange0to1 = Range(.9091f, .9091f),
                        targetRangeLabel = "<5g (≈2g Na)",
                        color = ExtraColors.saltColor,
                    ),
                    MacroProgressItem(
                        title = "Fibre",
                        progress0to1 = .0f,
                        progressLabel = "0g",
                        targetRange0to1 = Range(.9091f, .9091f),
                        targetRangeLabel = "30-38g",
                        color = ExtraColors.fibreColor,
                    ),
                ),
                averageAdherence100Percentage = 86,
            ),
        )
    }
}
