package dev.gaborbiro.dailymacros.features.modal.views

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.gaborbiro.dailymacros.design.PaddingDefault

@Composable
internal fun MacroTable(
    calories: String?,
    protein: String?,
    fat: String?,
    saturated: String?,
    carbs: String?,
    sugar: String?,
    salt: String?,
    fibre: String?,
    notes: String?,
) {
    if (calories != null ||
        protein != null ||
        fat != null ||
        saturated != null ||
        carbs != null ||
        sugar != null ||
        salt != null ||
        fibre != null ||
        notes != null
    ) {
        Spacer(
            modifier = Modifier.Companion
                .height(PaddingDefault)
        )
    }

    calories?.let {
        PillLabel(
            modifier = Modifier.Companion
                .padding(horizontal = PaddingDefault)
                .padding(top = 4.dp),
            text = calories,
            contentColor = MaterialTheme.colorScheme.onSurface,
            backgroundColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f),
            border = null,
            elevation = 0.dp,
        )
    }

    protein?.let {
        PillLabel(
            modifier = Modifier.Companion
                .padding(horizontal = PaddingDefault)
                .padding(top = 4.dp),
            text = protein,
            contentColor = MaterialTheme.colorScheme.onSurface,
            backgroundColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f),
            border = null,
            elevation = 0.dp,
        )
    }

    fat?.let {
        PillLabel(
            modifier = Modifier.Companion
                .padding(horizontal = PaddingDefault)
                .padding(top = 4.dp),
            text = fat,
            contentColor = MaterialTheme.colorScheme.onSurface,
            backgroundColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f),
            border = null,
            elevation = 0.dp,
        )
    }

    saturated?.let {
        PillLabel(
            modifier = Modifier.Companion
                .padding(horizontal = PaddingDefault)
                .padding(start = 16.dp, top = 4.dp),
            text = saturated,
            contentColor = MaterialTheme.colorScheme.onSurface,
            backgroundColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f),
            border = null,
            elevation = 0.dp,
        )
    }

    carbs?.let {
        PillLabel(
            modifier = Modifier.Companion
                .padding(horizontal = PaddingDefault)
                .padding(top = 4.dp),
            text = carbs,
            contentColor = MaterialTheme.colorScheme.onSurface,
            backgroundColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f),
            border = null,
            elevation = 0.dp,
        )
    }

    sugar?.let {
        PillLabel(
            modifier = Modifier.Companion
                .padding(horizontal = PaddingDefault)
                .padding(start = 16.dp, top = 4.dp),
            text = sugar,
            contentColor = MaterialTheme.colorScheme.onSurface,
            backgroundColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f),
            border = null,
            elevation = 0.dp,
        )
    }

    salt?.let {
        PillLabel(
            modifier = Modifier.Companion
                .padding(horizontal = PaddingDefault)
                .padding(top = 4.dp),
            text = salt,
            contentColor = MaterialTheme.colorScheme.onSurface,
            backgroundColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f),
            border = null,
            elevation = 0.dp,
        )
    }

    fibre?.let {
        PillLabel(
            modifier = Modifier.Companion
                .padding(horizontal = PaddingDefault)
                .padding(top = 4.dp),
            text = fibre,
            contentColor = MaterialTheme.colorScheme.onSurface,
            backgroundColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f),
            border = null,
            elevation = 0.dp,
        )
    }

    notes?.let {
        PillLabel(
            modifier = Modifier.Companion
                .padding(horizontal = PaddingDefault)
                .padding(top = 4.dp),
            text = notes,
            contentColor = MaterialTheme.colorScheme.onSurface,
            backgroundColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f),
            border = null,
            elevation = 0.dp,
        )
    }

    if (calories != null ||
        protein != null ||
        fat != null ||
        saturated != null ||
        carbs != null ||
        sugar != null ||
        salt != null ||
        fibre != null ||
        notes != null
    ) {
        Spacer(
            modifier = Modifier.Companion
                .height(PaddingDefault)
        )
    }
}
