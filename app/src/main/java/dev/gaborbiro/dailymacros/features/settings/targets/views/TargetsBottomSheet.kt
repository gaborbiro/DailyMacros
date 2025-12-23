package dev.gaborbiro.dailymacros.features.settings.targets.views

import android.content.res.Configuration
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.systemGestures
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.ModalBottomSheetProperties
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RangeSlider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import dev.gaborbiro.dailymacros.design.PaddingDefault
import dev.gaborbiro.dailymacros.design.PaddingHalf
import dev.gaborbiro.dailymacros.design.PreviewContext
import dev.gaborbiro.dailymacros.design.ViewPreviewContext
import dev.gaborbiro.dailymacros.features.common.verticalScrollWithBar
import dev.gaborbiro.dailymacros.features.settings.targets.model.FieldErrors
import dev.gaborbiro.dailymacros.features.settings.targets.model.MacroType
import dev.gaborbiro.dailymacros.features.settings.targets.model.TargetUIModel
import dev.gaborbiro.dailymacros.features.settings.targets.model.TargetsEvents
import dev.gaborbiro.dailymacros.features.settings.targets.model.TargetsViewState
import dev.gaborbiro.dailymacros.features.settings.targets.model.ValidationError
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun TargetsBottomSheet(
    viewState: TargetsViewState,
    events: Flow<TargetsEvents>,
    onDismissRequested: () -> Unit,
    onTargetChanged: (MacroType, TargetUIModel) -> Unit,
    onResetTapped: () -> Unit,
    onSaveTapped: () -> Unit,
    onUnsavedTargetsDiscardTapped: () -> Unit,
    onUnsavedTargetsDismissRequested: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true,
        confirmValueChange = { true },
    )
    LaunchedEffect(Unit) {
        events.collect { event ->
            when (event) {
                TargetsEvents.Show -> sheetState.show()
                TargetsEvents.Hide -> {
                    sheetState.hide()
                    onDismissRequested()
                }
                TargetsEvents.Close -> {
                    // handled by parent container
                }
            }
        }
    }
    val systemBarHeight = with(LocalDensity.current) {
        WindowInsets.systemBars.getTop(this)
    }
    ModalBottomSheet(
        containerColor = MaterialTheme.colorScheme.surface,
        sheetState = sheetState,
        contentWindowInsets = { WindowInsets(0, systemBarHeight, 0, 0) },
        onDismissRequest = onDismissRequested,
        properties = ModalBottomSheetProperties(
            shouldDismissOnBackPress = true,
        ),
    ) {
        TargetsHeader(
            modifier = Modifier
                .background(MaterialTheme.colorScheme.surface)
                .padding(horizontal = 16.dp),
            saveButtonEnabled = viewState.canSave,
            resetButtonVisible = viewState.canReset,
            onSaveTapped = onSaveTapped,
            onResetTapped = onResetTapped,
        )
        Column(
            modifier = Modifier
                .verticalScrollWithBar(autoFade = false)
                .padding(16.dp)
                .imePadding()
        ) {
            val ordered = listOf(
                Triple(MacroType.CALORIES, "Calories", "kcal"),
                Triple(MacroType.PROTEIN, "Protein", "g"),
                Triple(MacroType.SALT, "Salt", "g"),
                Triple(MacroType.FIBRE, "Fibre", "g"),
                Triple(MacroType.FAT, "Fat", "g"),
                Triple(MacroType.SATURATED, "of which saturated", "g"),
                Triple(MacroType.CARBS, "Carbs", "g"),
                Triple(MacroType.SUGAR, "of which sugar", "g"),
            )

            ordered.forEach { (type, label, unit) ->
                val target = viewState.targets[type]
                if (target != null) {
                    MacroRow(
                        label = label,
                        unit = unit,
                        target = target,
                        error = viewState.errors[type],
                        onChange = { onTargetChanged(type, it) }
                    )
                }
            }
        }
    }

    if (viewState.showExitDialog) {
        ExitConfirmationDialog(
            canSave = viewState.canSave,
            onSaveTapped = onSaveTapped,
            onDiscardTapped = onUnsavedTargetsDiscardTapped,
            onCancelTapped = onUnsavedTargetsDismissRequested,
        )
    }
}

