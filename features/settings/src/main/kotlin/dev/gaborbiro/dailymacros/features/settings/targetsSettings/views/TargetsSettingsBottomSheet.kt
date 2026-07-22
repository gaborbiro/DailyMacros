package dev.gaborbiro.dailymacros.features.settings.targetsSettings.views

import android.content.res.Configuration
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemGestures
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RangeSlider
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import dev.gaborbiro.dailymacros.design.PaddingDefault
import dev.gaborbiro.dailymacros.design.PaddingHalf
import dev.gaborbiro.dailymacros.features.common.utils.verticalScrollWithBar
import dev.gaborbiro.dailymacros.features.common.views.NutrientDisplayLine
import dev.gaborbiro.dailymacros.features.settings.R
import dev.gaborbiro.dailymacros.features.settings.targetsSettings.model.FieldErrors
import dev.gaborbiro.dailymacros.features.settings.targetsSettings.model.MacroType
import dev.gaborbiro.dailymacros.features.settings.targetsSettings.model.TargetUiModel
import dev.gaborbiro.dailymacros.features.settings.targetsSettings.model.TargetsSettingsUiState
import dev.gaborbiro.dailymacros.features.settings.targetsSettings.model.ValidationError
import dev.gaborbiro.dailymacros.features.settings.views.SettingsPreviewContext
import dev.gaborbiro.dailymacros.features.settings.views.SettingsViewPreviewContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun TargetsSettingsBottomSheet(
    viewState: TargetsSettingsUiState,
    onDismissRequested: () -> Unit,
    onTargetChanged: (MacroType, TargetUiModel) -> Unit,
    onResetTapped: () -> Unit,
    onSaveTapped: () -> Unit,
    onUnsavedTargetsDiscardTapped: () -> Unit,
    onUnsavedTargetsDismissRequested: () -> Unit,
) {
    Dialog(
        onDismissRequest = onDismissRequested,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.surface,
        ) {
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = { Text(stringResource(R.string.settings_content_targets_title)) },
                        navigationIcon = {
                            IconButton(onClick = onDismissRequested) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = stringResource(R.string.settings_content_targets_back_cd),
                                )
                            }
                        },
                        actions = {
                            TextButton(
                                onClick = onResetTapped,
                                enabled = viewState.canReset,
                            ) {
                                Text(stringResource(R.string.settings_content_targets_reset))
                            }
                            TextButton(
                                onClick = onSaveTapped,
                                enabled = viewState.canSave,
                            ) {
                                Text(stringResource(R.string.settings_content_targets_save))
                            }
                        },
                    )
                },
            ) { paddingValues ->
                Column(
                    modifier = Modifier
                        .padding(paddingValues)
                        .verticalScrollWithBar(autoFade = false)
                        .padding(16.dp)
                        .imePadding(),
                ) {
                    Text(
                        text = stringResource(R.string.settings_content_targets_hint),
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(bottom = 16.dp),
                    )
                    val ordered = listOf(
                        Triple(MacroType.CALORIES, "Calories ", NutrientDisplayLine.Calories.unit),
                        Triple(MacroType.PROTEIN, "Protein ", NutrientDisplayLine.Protein.unit),
                        Triple(MacroType.SALT, "Salt ", NutrientDisplayLine.Salt.unit),
                        Triple(MacroType.FIBRE, "Fibre ", NutrientDisplayLine.Fibre.unit),
                        Triple(MacroType.FAT, "Fat ", NutrientDisplayLine.Fat.unit),
                        Triple(MacroType.SATURATED, "of which saturated ", NutrientDisplayLine.OfWhichSaturated.unit),
                        Triple(MacroType.CARBS, "Carbs ", NutrientDisplayLine.Carb.unit),
                        Triple(MacroType.SUGAR, "of which sugar ", NutrientDisplayLine.OfWhichSugar.unit),
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
    target: TargetUiModel,
    error: FieldErrors?,
    onChange: (TargetUiModel) -> Unit,
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
                .then(
                    if (target.enabled) Modifier.clickable { expanded = !expanded }
                    else Modifier
                ),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (target.enabled) {
                Icon(
                    imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = if (expanded) stringResource(R.string.settings_content_targets_collapse_cd) else stringResource(R.string.settings_content_targets_expand_cd),
                )
            }
            Text(
                modifier = Modifier
                    .padding(vertical = PaddingDefault)
                    .weight(1f),
                text = if (target.enabled) "$label${target.min ?: "?"}–${target.max ?: "?"}$unit" else label,
                style = MaterialTheme.typography.bodyLarge,
            )
            Switch(
                checked = target.enabled,
                onCheckedChange = { enabled ->
                    onChange(target.copy(enabled = enabled))
                    expanded = enabled
                },
            )
        }

        if (expanded && target.enabled) {
            Spacer(Modifier.height(8.dp))
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
                        .fillMaxWidth(0.48f)
                        .padding(end = 8.dp),
                    singleLine = true,
                    isError = fieldErrors.minError != null,
                    label = { Text(stringResource(R.string.settings_content_targets_min_label)) },
                    supportingText = {
                        when (fieldErrors.minError) {
                            ValidationError.Empty -> Text(stringResource(R.string.settings_content_targets_error_required), color = MaterialTheme.colorScheme.error)
                            ValidationError.MinGreaterThanMax -> Text(stringResource(R.string.settings_content_targets_error_min_max), color = MaterialTheme.colorScheme.error)
                            null -> {}
                        }
                    }
                )
                OutlinedTextField(
                    value = target.max?.toString() ?: "",
                    onValueChange = { onChange(target.copy(max = it.toIntOrNull())) },
                    modifier = Modifier
                        .fillMaxWidth(0.48f)
                        .padding(start = 8.dp),
                    singleLine = true,
                    isError = fieldErrors.maxError != null,
                    label = { Text(stringResource(R.string.settings_content_targets_max_label)) },
                    supportingText = {
                        when (fieldErrors.maxError) {
                            ValidationError.Empty -> Text(stringResource(R.string.settings_content_targets_error_required), color = MaterialTheme.colorScheme.error)
                            ValidationError.MinGreaterThanMax -> Text(stringResource(R.string.settings_content_targets_error_min_max), color = MaterialTheme.colorScheme.error)
                            null -> {}
                        }
                    }
                )
                Text(unit, Modifier.padding(start = 8.dp))
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
        title = { Text(stringResource(R.string.settings_dialog_targets_unsaved_title)) },
        text = { Text(stringResource(R.string.settings_dialog_targets_unsaved_message)) },
        confirmButton = {
            if (canSave) {
                TextButton(onClick = onSaveTapped) { Text(stringResource(R.string.settings_content_targets_save)) }
            }
        },
        dismissButton = {
            Row {
                TextButton(onClick = onDiscardTapped) { Text(stringResource(R.string.settings_dialog_targets_discard)) }
                TextButton(onClick = onCancelTapped) { Text(stringResource(R.string.settings_dialog_cancel)) }
            }
        },
    )
}

