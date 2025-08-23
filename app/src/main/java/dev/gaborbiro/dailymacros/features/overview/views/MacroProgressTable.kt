package dev.gaborbiro.dailymacros.features.overview.views

import android.content.res.Configuration
import android.util.Range
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import dev.gaborbiro.dailymacros.design.DailyMacrosColors
import dev.gaborbiro.dailymacros.design.DailyMacrosTheme
import dev.gaborbiro.dailymacros.features.common.model.MacroProgressItem
import dev.gaborbiro.dailymacros.features.common.model.MacroProgressTableUIModel
import java.time.LocalDate

@Composable
fun MacroProgressTable(
    modifier: Modifier = Modifier,
    model: MacroProgressTableUIModel,
) {
    val cols = 3
    val chunks = remember(model.macros) { model.macros.chunked(cols) }
    if (chunks.isEmpty()) return

    EqualTable(
        modifier = modifier,
        rows = chunks.size,
        cols = cols,
    ) { r, c ->
        val macroProgress = chunks.getOrNull(r)?.getOrNull(c)

        if (macroProgress != null) {
            Box(
                Modifier
                    .border(1.dp, color = Color.White)
                    .background(color = DailyMacrosColors.CardColorLight)
            ) {
                // Target Range
                Row(Modifier.fillMaxWidth()) {
                    Spacer(
                        Modifier
                            .weight(
                                macroProgress.range.lower
                                    .coerceAtLeast(.0001f)
                            )
                    )
                    Box(
                        modifier = Modifier
                            .weight(
                                (macroProgress.range.upper - macroProgress.range.lower)
                                    .coerceAtLeast(.0001f)
                            )
                            .fillMaxHeight()
                            .background(
                                brush = Brush.horizontalGradient(
                                    colors = listOf(
                                        Color.Magenta.copy(alpha = 0.15f),
                                        Color.Magenta.copy(alpha = 0.4f)
                                    )
                                )
                            ),
                    )
                    Spacer(
                        Modifier
                            .weight(
                                (1 - macroProgress.range.upper)
                                    .coerceAtLeast(.0001f)
                            )
                    )
                }

                // Progress
                Row(Modifier.fillMaxWidth()) {
                    Spacer(
                        Modifier.weight(
                            (macroProgress.progress)
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
                                (1 - macroProgress.progress)
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
                        text = macroProgress.title,
                        style = MaterialTheme.typography.bodySmall,
                    )
                    val text = highlightSubstring(
                        text = macroProgress.progressLabel + " /" + macroProgress.rangeLabel,
                        highlight = macroProgress.progressLabel,
                        textStyle = MaterialTheme.typography.bodyMedium
                            .copy(color = Color.Blue.copy(alpha = 1f)),
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
        } else {
            // Empty filler cell so the grid stays aligned
            Spacer(Modifier.fillMaxSize())
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
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
private fun MacroGoalsViewPreview() {
    DailyMacrosTheme {
        MacroProgressTable(
            model = MacroProgressTableUIModel(
                date = LocalDate.now(),
                macros = listOf(
                    MacroProgressItem(
                        title = "Calories",
                        progress = .15f,
                        progressLabel = "1005 cal",
                        range = Range(.84f, .88f),
                        rangeLabel = "2.1-2.2kcal",
                    ),
                    MacroProgressItem(
                        title = "Protein",
                        progress = .0809f,
                        progressLabel = "110g",
                        range = Range(.8095f, .9047f),
                        rangeLabel = "170-190g",
                    ),
                    MacroProgressItem(
                        title = "Fat",
                        progress = .2121f,
                        progressLabel = "30g",
                        range = Range(.6818f, .9091f),
                        rangeLabel = "45-60g",
                    ),
                    MacroProgressItem(
                        title = "Carbs",
                        progress = .1818f,
                        progressLabel = "105g",
                        range = Range(.6818f, .9091f),
                        rangeLabel = "150-200g",
                    ),
                    MacroProgressItem(
                        title = "Sugar",
                        progress = .2955f,
                        progressLabel = "35g",
                        range = Range(.9091f, .9091f),
                        rangeLabel = "<40g ttl., <25g",
                    ),
                    MacroProgressItem(
                        title = "Salt",
                        progress = .0f,
                        progressLabel = "0g",
                        range = Range(.9091f, .9091f),
                        rangeLabel = "<5g (≈2g Na)",
                    ),
                    MacroProgressItem(
                        title = "Fibre",
                        progress = .0f,
                        progressLabel = "0g",
                        range = Range(.9091f, .9091f),
                        rangeLabel = "30-38g",
                    )
                )
            )
        )
    }
}