@Composable
private fun MacroRow(
    label: String,
    unit: String,
    target: TargetUIModel,
    error: FieldErrors?,
    onChange: (TargetUIModel) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }

    Column(
        Modifier
            .fillMaxWidth()
            .animateContentSize()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = !expanded },
            verticalAlignment = Alignment.CenterVertically
        ) {
            val style = if (target.enabled) {
                MaterialTheme.typography.bodyLarge
            } else {
                MaterialTheme.typography.bodyLarge.copy(
                    textDecoration = TextDecoration.LineThrough
                )
            }
            Text(
                modifier = Modifier
                    .weight(1f)
                    .padding(vertical = PaddingDefault),
                text = label,
                style = style
            )
            Text("${target.min ?: "?"}–${target.max ?: "?"}$unit")
            Spacer(modifier = Modifier.width(8.dp))
            Icon(
                imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                contentDescription = if (expanded) "Collapse" else "Expand"
            )
        }

        if (expanded) {
            Spacer(Modifier.height(8.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Switch(
                    checked = target.enabled,
                    onCheckedChange = { onChange(target.copy(enabled = it)) }
                )
                Spacer(Modifier.width(8.dp))
                Text(if (target.enabled) "Enabled" else "Disabled")
            }

            if (target.enabled) {
                val minValue = target.min ?: 0
                val maxValue = target.max ?: target.theoreticalMax
                val layoutDirection = LocalLayoutDirection.current
                val fieldErrors = error ?: FieldErrors()

                RangeSlider(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(
                            horizontal = WindowInsets.systemGestures
                                .asPaddingValues()
                                .calculateStartPadding(layoutDirection),
                        )
                        .padding(vertical = PaddingHalf),
                    value = minValue.toFloat()..maxValue.toFloat(),
                    onValueChange = { range ->
                        onChange(
                            target.copy(
                                min = range.start.toInt(),
                                max = range.endInclusive.toInt()
                            )
                        )
                    },
                    valueRange = 0f..target.theoreticalMax.toFloat(),
                    steps = 0,
                )

                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = target.min?.toString() ?: "",
                        onValueChange = { onChange(target.copy(min = it.toIntOrNull())) },
                        modifier = Modifier
                            .weight(1f)
                            .padding(end = 8.dp),
                        singleLine = true,
                        isError = fieldErrors.minError != null,
                        label = { Text("Min") },
                        supportingText = {
                            when (fieldErrors.minError) {
                                ValidationError.Empty -> Text("Value required", color = MaterialTheme.colorScheme.error)
                                ValidationError.MinGreaterThanMax -> Text("min cannot be higher than max", color = MaterialTheme.colorScheme.error)
                                null -> {}
                            }
                        }
                    )
                    OutlinedTextField(
                        value = target.max?.toString() ?: "",
                        onValueChange = { onChange(target.copy(max = it.toIntOrNull())) },
                        modifier = Modifier
                            .weight(1f)
                            .padding(start = 8.dp),
                        singleLine = true,
                        isError = fieldErrors.maxError != null,
                        label = { Text("Max") },
                        supportingText = {
                            when (fieldErrors.maxError) {
                                ValidationError.Empty -> Text("Value required", color = MaterialTheme.colorScheme.error)
                                ValidationError.MinGreaterThanMax -> Text("min cannot be higher than max", color = MaterialTheme.colorScheme.error)
                                null -> {}
                            }
                        }
                    )
                    Text(unit, Modifier.padding(start = 8.dp))
                }
            }
        }
    }
}


@Composable
private fun ExitConfirmationDialog(
    canSave: Boolean,
    onSaveTapped: () -> Unit,
    onDiscardTapped: () -> Unit,
    onCancelTapped: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onCancelTapped,
        title = { Text("Unsaved changes") },
        text = { Text("You have unsaved changes. Do you want to save them?") },
        confirmButton = {
            if (canSave) {
                TextButton(onClick = onSaveTapped) { Text("Save") }
            }
        },
        dismissButton = {
            Row {
                TextButton(onClick = onDiscardTapped) { Text("Discard") }
                TextButton(onClick = onCancelTapped) { Text("Cancel") }
            }
        }
    )
}

