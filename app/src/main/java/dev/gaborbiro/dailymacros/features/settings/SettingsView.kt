package dev.gaborbiro.dailymacros.features.settings

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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.systemGestures
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import dev.gaborbiro.dailymacros.BuildConfig
import dev.gaborbiro.dailymacros.design.DailyMacrosTheme
import dev.gaborbiro.dailymacros.design.PaddingDefault
import dev.gaborbiro.dailymacros.design.PaddingHalf
import dev.gaborbiro.dailymacros.features.settings.model.FieldErrors
import dev.gaborbiro.dailymacros.features.settings.model.MacroType
import dev.gaborbiro.dailymacros.features.settings.model.SettingsUIModel
import dev.gaborbiro.dailymacros.features.settings.model.SettingsViewState
import dev.gaborbiro.dailymacros.features.settings.model.TargetUIModel
import dev.gaborbiro.dailymacros.features.settings.model.ValidationError

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SettingsView(
    viewState: SettingsViewState,
    onBackClick: () -> Unit,
    onMacroTargetChange: (MacroType, TargetUIModel) -> Unit,
    onReset: () -> Unit,
    onSave: () -> Unit,
    onDiscardExit: () -> Unit,
    onDismissExitDialog: () -> Unit,
) {
    val snackbarHostState = remember { SnackbarHostState() }

    Scaffold(
        contentWindowInsets = WindowInsets.systemBars.union(WindowInsets.ime),
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Default.ArrowBack,
                            contentDescription = "Back Button"
                        )
                    }
                },
                actions = {
                    if (viewState.canReset) {
                        TextButton(onClick = onReset) { Text("Reset") }
                    }
                    TextButton(
                        onClick = onSave,
                        enabled = viewState.canSave
                    ) { Text("Save") }
                }
            )
        },
        bottomBar = {
            Text(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp)
                    .padding(WindowInsets.navigationBars.asPaddingValues()),
                text = "v${BuildConfig.VERSION_NAME}(${BuildConfig.VERSION_CODE})",
                textAlign = TextAlign.Center,
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
                .imePadding()
        ) {
            Text(
                text = "Your daily macro targets",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(vertical = PaddingHalf)
            )

            val ordered = listOf(
                Triple(MacroType.CALORIES, "Calories", "cal"),
                Triple(MacroType.PROTEIN, "Protein", "g"),
                Triple(MacroType.SALT, "Salt", "g"),
                Triple(MacroType.FIBRE, "Fibre", "g"),
                Triple(MacroType.FAT, "Fat", "g"),
                Triple(MacroType.SATURATED, "of which saturated", "g"),
                Triple(MacroType.CARBS, "Carbs", "g"),
                Triple(MacroType.SUGAR, "of which sugar", "g"),
            )

            ordered.forEach { (type, label, unit) ->
                val target = viewState.settings.targets[type]
                if (target != null) {
                    MacroRow(
                        label = label,
                        unit = unit,
                        target = target,
                        error = viewState.errors[type],
                        onChange = { onMacroTargetChange(type, it) }
                    )
                }
            }
        }
    }

    if (viewState.showExitDialog) {
        ExitConfirmationDialog(
            canSave = viewState.canSave,
            onSave = onSave,
            onDiscard = onDiscardExit,
            onDismiss = onDismissExitDialog
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
            Text("${target.min ?: "?"}–${target.max ?: "?"} $unit")
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
    onSave: () -> Unit,
    onDiscard: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Unsaved changes") },
        text = { Text("You have unsaved changes. Do you want to save before leaving?") },
        confirmButton = {
            if (canSave) {
                TextButton(onClick = onSave) { Text("Save") }
            }
        },
        dismissButton = {
            Row {
                TextButton(onClick = onDiscard) { Text("Discard") }
                TextButton(onClick = onDismiss) { Text("Cancel") }
            }
        }
    )
}

@Preview(name = "Default - Light")
@Preview(name = "Default - Dark", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun SettingsViewPreview_Default() {
    DailyMacrosTheme {
        SettingsView(
            viewState = SettingsViewState(
                settings = SettingsUIModel(targets = dummyTargets()),
                canReset = false,
                canSave = false,
                showExitDialog = false
            ),
            onBackClick = {},
            onMacroTargetChange = { _, _ -> },
            onReset = {},
            onSave = {},
            onDiscardExit = {},
            onDismissExitDialog = {}
        )
    }
}

@Preview(name = "Dirty Valid - Light")
@Preview(name = "Dirty Valid - Dark", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun SettingsViewPreview_DirtyValid() {
    DailyMacrosTheme {
        SettingsView(
            viewState = SettingsViewState(
                settings = SettingsUIModel(targets = dummyTargets(calories = 1800 to 2000, protein = 60 to 120)),
                canReset = true,
                canSave = true,
                showExitDialog = false
            ),
            onBackClick = {},
            onMacroTargetChange = { _, _ -> },
            onReset = {},
            onSave = {},
            onDiscardExit = {},
            onDismissExitDialog = {}
        )
    }
}

@Preview(name = "Dirty Invalid - Light")
@Preview(name = "Dirty Invalid - Dark", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun SettingsViewPreview_DirtyInvalid() {
    DailyMacrosTheme {
        SettingsView(
            viewState = SettingsViewState(
                settings = SettingsUIModel(targets = dummyTargets(calories = 2200 to 2000)), // min > max
                canReset = true,
                canSave = false,
                showExitDialog = false
            ),
            onBackClick = {},
            onMacroTargetChange = { _, _ -> },
            onReset = {},
            onSave = {},
            onDiscardExit = {},
            onDismissExitDialog = {}
        )
    }
}

@Preview(name = "Exit Dialog Valid - Light")
@Preview(name = "Exit Dialog Valid - Dark", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun ExitDialogPreview_Valid() {
    DailyMacrosTheme {
        ExitConfirmationDialog(
            canSave = true,
            onSave = {},
            onDiscard = {},
            onDismiss = {}
        )
    }
}

@Preview(name = "Exit Dialog Invalid - Light")
@Preview(name = "Exit Dialog Invalid - Dark", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun ExitDialogPreview_Invalid() {
    DailyMacrosTheme {
        ExitConfirmationDialog(
            canSave = false,
            onSave = {},
            onDiscard = {},
            onDismiss = {}
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
