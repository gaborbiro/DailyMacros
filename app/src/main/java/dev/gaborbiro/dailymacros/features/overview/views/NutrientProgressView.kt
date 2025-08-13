package dev.gaborbiro.dailymacros.features.overview.views

import android.util.Range
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import dev.gaborbiro.dailymacros.design.DailyMacrosColors
import dev.gaborbiro.dailymacros.design.DailyMacrosTheme
import dev.gaborbiro.dailymacros.features.overview.model.NutrientProgress
import dev.gaborbiro.dailymacros.features.overview.model.NutrientProgressItem

@Composable
fun NutrientProgressView(model: NutrientProgress) {
    val matrix = arrayOf(
        arrayOf(
            model.calories, model.protein, model.fat,
        ),
        arrayOf(
            model.carbs, model.sugar, model.salt,
        )
    )

    EqualTable(
        rows = 2,
        cols = 3,
    ) { r, c ->
        val cellData = matrix[r][c]
        Box(
            Modifier
                .border(1.dp, DailyMacrosColors.DailyMacrosGrey80)
                .background(color = DailyMacrosColors.CardColorLight)
        ) {
            Row(Modifier.fillMaxWidth()) {
                Spacer(
                    Modifier
                        .weight(
                            cellData.range.lower
                                .coerceAtLeast(.0001f)
                        )
                )
                Box(
                    modifier = Modifier
                        .weight(
                            (cellData.range.upper - cellData.range.lower)
                                .coerceAtLeast(.0001f)
                        )
                        .fillMaxHeight()
                        .background(Color.Magenta.copy(alpha = .15f)),
                )
                Spacer(
                    Modifier
                        .weight(
                            (1 - cellData.range.upper)
                                .coerceAtLeast(.0001f)
                        )
                )
            }
            Row(Modifier.fillMaxWidth()) {
                Spacer(
                    Modifier.weight(
                        (cellData.progress)
                            .coerceAtLeast(.0001f)
                    )
                )
                Box(
                    modifier = Modifier
                        .width(2.dp)
                        .fillMaxHeight()
                        .background(Color.Blue.copy(alpha = .5f)),
                )
                Spacer(
                    Modifier
                        .weight(
                            (1 - cellData.progress)
                                .coerceAtLeast(.0001f)
                        )
                )
            }
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp)
                    .align(Alignment.Center)
            ) {
                Text(
                    color = Color.Black,
                    text = cellData.title,
                    style = MaterialTheme.typography.bodySmall,
                )
                val text = highlightSubstring(
                    text = cellData.progressLabel + " /" + cellData.rangeLabel,
                    highlight = cellData.progressLabel,
                    textStyle = MaterialTheme.typography.bodyMedium
                        .copy(color = Color.Blue.copy(alpha = .5f)),
                )
                Text(
                    modifier = Modifier
                        .padding(top = 4.dp),
                    color = Color.Black,
                    text = text,
                    style = MaterialTheme.typography.labelSmall,
                )
            }
        }
    }
}

private fun highlightSubstring(
    text: String,
    highlight: String,
    textStyle: TextStyle,
): AnnotatedString {
    val start = text.indexOf(highlight)
    return buildAnnotatedString {
        if (start == -1) {
            append(text)
        } else {
            append(text.substring(0, start))
            withStyle(
                textStyle.toSpanStyle()
            ) {
                append(highlight)
            }
            append(text.substring(start + highlight.length))
        }
    }
}

@Preview
@Composable
private fun MacroGoalsViewPreview() {
    DailyMacrosTheme {
        NutrientProgressView(
            NutrientProgress(
                calories = NutrientProgressItem(
                    title = "Calories",
                    progress = .15f,
                    progressLabel = "1005 cal",
                    range = Range(.84f, .88f),
                    rangeLabel = "2.1-2.2kcal",
                ),
                protein = NutrientProgressItem(
                    title = "Protein",
                    progress = .0809f,
                    progressLabel = "110g",
                    range = Range(.8095f, .9047f),
                    rangeLabel = "170-190g",
                ),
                fat = NutrientProgressItem(
                    title = "Fat",
                    progress = .2121f,
                    progressLabel = "30g",
                    range = Range(.6818f, .9091f),
                    rangeLabel = "45-60g",
                ),
                carbs = NutrientProgressItem(
                    title = "Carbs",
                    progress = .1818f,
                    progressLabel = "105g",
                    range = Range(.6818f, .9091f),
                    rangeLabel = "150-200g",
                ),
                sugar = NutrientProgressItem(
                    title = "Sugar",
                    progress = .2955f,
                    progressLabel = "35g",
                    range = Range(.9091f, .9091f),
                    rangeLabel = "<40g ttl., <25g",
                ),
                salt = NutrientProgressItem(
                    title = "Salt",
                    progress = .0f,
                    progressLabel = "0g",
                    range = Range(.9091f, .9091f),
                    rangeLabel = "<5g (≈2g Na)",
                ),
            )
        )
    }
}
