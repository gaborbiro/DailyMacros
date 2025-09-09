package dev.gaborbiro.dailymacros.features.modal.views

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.gaborbiro.dailymacros.design.PaddingDefault
import dev.gaborbiro.dailymacros.design.PaddingQuarter
import dev.gaborbiro.dailymacros.features.modal.model.MacrosUIModel

@Composable
internal fun MacroTable(
    macros: MacrosUIModel,
) {
    Spacer(
        modifier = Modifier.Companion
            .height(PaddingDefault)
    )

    macros.calories?.let {
        PillLabel(
            modifier = Modifier.Companion
                .padding(horizontal = PaddingDefault)
                .padding(top = PaddingQuarter),
            text = it,
            contentColor = MaterialTheme.colorScheme.onSurface,
            backgroundColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f),
            border = null,
            elevation = 0.dp,
        )
    }

    macros.protein?.let {
        PillLabel(
            modifier = Modifier.Companion
                .padding(horizontal = PaddingDefault)
                .padding(top = PaddingQuarter),
            text = it,
            contentColor = MaterialTheme.colorScheme.onSurface,
            backgroundColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f),
            border = null,
            elevation = 0.dp,
        )
    }

    macros.fat?.let {
        PillLabel(
            modifier = Modifier.Companion
                .padding(horizontal = PaddingDefault)
                .padding(top = PaddingQuarter),
            text = it,
            contentColor = MaterialTheme.colorScheme.onSurface,
            backgroundColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f),
            border = null,
            elevation = 0.dp,
        )
    }

    macros.ofWhichSaturated?.let {
        PillLabel(
            modifier = Modifier.Companion
                .padding(horizontal = PaddingDefault)
                .padding(start = PaddingDefault, top = PaddingQuarter),
            text = it,
            contentColor = MaterialTheme.colorScheme.onSurface,
            backgroundColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f),
            border = null,
            elevation = 0.dp,
        )
    }

    macros.carbs?.let {
        PillLabel(
            modifier = Modifier.Companion
                .padding(horizontal = PaddingDefault)
                .padding(top = PaddingQuarter),
            text = it,
            contentColor = MaterialTheme.colorScheme.onSurface,
            backgroundColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f),
            border = null,
            elevation = 0.dp,
        )
    }

    macros.ofWhichSugar?.let {
        PillLabel(
            modifier = Modifier.Companion
                .padding(horizontal = PaddingDefault)
                .padding(start = PaddingDefault, top = PaddingQuarter),
            text = it,
            contentColor = MaterialTheme.colorScheme.onSurface,
            backgroundColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f),
            border = null,
            elevation = 0.dp,
        )
    }

    macros.salt?.let {
        PillLabel(
            modifier = Modifier.Companion
                .padding(horizontal = PaddingDefault)
                .padding(top = PaddingQuarter),
            text = it,
            contentColor = MaterialTheme.colorScheme.onSurface,
            backgroundColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f),
            border = null,
            elevation = 0.dp,
        )
    }

    macros.fibre?.let {
        PillLabel(
            modifier = Modifier.Companion
                .padding(horizontal = PaddingDefault)
                .padding(top = PaddingQuarter),
            text = it,
            contentColor = MaterialTheme.colorScheme.onSurface,
            backgroundColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f),
            border = null,
            elevation = 0.dp,
        )
    }

    macros.notes?.let {
        PillLabel(
            modifier = Modifier.Companion
                .padding(horizontal = PaddingDefault)
                .padding(top = PaddingQuarter),
            text = it,
            contentColor = MaterialTheme.colorScheme.onSurface,
            backgroundColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f),
            border = null,
            elevation = 0.dp,
        )
    }

    Spacer(
        modifier = Modifier.Companion
            .height(PaddingDefault)
    )
}
