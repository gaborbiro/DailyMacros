package dev.gaborbiro.dailymacros.features.modal.views

import android.content.res.Configuration
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextDecoration
import dev.gaborbiro.dailymacros.R
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import dev.gaborbiro.dailymacros.design.LocalExtraColorScheme
import dev.gaborbiro.dailymacros.design.PaddingDefault
import dev.gaborbiro.dailymacros.design.PaddingDouble
import dev.gaborbiro.dailymacros.design.PaddingHalf
import dev.gaborbiro.dailymacros.design.PaddingQuarter
import dev.gaborbiro.dailymacros.features.common.views.ViewPreviewContext
import dev.gaborbiro.dailymacros.features.modal.model.NutrientBreakdownUiModel

@Composable
internal fun NutrientsIndentedList(
    modifier: Modifier = Modifier,
    nutrientBreakdown: NutrientBreakdownUiModel,
) {
    Column(
        modifier = modifier
    ) {
        nutrientBreakdown.calories?.let {
            OutlinedText(
                text = it,
                contentColor = Color.Black,
                backgroundColor = LocalExtraColorScheme.current.caloriesColor,
                border = null,
                elevation = 0.dp,
            )
            Spacer(
                modifier = Modifier
                    .height(PaddingHalf)
            )
        }

        nutrientBreakdown.protein?.let {
            OutlinedText(
                text = it,
                contentColor = Color.Black,
                backgroundColor = LocalExtraColorScheme.current.proteinColor,
                border = null,
                elevation = 0.dp,
            )
            Spacer(
                modifier = Modifier
                    .height(PaddingHalf)
            )
        }

        nutrientBreakdown.fat?.let {
            OutlinedText(
                text = it,
                contentColor = Color.Black,
                backgroundColor = LocalExtraColorScheme.current.fatColor,
                border = null,
                elevation = 0.dp,
            )
            Spacer(
                modifier = Modifier
                    .height(PaddingHalf)
            )
        }

        nutrientBreakdown.ofWhichSaturated?.let {
            OutlinedText(
                modifier = Modifier
                    .padding(start = PaddingDefault),
                text = it,
                contentColor = Color.Black,
                backgroundColor = LocalExtraColorScheme.current.fatColor,
                border = null,
                elevation = 0.dp,
            )
            Spacer(
                modifier = Modifier
                    .height(PaddingHalf)
            )
        }

        nutrientBreakdown.carbs?.let {
            OutlinedText(
                text = it,
                contentColor = Color.Black,
                backgroundColor = LocalExtraColorScheme.current.carbsColor,
                border = null,
                elevation = 0.dp,
            )
            Spacer(
                modifier = Modifier
                    .height(PaddingHalf)
            )
        }

        nutrientBreakdown.ofWhichSugar?.let {
            OutlinedText(
                modifier = Modifier
                    .padding(start = PaddingDefault),
                text = it,
                contentColor = Color.Black,
                backgroundColor = LocalExtraColorScheme.current.carbsColor,
                border = null,
                elevation = 0.dp,
            )
            Spacer(
                modifier = Modifier
                    .height(PaddingHalf)
            )
        }

        nutrientBreakdown.ofWhichAddedSugar?.let {
            OutlinedText(
                modifier = Modifier
                    .padding(start = PaddingDouble),
                text = it,
                contentColor = Color.Black,
                backgroundColor = LocalExtraColorScheme.current.carbsColor,
                border = null,
                elevation = 0.dp,
            )
            Spacer(
                modifier = Modifier
                    .height(PaddingHalf)
            )
        }

        nutrientBreakdown.salt?.let {
            OutlinedText(
                text = it,
                contentColor = Color.Black,
                backgroundColor = LocalExtraColorScheme.current.saltColor,
                border = null,
                elevation = 0.dp,
            )
            Spacer(
                modifier = Modifier
                    .height(PaddingHalf)
            )
        }

        nutrientBreakdown.fibre?.let {
            OutlinedText(
                text = it,
                contentColor = Color.Black,
                backgroundColor = LocalExtraColorScheme.current.fibreColor,
                border = null,
                elevation = 0.dp,
            )
        }

        nutrientBreakdown.notes?.takeIf { it.isNotBlank() }?.let {
            Spacer(modifier = Modifier.height(PaddingDefault))
            Text(
                text = "AI notes:",
                style = MaterialTheme.typography.bodyLarge.copy(
                    textDecoration = TextDecoration.Underline,
                ),
            )
            Spacer(modifier = Modifier.height(PaddingQuarter))
            Text(
                text = it,
                style = MaterialTheme.typography.bodyMedium,
            )
        }

        if (nutrientBreakdown.components.isNotEmpty()) {
            Spacer(modifier = Modifier.height(PaddingDefault))
            Text(
                text = "Components:",
                style = MaterialTheme.typography.bodyLarge.copy(
                    textDecoration = TextDecoration.Underline,
                ),
            )
            Spacer(modifier = Modifier.height(PaddingQuarter))
            nutrientBreakdown.components.forEach { line ->
                Text(
                    text = line,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }

        Spacer(modifier = Modifier.height(PaddingDefault))
        Text(
            text = stringResource(R.string.record_details_ai_disclaimer),
            style = MaterialTheme.typography.bodySmall.copy(
                fontStyle = FontStyle.Italic,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            ),
        )
    }
}

@Preview
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun NutrientsIndentedListPreview() {
    ViewPreviewContext {
        NutrientsIndentedList(
            nutrientBreakdown = NutrientBreakdownUiModel(
                calories = "Calories: 2100 cal",
                protein = "Protein: 150g",
                fat = "Fat 100g",
                ofWhichSaturated = "of which saturated: 20g",
                carbs = "Carbs: 100g",
                ofWhichSugar = "of which sugar: 30g",
                ofWhichAddedSugar = "of which added sugar: 15g",
                salt = "Salt: 5g",
                fibre = "Fibre: 4.5g",
                notes = "Notes: This is a note",
            ),
        )
    }
}