@Preview(name = "Default - Light")
@Preview(name = "Default - Dark", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun TargetsBottomSheetPreview_Default() {
    PreviewContext {
        TargetsBottomSheet(
            viewState = TargetsViewState(
                targets = dummyTargets(),
                canReset = false,
                canSave = false,
                showExitDialog = false,
            ),
            events = emptyFlow(),
            onDismissRequested = {},
            onTargetChanged = { _, _ -> },
            onResetTapped = {},
            onSaveTapped = {},
            onUnsavedTargetsDiscardTapped = {},
            onUnsavedTargetsDismissRequested = {}
        )
    }
}

@Preview(name = "Dirty Valid - Light")
@Preview(name = "Dirty Valid - Dark", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun TargetsBottomSheetPreview_DirtyValid() {
    PreviewContext {
        TargetsBottomSheet(
            viewState = TargetsViewState(
                targets = dummyTargets(calories = 1800 to 2000, protein = 60 to 120),
                canReset = true,
                canSave = true,
                showExitDialog = false,
            ),
            events = emptyFlow(),
            onDismissRequested = {},
            onTargetChanged = { _, _ -> },
            onResetTapped = {},
            onSaveTapped = {},
            onUnsavedTargetsDiscardTapped = {},
            onUnsavedTargetsDismissRequested = {}
        )
    }
}

@Preview(name = "Dirty Invalid - Light")
@Preview(name = "Dirty Invalid - Dark", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun TargetsBottomSheetPreview_DirtyInvalid() {
    PreviewContext {
        TargetsBottomSheet(
            viewState = TargetsViewState(
                targets = dummyTargets(calories = 2200 to 2000), // min > max
                canReset = true,
                canSave = false,
                showExitDialog = false,
            ),
            events = emptyFlow(),
            onDismissRequested = {},
            onTargetChanged = { _, _ -> },
            onResetTapped = {},
            onSaveTapped = {},
            onUnsavedTargetsDiscardTapped = {},
            onUnsavedTargetsDismissRequested = {}
        )
    }
}

@Preview(name = "Exit Dialog Valid - Light")
@Preview(name = "Exit Dialog Valid - Dark", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun ExitDialogPreview_Valid() {
    ViewPreviewContext {
        ExitConfirmationDialog(
            canSave = true,
            onSaveTapped = {},
            onDiscardTapped = {},
            onCancelTapped = {}
        )
    }
}

@Preview(name = "Exit Dialog Invalid - Light")
@Preview(name = "Exit Dialog Invalid - Dark", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun ExitDialogPreview_Invalid() {
    ViewPreviewContext {
        ExitConfirmationDialog(
            canSave = false,
            onSaveTapped = {},
            onDiscardTapped = {},
            onCancelTapped = {}
        )
    }
}

private fun dummyTargets(
    calories: Pair<Int, Int> = 1900 to 2000,
    protein: Pair<Int, Int> = 60 to 105,
    salt: Pair<Int, Int> = 0 to 5,
    fat: Pair<Int, Int> = 55 to 65,
    fibre: Pair<Int, Int> = 30 to 38,
    saturated: Pair<Int, Int> = 0 to 21,
    carbs: Pair<Int, Int> = 150 to 200,
    sugar: Pair<Int, Int> = 0 to 40,
): Map<MacroType, TargetUIModel> =
    mapOf(
        MacroType.CALORIES to TargetUIModel(true, calories.first, calories.second, 4000),
        MacroType.PROTEIN to TargetUIModel(true, protein.first, protein.second, 300),
        MacroType.SALT to TargetUIModel(false, salt.first, salt.second, 20),
        MacroType.FAT to TargetUIModel(true, fat.first, fat.second, 200),
        MacroType.FIBRE to TargetUIModel(true, fibre.first, fibre.second, 100),
        MacroType.SATURATED to TargetUIModel(true, saturated.first, saturated.second, 100),
        MacroType.CARBS to TargetUIModel(true, carbs.first, carbs.second, 600),
        MacroType.SUGAR to TargetUIModel(true, sugar.first, sugar.second, 200),
    )
