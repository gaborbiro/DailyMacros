package dev.gaborbiro.dailymacros.features.modal.views

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.ModalBottomSheetProperties
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import dev.gaborbiro.dailymacros.features.common.views.ViewPreviewContext
import dev.gaborbiro.dailymacros.features.modal.R
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import kotlin.math.abs

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun EditTimestampBottomSheet(
    initial: ZonedDateTime,
    onConfirm: (ZonedDateTime) -> Unit,
    onDismissRequested: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var selected by remember { mutableStateOf(initial) }
    var showDatePicker by remember { mutableStateOf(false) }
    val today = remember { LocalDate.now(initial.zone) }
    val yesterday = remember(today) { today.minusDays(1) }

    ModalBottomSheet(
        containerColor = MaterialTheme.colorScheme.surface,
        sheetState = sheetState,
        onDismissRequest = onDismissRequested,
        properties = ModalBottomSheetProperties(shouldDismissOnBackPress = true),
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .padding(bottom = 16.dp),
        ) {
            Text(
                modifier = Modifier.fillMaxWidth(),
                text = stringResource(R.string.edit_timestamp_sheet_title),
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.headlineSmall,
                textAlign = TextAlign.Center,
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
            ) {
                FilterChip(
                    selected = selected.toLocalDate() == today,
                    onClick = { selected = selected.withDate(today) },
                    label = { Text(stringResource(R.string.edit_timestamp_day_today)) },
                )
                FilterChip(
                    selected = selected.toLocalDate() == yesterday,
                    onClick = { selected = selected.withDate(yesterday) },
                    label = { Text(stringResource(R.string.edit_timestamp_day_yesterday)) },
                )
                FilterChip(
                    selected = selected.toLocalDate() != today && selected.toLocalDate() != yesterday,
                    onClick = { showDatePicker = true },
                    label = {
                        Text(
                            if (selected.toLocalDate() != today && selected.toLocalDate() != yesterday) {
                                selected.toLocalDate().format(dayChipDateFormatter)
                            } else {
                                stringResource(R.string.edit_timestamp_day_pick_date)
                            }
                        )
                    },
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TimeWheel(
                    values = 0..23,
                    selected = selected.hour,
                    onValueChange = { selected = selected.withHour(it) },
                )
                Text(
                    text = ":",
                    style = MaterialTheme.typography.headlineMedium,
                    modifier = Modifier.padding(horizontal = 4.dp),
                )
                TimeWheel(
                    values = 0..59,
                    selected = selected.minute,
                    onValueChange = { selected = selected.withMinute(it) },
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Row(modifier = Modifier.fillMaxWidth()) {
                TextButton(
                    modifier = Modifier.weight(1f),
                    onClick = onDismissRequested,
                ) {
                    Text(stringResource(R.string.meal_details_action_cancel))
                }
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    modifier = Modifier.weight(1f),
                    onClick = { onConfirm(selected) },
                ) {
                    Text(stringResource(R.string.edit_timestamp_confirm))
                }
            }
        }
    }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = selected.toLocalDate()
                .atStartOfDay(ZoneOffset.UTC)
                .toInstant()
                .toEpochMilli(),
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        selected = selected.withDate(millisToLocalDate(millis))
                    }
                    showDatePicker = false
                }) { Text(stringResource(R.string.edit_timestamp_date_picker_confirm)) }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text(stringResource(R.string.meal_details_action_cancel))
                }
            },
        ) {
            DatePicker(state = datePickerState)
        }
    }
}

/**
 * iOS-style snapping wheel: a fixed-height [LazyColumn] where the item nearest the viewport
 * center is the selected value, with outer rows faded/scaled down to read as a rolling drum.
 */
@Composable
private fun TimeWheel(
    values: IntRange,
    selected: Int,
    onValueChange: (Int) -> Unit,
) {
    val itemHeight = 40.dp
    val visibleRows = 5
    val valuesList = remember(values) { values.toList() }
    val listState = rememberLazyListState()
    val flingBehavior = rememberSnapFlingBehavior(listState)

    val centerIndex by remember {
        derivedStateOf {
            val info = listState.layoutInfo
            if (info.visibleItemsInfo.isEmpty()) return@derivedStateOf 0
            val viewportCenter = (info.viewportStartOffset + info.viewportEndOffset) / 2
            info.visibleItemsInfo.minBy { abs((it.offset + it.size / 2) - viewportCenter) }.index
        }
    }

    LaunchedEffect(Unit) {
        val index = valuesList.indexOf(selected).coerceAtLeast(0)
        listState.scrollToItem(index)
    }

    LaunchedEffect(centerIndex) {
        valuesList.getOrNull(centerIndex)?.let { value ->
            if (value != selected) onValueChange(value)
        }
    }

    LaunchedEffect(selected) {
        val targetIndex = valuesList.indexOf(selected)
        if (targetIndex >= 0 && targetIndex != centerIndex) {
            listState.animateScrollToItem(targetIndex)
        }
    }

    Box(
        modifier = Modifier
            .height(itemHeight * visibleRows)
            .width(64.dp),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(itemHeight)
                .background(
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                    shape = RoundedCornerShape(10.dp),
                ),
        )

        LazyColumn(
            state = listState,
            flingBehavior = flingBehavior,
            contentPadding = PaddingValues(vertical = itemHeight * (visibleRows / 2)),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            itemsIndexed(valuesList) { index, value ->
                val distance = abs(index - centerIndex)
                val rowAlpha = when (distance) {
                    0 -> 1f
                    1 -> 0.5f
                    else -> 0.25f
                }
                val rowScale = if (distance == 0) 1f else 0.8f
                Box(
                    modifier = Modifier
                        .height(itemHeight)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "%02d".format(value),
                        style = MaterialTheme.typography.headlineSmall,
                        modifier = Modifier.graphicsLayer {
                            alpha = rowAlpha
                            scaleX = rowScale
                            scaleY = rowScale
                        },
                    )
                }
            }
        }
    }
}

private val dayChipDateFormatter = DateTimeFormatter.ofPattern("dd MMM")

private fun ZonedDateTime.withDate(date: LocalDate): ZonedDateTime =
    LocalDateTime.of(date, this.toLocalTime()).atZone(this.zone)

private fun millisToLocalDate(millis: Long): LocalDate =
    java.time.Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate()

@Preview
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun EditTimestampBottomSheetPreview() {
    ViewPreviewContext {
        EditTimestampBottomSheet(
            initial = ZonedDateTime.now(ZoneId.systemDefault()),
            onConfirm = {},
            onDismissRequested = {},
        )
    }
}
