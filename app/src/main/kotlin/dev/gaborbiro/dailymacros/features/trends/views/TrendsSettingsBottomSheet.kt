package dev.gaborbiro.dailymacros.features.trends.views

import android.content.res.Configuration
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.ModalBottomSheetProperties
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import dev.gaborbiro.dailymacros.features.common.views.ViewPreviewContext
import dev.gaborbiro.dailymacros.features.trends.model.DayQualifier


@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun TrendsSettingsBottomSheet(
    dayQualifier: DayQualifier,
    qualifiedDaysThreshold: Long,
    onAggregationModeChanged: (DayQualifier) -> Unit,
    onThresholdChanged: (Long) -> Unit,
    onDismissRequested: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true,
        confirmValueChange = { true },
    )
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
        Column(
            modifier = Modifier
                .padding(16.dp)
        ) {
            Text(
                text = "Trends Settings",
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.headlineMedium,
            )
            Text(
                modifier = Modifier
                    .padding(top = 8.dp),
                text = "Which days to consider for Trends calculations?",
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.bodyMedium,
            )

            var expanded by remember { mutableStateOf(false) }
            var selectedMode by remember { mutableStateOf(dayQualifier) }
            val selectedModeLabel = remember(selectedMode) {
                when (selectedMode) {
                    DayQualifier.ALL_CALENDAR_DAYS -> "All days"
                    DayQualifier.ONLY_LOGGED_DAYS -> "Days with anything logged"
                    DayQualifier.ONLY_QUALIFIED_DAYS -> "Days that have enough calories logged"
                }
            }
            var customThresholdValue by remember { mutableStateOf(qualifiedDaysThreshold.toString()) }

            Spacer(
                modifier = Modifier
                    .height(16.dp)
            )

            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = !expanded },
            ) {
                TextField(
                    modifier =
                        Modifier
                            .menuAnchor()
                            .fillMaxWidth(),
                    readOnly = true,
                    value = selectedModeLabel,
                    onValueChange = {},
                    trailingIcon = {
                        ExposedDropdownMenuDefaults.TrailingIcon(
                            expanded = expanded,
                        )
                    },
                    colors =
                        ExposedDropdownMenuDefaults.textFieldColors(),
                )

                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false },
                ) {
                    DropdownMenuItem(
                        text = {
                            Text(text = "All days")
                        },
                        onClick = {
                            selectedMode = DayQualifier.ALL_CALENDAR_DAYS
                            expanded = false
                            onAggregationModeChanged(selectedMode)
                        },
                    )
                    DropdownMenuItem(
                        text = {
                            Text(text = "Days with anything logged")
                        },
                        onClick = {
                            selectedMode = DayQualifier.ONLY_LOGGED_DAYS
                            expanded = false
                            onAggregationModeChanged(selectedMode)
                        },
                    )
                    DropdownMenuItem(
                        text = {
                            Text(text = "Days that have enough calories logged")
                        },
                        onClick = {
                            selectedMode = DayQualifier.ONLY_QUALIFIED_DAYS
                            expanded = false
                            onAggregationModeChanged(selectedMode)
                        },
                    )
                }
            }

            if (selectedMode == DayQualifier.ONLY_QUALIFIED_DAYS) {
                var isError by remember { mutableStateOf(false) }
                OutlinedTextField(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    value = customThresholdValue,
                    onValueChange = { input ->
                        customThresholdValue = input.filter { it.isDigit() }
                        val validation = runCatching {
                            onThresholdChanged(customThresholdValue.toLong())
                        }
                        isError = validation.isFailure
                    },
                    isError = isError,
                    supportingText = {
                        if (isError) {
                            Text(text = "Invalid value")
                        }
                    },
                    label = {
                        Text(text = "Calorie threshold (for ex: 800)")
                    },
                    keyboardOptions =
                        KeyboardOptions(
                            keyboardType = KeyboardType.Number,
                        ),
                    singleLine = true,
                )
            }

            Spacer(
                modifier = Modifier
                    .height(16.dp)
            )
        }
    }
}

@Preview
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun TrendsSettingsBottomSheetQualifiedPreview() {
    ViewPreviewContext {
        TrendsSettingsBottomSheet(
            dayQualifier = DayQualifier.ONLY_QUALIFIED_DAYS,
            qualifiedDaysThreshold = 800,
            onDismissRequested = {},
            onAggregationModeChanged = {},
            onThresholdChanged = {},
        )
    }
}

@Preview
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun TrendsSettingsBottomSheetLoggedPreview() {
    ViewPreviewContext {
        TrendsSettingsBottomSheet(
            dayQualifier = DayQualifier.ONLY_LOGGED_DAYS,
            qualifiedDaysThreshold = 800,
            onDismissRequested = {},
            onAggregationModeChanged = {},
            onThresholdChanged = {},
        )
    }
}