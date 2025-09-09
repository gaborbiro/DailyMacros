package dev.gaborbiro.dailymacros.features.settings

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.systemGestures
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.LayoutDirection
import dev.gaborbiro.dailymacros.design.DailyMacrosTheme
import dev.gaborbiro.dailymacros.design.PaddingDefault
import dev.gaborbiro.dailymacros.design.PaddingHalf
import dev.gaborbiro.dailymacros.features.settings.model.MacroType
import dev.gaborbiro.dailymacros.features.settings.model.SettingsUIModel
import dev.gaborbiro.dailymacros.features.settings.model.SettingsViewState
import dev.gaborbiro.dailymacros.features.settings.model.TargetUIModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SettingsView(
    viewState: SettingsViewState,
    onBackClick: () -> Unit,
    onMacroTargetChange: (MacroType, TargetUIModel) -> Unit,
    onReset: () -> Unit,
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
                        TextButton(onClick = onReset) {
                            Text("Reset")
                        }
                    }
                }
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(PaddingDefault)
        ) {
            Text(
                text = "Your daily macro targets",
                style = MaterialTheme.typography.titleMedium,
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
                        onChange = { onMacroTargetChange(type, it) }
                    )
                }
            }
        }
    }
}

@Composable
private fun MacroRow(
    label: String,
    unit: String,
    target: TargetUIModel,
    onChange: (TargetUIModel) -> Unit,
) {
    Column(
        Modifier
            .fillMaxWidth()
            .padding(vertical = PaddingHalf)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(label, Modifier.weight(1f))
            Switch(
                checked = target.enabled,
                onCheckedChange = { onChange(target.copy(enabled = it)) }
            )
        }
        if (target.enabled) {
            val minValue = target.min
            val maxValue = target.max

            Column {
                RangeSlider(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(
                            horizontal = WindowInsets.systemGestures
                                .asPaddingValues()
                                .calculateStartPadding(LayoutDirection.Ltr)
                        ),
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
                        .padding(top = PaddingHalf),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = target.min.toString(),
                        onValueChange = { onChange(target.copy(min = it.toIntOrNull() ?: 0)) },
                        modifier = Modifier
                            .weight(1f)
                            .padding(end = PaddingHalf),
                        singleLine = true,
                        label = { Text("Min") }
                    )
                    OutlinedTextField(
                        value = target.max.toString(),
                        onValueChange = {
                            onChange(
                                target.copy(
                                    max = it.toIntOrNull() ?: target.theoreticalMax
                                )
                            )
                        },
                        modifier = Modifier
                            .weight(1f)
                            .padding(start = PaddingHalf),
                        singleLine = true,
                        label = { Text("Max") }
                    )
                    Text(unit, Modifier.padding(start = PaddingHalf))
                }
            }
        }
    }
}

@Preview
@Composable
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
private fun SettingsViewPreview() {
    DailyMacrosTheme {
        SettingsView(
            viewState = SettingsViewState(
                settings = SettingsUIModel(
                    targets = mapOf(
                        MacroType.CALORIES to TargetUIModel(true, 1900, 2000, 4000),
                        MacroType.PROTEIN to TargetUIModel(true, 60, 105, 300),
                        MacroType.SALT to TargetUIModel(false, 0, 5, 20),
                        MacroType.FAT to TargetUIModel(true, 55, 65, 200),
                        MacroType.CARBS to TargetUIModel(true, 150, 200, 600),
                        MacroType.FIBRE to TargetUIModel(true, 30, 38, 100),
                        MacroType.SATURATED to TargetUIModel(true, 0, 21, 100),
                        MacroType.SUGAR to TargetUIModel(true, 0, 40, 200)
                    )
                ),
                canReset = true
            ),
            onBackClick = {},
            onMacroTargetChange = { _, _ -> },
            onReset = {}
        )
    }
}
