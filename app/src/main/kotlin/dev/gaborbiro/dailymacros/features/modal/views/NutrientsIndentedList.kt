package dev.gaborbiro.dailymacros.features.modal.views

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import dev.gaborbiro.dailymacros.design.LocalExtraColorScheme
import dev.gaborbiro.dailymacros.design.PaddingDefault
import dev.gaborbiro.dailymacros.design.PaddingDouble
import dev.gaborbiro.dailymacros.features.common.views.ViewPreviewContext
import dev.gaborbiro.dailymacros.features.modal.model.NutrientBreakdownUiModel

@Composable
internal fun NutrientsIndentedList(
    nutrientBreakdown: NutrientBreakdownUiModel,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = PaddingDefault),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        nutrientBreakdown.calories?.let {
        OutlinedText(
            modifier = Modifier.fillMaxWidth(),
            text = it,
            contentColor = Color.Black,
            backgroundColor = LocalExtraColorScheme.current.caloriesColor,
            border = null,
            elevation = 0.dp,
        )
    }

    nutrientBreakdown.protein?.let {
        OutlinedText(
            modifier = Modifier.fillMaxWidth(),
            text = it,
            contentColor = Color.Black,
            backgroundColor = LocalExtraColorScheme.current.proteinColor,
            border = null,
            elevation = 0.dp,
        )
    }

    nutrientBreakdown.fat?.let {
        OutlinedText(
            modifier = Modifier.fillMaxWidth(),
            text = it,
            contentColor = Color.Black,
            backgroundColor = LocalExtraColorScheme.current.fatColor,
            border = null,
            elevation = 0.dp,
        )
    }

    nutrientBreakdown.ofWhichSaturated?.let {
        OutlinedText(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = PaddingDefault),
            text = it,
            contentColor = Color.Black,
            backgroundColor = LocalExtraColorScheme.current.fatColor,
            border = null,
            elevation = 0.dp,
        )
    }

    nutrientBreakdown.carbs?.let {
        OutlinedText(
            modifier = Modifier.fillMaxWidth(),
            text = it,
            contentColor = Color.Black,
            backgroundColor = LocalExtraColorScheme.current.carbsColor,
            border = null,
            elevation = 0.dp,
        )
    }

    nutrientBreakdown.ofWhichSugar?.let {
        OutlinedText(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = PaddingDefault),
            text = it,
            contentColor = Color.Black,
            backgroundColor = LocalExtraColorScheme.current.carbsColor,
            border = null,
            elevation = 0.dp,
        )
    }

    nutrientBreakdown.ofWhichAddedSugar?.let {
        OutlinedText(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = PaddingDouble),
            text = it,
            contentColor = Color.Black,
            backgroundColor = LocalExtraColorScheme.current.carbsColor,
            border = null,
            elevation = 0.dp,
        )
    }

    nutrientBreakdown.salt?.let {
        OutlinedText(
            modifier = Modifier.fillMaxWidth(),
            text = it,
            contentColor = Color.Black,
            backgroundColor = LocalExtraColorScheme.current.saltColor,
            border = null,
            elevation = 0.dp,
        )
    }

    nutrientBreakdown.fibre?.let {
        OutlinedText(
            modifier = Modifier.fillMaxWidth(),
            text = it,
            contentColor = Color.Black,
            backgroundColor = LocalExtraColorScheme.current.fibreColor,
            border = null,
            elevation = 0.dp,
        )
    }

    nutrientBreakdown.notes?.let {
        OutlinedText(
            modifier = Modifier.fillMaxWidth(),
            text = it,
            border = null,
            elevation = 0.dp,
        )
    }
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
