package dev.gaborbiro.dailymacros.features.modal.views

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import dev.gaborbiro.dailymacros.design.LocalExtraColorScheme
import dev.gaborbiro.dailymacros.design.PaddingDefault
import dev.gaborbiro.dailymacros.features.common.views.ViewPreviewContext
import dev.gaborbiro.dailymacros.features.modal.R
import dev.gaborbiro.dailymacros.features.modal.model.NutrientBreakdownUiModel
import dev.gaborbiro.dailymacros.features.modal.model.NutrientKind
import dev.gaborbiro.dailymacros.features.modal.model.NutrientRowUiModel

private val DotSize = 12.dp
private val DotGap = 12.dp
private val IndentStep = 16.dp

/**
 * The full breakdown as quiet rows: a small dot in the macro's colour, the name, the amount on the right and what
 * contributes most underneath in muted text. "Of which" lines step in under their parent without a dot of their
 * own. The colour stays a hint so the chips' strong colours are reserved for the at-a-glance view.
 */
@Composable
internal fun NutrientsIndentedList(
    modifier: Modifier = Modifier,
    nutrientBreakdown: NutrientBreakdownUiModel,
) {
    Column(modifier = modifier) {
        nutrientBreakdown.nutrients.forEachIndexed { index, row ->
            // A little more air before each new macro than between a macro and its "of which" lines.
            if (index > 0) {
                Spacer(modifier = Modifier.height(if (row.indent == 0) 12.dp else 6.dp))
            }
            NutrientRow(row)
        }

        Spacer(modifier = Modifier.height(PaddingDefault))
        MealDetailsFootnote(text = stringResource(R.string.record_details_ai_disclaimer))
    }
}

@Composable
private fun NutrientRow(row: NutrientRowUiModel) {
    val topLevel = row.indent == 0
    Row(modifier = Modifier.fillMaxWidth()) {
        if (topLevel) {
            // Centres the dot on the label's first line.
            val lineHeight = with(LocalDensity.current) { MaterialTheme.typography.bodyMedium.lineHeight.toDp() }
            Box(
                modifier = Modifier
                    .padding(top = ((lineHeight - DotSize) / 2).coerceAtLeast(0.dp))
                    .size(DotSize)
                    .background(color = row.kind.color(), shape = CircleShape),
            )
            Spacer(modifier = Modifier.width(DotGap))
        } else {
            Spacer(modifier = Modifier.width(DotSize + DotGap + IndentStep * row.indent))
        }
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    modifier = Modifier.weight(1f),
                    text = row.label,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (topLevel) {
                        MaterialTheme.colorScheme.onSurface
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                )
                Text(
                    text = row.amount,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = if (topLevel) FontWeight.Medium else FontWeight.Normal,
                    color = if (topLevel) {
                        MaterialTheme.colorScheme.onSurface
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                )
            }
            row.sources?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = .8f),
                )
            }
        }
    }
}

@Composable
private fun NutrientKind.color(): Color = with(LocalExtraColorScheme.current) {
    when (this@color) {
        NutrientKind.CALORIES -> caloriesColor
        NutrientKind.PROTEIN -> proteinColor
        NutrientKind.FAT -> fatColor
        NutrientKind.CARBS -> carbsColor
        NutrientKind.SALT -> saltColor
        NutrientKind.FIBRE -> fibreColor
    }
}

@Preview
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun NutrientsIndentedListPreview() {
    ViewPreviewContext {
        NutrientsIndentedList(
            modifier = Modifier.padding(PaddingDefault),
            nutrientBreakdown = NutrientBreakdownUiModel(
                nutrients = previewNutrientRows(),
                notes = null,
            ),
        )
    }
}

internal fun previewNutrientRows() = listOf(
    NutrientRowUiModel(NutrientKind.CALORIES, "Calories", "260kcal"),
    NutrientRowUiModel(NutrientKind.PROTEIN, "Protein", "6g", sources = "red bean paste, wheat flour batter, egg"),
    NutrientRowUiModel(NutrientKind.FAT, "Fat", "6g", sources = "oil, egg, milk"),
    NutrientRowUiModel(NutrientKind.FAT, "Saturated", "2g", sources = "milk, egg, oil", indent = 1),
    NutrientRowUiModel(NutrientKind.CARBS, "Carbs", "46g", sources = "wheat flour, red bean paste, sugar"),
    NutrientRowUiModel(NutrientKind.CARBS, "Sugar", "18g", sources = "red bean paste, sugar", indent = 1),
    NutrientRowUiModel(NutrientKind.CARBS, "Added sugar", "15g", sources = "sugar", indent = 2),
    NutrientRowUiModel(NutrientKind.SALT, "Salt", "0.4g", sources = "batter, red bean paste"),
    NutrientRowUiModel(NutrientKind.FIBRE, "Fibre", "3g", sources = "red bean paste, wheat flour"),
)
