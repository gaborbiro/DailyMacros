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
import dev.gaborbiro.dailymacros.design.PaddingDefault
import dev.gaborbiro.dailymacros.design.PaddingDouble
import dev.gaborbiro.dailymacros.design.PaddingHalf
import dev.gaborbiro.dailymacros.design.ViewPreviewContext
import dev.gaborbiro.dailymacros.features.common.model.ChangeDirection
import dev.gaborbiro.dailymacros.features.common.model.ChangeIndicator
import dev.gaborbiro.dailymacros.features.common.model.ListUIModelWeeklyReport
import dev.gaborbiro.dailymacros.features.common.model.WeeklySummaryMacroProgressItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ListItemWeeklySummary(
    modifier: Modifier = Modifier,
    model: ListUIModelWeeklyReport,
) {
    Column(
        modifier = modifier
            .padding(top = PaddingDouble)
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
    ) {
        Text(
            modifier = Modifier
                .padding(horizontal = PaddingHalf)
                .padding(top = PaddingHalf)
                .align(Alignment.CenterHorizontally),
            text = "Summary for your week below",
            style = MaterialTheme.typography.titleLarge.copy(textDecoration = TextDecoration.Underline),
        )
        Row(
            modifier = Modifier
                .padding(top = PaddingHalf)
                .align(Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                modifier = Modifier
                    .padding(horizontal = PaddingHalf),
                text = "Adherence score: ${model.averageAdherence100Percentage}%",
                style = MaterialTheme.typography.titleMedium,
            )
            model.adherenceChange?.let {
                ChangeIndicatorView(
                    changeIndicator = it,
                    showColor = true,
                    textStyle = MaterialTheme.typography.titleSmall,
                )
            }
        }
        Text(
            modifier = Modifier
                .padding(horizontal = PaddingHalf)
                .padding(top = PaddingHalf)
                .align(Alignment.Start),
            text = "Averages:",
            style = MaterialTheme.typography.titleMedium,
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = PaddingHalf)
                .padding(top = PaddingHalf, bottom = PaddingDefault),
        ) {
            val rowHeight = 32.dp
            val leftColumn = model.weeklyProgress.filterIndexed { index, _ -> index % 2 == 0 }
            val rightColumn = model.weeklyProgress.filterIndexed { index, _ -> index % 2 == 1 }
            Column(
                horizontalAlignment = Alignment.End,
            ) {
                leftColumn.forEach {
                    WeeklySummaryTitleView(
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
                    WeeklyMacroSummaryBarView(
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
                    WeeklySummaryTitleView(
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
                    WeeklyMacroSummaryBarView(
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
    ViewPreviewContext {
        ListItemWeeklySummary(
            modifier = Modifier
                .background(MaterialTheme.colorScheme.background),
            model = ListUIModelWeeklyReport(
                listItemId = 1L,
                weeklyProgress = listOf(
                    WeeklySummaryMacroProgressItem(
                        title = "Calories",
                        progress0to1 = .15f,
                        progressLabel = "1005kcal",
                        targetRange0to1 = Range(.84f, .88f),
                        changeIndicator = ChangeIndicator(ChangeDirection.UP, "+5.2%"),
                        color = { it.calorieColor },
                    ),
                    WeeklySummaryMacroProgressItem(
                        title = "Protein",
                        progress0to1 = .0809f,
                        progressLabel = "110g",
                        targetRange0to1 = Range(.8095f, .9047f),
                        changeIndicator = ChangeIndicator(ChangeDirection.DOWN, "-3.1%"),
                        color = { it.proteinColor },
                    ),
                    WeeklySummaryMacroProgressItem(
                        title = "Fat",
                        progress0to1 = .2121f,
                        progressLabel = "30g",
                        targetRange0to1 = Range(.6818f, .9091f),
                        changeIndicator = ChangeIndicator(ChangeDirection.NEUTRAL, "0%"),
                        color = { it.fatColor },
                    ),
                    WeeklySummaryMacroProgressItem(
                        title = "Carbs",
                        progress0to1 = .4818f,
                        progressLabel = "105g",
                        targetRange0to1 = Range(.6818f, .9091f),
                        changeIndicator = ChangeIndicator(ChangeDirection.UP, "+2.5%"),
                        color = { it.carbsColor },
                    ),
                    WeeklySummaryMacroProgressItem(
                        title = "Sugar",
                        progress0to1 = .2955f,
                        progressLabel = "35g",
                        targetRange0to1 = Range(.9091f, .9091f),
                        changeIndicator = ChangeIndicator(ChangeDirection.DOWN, "-1.8%"),
                        color = { it.carbsColor },
                    ),
                    WeeklySummaryMacroProgressItem(
                        title = "Salt",
                        progress0to1 = 1.2f,
                        progressLabel = "6g",
                        targetRange0to1 = Range(.9091f, .9091f),
                        changeIndicator = ChangeIndicator(ChangeDirection.UP, "+8.3%"),
                        color = { it.saltColor },
                    ),
                    WeeklySummaryMacroProgressItem(
                        title = "Fibre",
                        progress0to1 = .0f,
                        progressLabel = "0g",
                        targetRange0to1 = Range(.9091f, .9091f),
                        changeIndicator = ChangeIndicator(ChangeDirection.DOWN, "-12.5%"),
                        color = { it.fibreColor },
                    ),
                ),
                averageAdherence100Percentage = 86,
                adherenceChange = ChangeIndicator(ChangeDirection.UP, "+1.5%"),
            ),
        )
    }
}
