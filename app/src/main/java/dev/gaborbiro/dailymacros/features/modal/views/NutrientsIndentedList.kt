package dev.gaborbiro.dailymacros.features.modal.views

import android.content.res.Configuration
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import dev.gaborbiro.dailymacros.design.PaddingDefault
import dev.gaborbiro.dailymacros.design.PaddingDouble
import dev.gaborbiro.dailymacros.design.PaddingQuarter
import dev.gaborbiro.dailymacros.features.common.views.ViewPreviewContext
import dev.gaborbiro.dailymacros.features.modal.model.NutrientsBreakdownUiModel

@Composable
internal fun NutrientsIndentedList(
    nutrientsBreakdown: NutrientsBreakdownUiModel,
) {
    nutrientsBreakdown.calories?.let {
        PillLabel(
            modifier = Modifier
                .padding(horizontal = PaddingDefault)
                .padding(top = PaddingQuarter),
            text = it,
            contentColor = MaterialTheme.colorScheme.onSurface,
            backgroundColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f),
            border = null,
            elevation = 0.dp,
        )
    }

    nutrientsBreakdown.protein?.let {
        PillLabel(
            modifier = Modifier
                .padding(horizontal = PaddingDefault)
                .padding(top = PaddingQuarter),
            text = it,
            contentColor = MaterialTheme.colorScheme.onSurface,
            backgroundColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f),
            border = null,
            elevation = 0.dp,
        )
    }

    nutrientsBreakdown.fat?.let {
        PillLabel(
            modifier = Modifier
                .padding(horizontal = PaddingDefault)
                .padding(top = PaddingQuarter),
            text = it,
            contentColor = MaterialTheme.colorScheme.onSurface,
            backgroundColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f),
            border = null,
            elevation = 0.dp,
        )
    }

    nutrientsBreakdown.ofWhichSaturated?.let {
        PillLabel(
            modifier = Modifier
                .padding(horizontal = PaddingDefault)
                .padding(start = PaddingDefault, top = PaddingQuarter),
            text = it,
            contentColor = MaterialTheme.colorScheme.onSurface,
            backgroundColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f),
            border = null,
            elevation = 0.dp,
        )
    }

    nutrientsBreakdown.carbs?.let {
        PillLabel(
            modifier = Modifier
                .padding(horizontal = PaddingDefault)
                .padding(top = PaddingQuarter),
            text = it,
            contentColor = MaterialTheme.colorScheme.onSurface,
            backgroundColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f),
            border = null,
            elevation = 0.dp,
        )
    }

    nutrientsBreakdown.ofWhichSugar?.let {
        PillLabel(
            modifier = Modifier
                .padding(horizontal = PaddingDefault)
                .padding(start = PaddingDefault, top = PaddingQuarter),
            text = it,
            contentColor = MaterialTheme.colorScheme.onSurface,
            backgroundColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f),
            border = null,
            elevation = 0.dp,
        )
    }

    nutrientsBreakdown.ofWhichAddedSugar?.let {
        PillLabel(
            modifier = Modifier
                .padding(horizontal = PaddingDefault)
                .padding(start = PaddingDouble, top = PaddingQuarter),
            text = it,
            contentColor = MaterialTheme.colorScheme.onSurface,
            backgroundColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f),
            border = null,
            elevation = 0.dp,
        )
    }

    nutrientsBreakdown.salt?.let {
        PillLabel(
            modifier = Modifier
                .padding(horizontal = PaddingDefault)
                .padding(top = PaddingQuarter),
            text = it,
            contentColor = MaterialTheme.colorScheme.onSurface,
            backgroundColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f),
            border = null,
            elevation = 0.dp,
        )
    }

    nutrientsBreakdown.fibre?.let {
        PillLabel(
            modifier = Modifier
                .padding(horizontal = PaddingDefault)
                .padding(top = PaddingQuarter),
            text = it,
            contentColor = MaterialTheme.colorScheme.onSurface,
            backgroundColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f),
            border = null,
            elevation = 0.dp,
        )
    }

    nutrientsBreakdown.notes?.let {
        PillLabel(
            modifier = Modifier
                .padding(horizontal = PaddingDefault)
                .padding(top = PaddingQuarter),
            text = it,
            contentColor = MaterialTheme.colorScheme.onSurface,
            backgroundColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f),
            border = null,
            elevation = 0.dp,
        )
    }
}

@Preview
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun NutrientsIndentedListPreview() {
    ViewPreviewContext {
        NutrientsIndentedList(
            nutrientsBreakdown = NutrientsBreakdownUiModel(
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
