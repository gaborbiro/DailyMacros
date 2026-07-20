package dev.gaborbiro.dailymacros.features.settings.views

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DateRangePicker
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDateRangePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dev.gaborbiro.dailymacros.features.settings.R
import dev.gaborbiro.dailymacros.features.settings.export.pdf.PdfRangeSelection
import dev.gaborbiro.dailymacros.repositories.settings.domain.model.DateRangePreset
import dev.gaborbiro.dailymacros.repositories.settings.domain.model.PdfExportOptions
import dev.gaborbiro.dailymacros.repositories.settings.domain.model.PdfPhotoMode
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
internal fun PdfExportDialog(
    initialOptions: PdfExportOptions,
    onDismiss: () -> Unit,
    onExport: (PdfRangeSelection, PdfExportOptions) -> Unit,
) {
    var selectedPreset by remember { mutableStateOf<DateRangePreset?>(initialOptions.rangePreset) }
    var customRange by remember { mutableStateOf<Pair<LocalDate, LocalDate>?>(null) }
    var showRangePicker by remember { mutableStateOf(false) }

    var dailyTotals by remember { mutableStateOf(initialOptions.dailyTotals) }
    var mealMacros by remember { mutableStateOf(initialOptions.mealMacros) }
    var photos by remember { mutableStateOf(initialOptions.photos) }
    var description by remember { mutableStateOf(initialOptions.description) }
    var components by remember { mutableStateOf(initialOptions.components) }

    if (showRangePicker) {
        val pickerState = rememberDateRangePickerState()
        DatePickerDialog(
            onDismissRequest = { showRangePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    val start = pickerState.selectedStartDateMillis
                    val end = pickerState.selectedEndDateMillis
                    if (start != null && end != null) {
                        customRange = millisToLocalDate(start) to millisToLocalDate(end)
                        selectedPreset = null
                    }
                    showRangePicker = false
                }) { Text(stringResource(R.string.pdf_export_range_picker_confirm)) }
            },
            dismissButton = {
                TextButton(onClick = { showRangePicker = false }) {
                    Text(stringResource(R.string.pdf_export_cancel))
                }
            },
        ) {
            DateRangePicker(state = pickerState, modifier = Modifier.height(480.dp))
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.pdf_export_dialog_title)) },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                SectionLabel(stringResource(R.string.pdf_export_range_section))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    presetLabels.forEach { (preset, labelRes) ->
                        FilterChip(
                            selected = selectedPreset == preset,
                            onClick = { selectedPreset = preset },
                            label = { Text(stringResource(labelRes)) },
                        )
                    }
                    FilterChip(
                        selected = selectedPreset == null && customRange != null,
                        onClick = { showRangePicker = true },
                        label = { Text(customChipLabel(customRange)) },
                    )
                }

                Spacer(Modifier.height(12.dp))
                SectionLabel(stringResource(R.string.pdf_export_options_section))

                SwitchRow(
                    label = stringResource(R.string.pdf_export_daily_totals),
                    checked = dailyTotals,
                    onCheckedChange = { dailyTotals = it },
                )
                SwitchRow(
                    label = stringResource(R.string.pdf_export_meal_macros),
                    checked = mealMacros,
                    onCheckedChange = { mealMacros = it },
                )

                SwitchRow(
                    label = stringResource(R.string.pdf_export_description),
                    checked = description,
                    onCheckedChange = { description = it },
                )
                SwitchRow(
                    label = stringResource(R.string.pdf_export_components),
                    checked = components,
                    onCheckedChange = { components = it },
                )

                ChoiceRow(label = stringResource(R.string.pdf_export_photos)) {
                    photoLabels.forEach { (mode, labelRes) ->
                        FilterChip(
                            selected = photos == mode,
                            onClick = { photos = mode },
                            label = { Text(stringResource(labelRes)) },
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val selection = customRange
                    ?.takeIf { selectedPreset == null }
                    ?.let { PdfRangeSelection.Custom(it.first, it.second) }
                    ?: PdfRangeSelection.Preset(selectedPreset ?: DateRangePreset.LAST_7_DAYS)
                onExport(
                    selection,
                    PdfExportOptions(
                        dailyTotals = dailyTotals,
                        photos = photos,
                        mealMacros = mealMacros,
                        description = description,
                        components = components,
                        // Remember the preset for next time; keep the previous one when a custom range is used.
                        rangePreset = selectedPreset ?: initialOptions.rangePreset,
                    ),
                )
            }) { Text(stringResource(R.string.pdf_export_action)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.pdf_export_cancel)) }
        },
    )
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(bottom = 6.dp),
    )
}

@Composable
private fun SwitchRow(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text = label, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ChoiceRow(label: String, content: @Composable () -> Unit) {
    Column(modifier = Modifier.padding(top = 8.dp)) {
        Text(text = label, modifier = Modifier.padding(bottom = 4.dp))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) { content() }
    }
}

private val presetLabels = listOf(
    DateRangePreset.TODAY to R.string.pdf_export_preset_today,
    DateRangePreset.THIS_WEEK to R.string.pdf_export_preset_this_week,
    DateRangePreset.LAST_7_DAYS to R.string.pdf_export_preset_last_7_days,
    DateRangePreset.LAST_WEEK to R.string.pdf_export_preset_last_week,
    DateRangePreset.THIS_MONTH to R.string.pdf_export_preset_this_month,
    DateRangePreset.LAST_30_DAYS to R.string.pdf_export_preset_last_30_days,
    DateRangePreset.LAST_MONTH to R.string.pdf_export_preset_last_month,
)

private val photoLabels = listOf(
    PdfPhotoMode.ALL to R.string.pdf_export_photos_all,
    PdfPhotoMode.TITULAR to R.string.pdf_export_photos_titular,
    PdfPhotoMode.NONE to R.string.pdf_export_photos_none,
)

private val chipDateFormatter = DateTimeFormatter.ofLocalizedDate(FormatStyle.SHORT)

@Composable
private fun customChipLabel(range: Pair<LocalDate, LocalDate>?): String =
    if (range == null) {
        stringResource(R.string.pdf_export_preset_custom)
    } else {
        "${range.first.format(chipDateFormatter)} – ${range.second.format(chipDateFormatter)}"
    }

private fun millisToLocalDate(millis: Long): LocalDate =
    Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate()