@Preview
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun TargetsSettingsBottomSheetPreview_Default() {
    SettingsPreviewContext {
        TargetsSettingsBottomSheet(
            viewState = TargetsSettingsUiState(
                targets = dummyTargets(),
                canReset = false,
                canSave = false,
                showExitDialog = false,
            ),
            onDismissRequested = {},
            onTargetChanged = { _, _ -> },
            onResetTapped = {},
            onSaveTapped = {},
            onUnsavedTargetsDiscardTapped = {},
            onUnsavedTargetsDismissRequested = {},
        )
    }
}

@Preview
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun TargetsSettingsBottomSheetPreview_DirtyValid() {
    SettingsPreviewContext {
        TargetsSettingsBottomSheet(
            viewState = TargetsSettingsUiState(
                targets = dummyTargets(calories = 1800 to 2000, protein = 60 to 120),
                canReset = true,
                canSave = true,
                showExitDialog = false,
            ),
            onDismissRequested = {},
            onTargetChanged = { _, _ -> },
            onResetTapped = {},
            onSaveTapped = {},
            onUnsavedTargetsDiscardTapped = {},
            onUnsavedTargetsDismissRequested = {},
        )
    }
}

@Preview
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun TargetsSettingsBottomSheetPreview_DirtyInvalid() {
    SettingsPreviewContext {
        TargetsSettingsBottomSheet(
            viewState = TargetsSettingsUiState(
                targets = dummyTargets(calories = 2200 to 2000),
                canReset = true,
                canSave = false,
                showExitDialog = false,
            ),
            onDismissRequested = {},
            onTargetChanged = { _, _ -> },
            onResetTapped = {},
            onSaveTapped = {},
            onUnsavedTargetsDiscardTapped = {},
            onUnsavedTargetsDismissRequested = {},
        )
    }
}

@Preview
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun ExitDialogPreview_Valid() {
    SettingsViewPreviewContext {
        ExitConfirmationDialog(
            canSave = true,
            onSaveTapped = {},
            onDiscardTapped = {},
            onCancelTapped = {},
        )
    }
}

@Preview
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun ExitDialogPreview_Invalid() {
    SettingsViewPreviewContext {
        ExitConfirmationDialog(
            canSave = false,
            onSaveTapped = {},
            onDiscardTapped = {},
            onCancelTapped = {},
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
): Map<MacroType, TargetUiModel> =
    mapOf(
        MacroType.CALORIES to TargetUiModel(true, calories.first, calories.second, 4000),
        MacroType.PROTEIN to TargetUiModel(true, protein.first, protein.second, 300),
        MacroType.SALT to TargetUiModel(false, salt.first, salt.second, 20),
        MacroType.FAT to TargetUiModel(true, fat.first, fat.second, 200),
        MacroType.FIBRE to TargetUiModel(true, fibre.first, fibre.second, 100),
        MacroType.SATURATED to TargetUiModel(true, saturated.first, saturated.second, 100),
        MacroType.CARBS to TargetUiModel(true, carbs.first, carbs.second, 600),
        MacroType.SUGAR to TargetUiModel(true, sugar.first, sugar.second, 200),
    )
