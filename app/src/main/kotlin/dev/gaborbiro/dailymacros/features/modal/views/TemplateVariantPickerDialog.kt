package dev.gaborbiro.dailymacros.features.modal.views

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.gaborbiro.dailymacros.design.PaddingDefault
import dev.gaborbiro.dailymacros.features.modal.model.DialogHandle
import dev.gaborbiro.dailymacros.repositories.records.TemplateVariabilityPreviewMapper
import kotlinx.coroutines.flow.Flow

@Composable
internal fun TemplateVariantPickerDialog(
    dialogHandle: DialogHandle.TemplateVariantPickerDialog,
    previewMapper: TemplateVariabilityPreviewMapper,
    errorMessages: Flow<String>,
    onCancel: () -> Unit,
    onConfirm: (Map<String, String>) -> Unit,
) {
    val slots = dialogHandle.slots
    val selection = remember(dialogHandle) {
        mutableStateMapOf<String, String>().apply {
            putAll(dialogHandle.initialSlotSelections)
        }
    }
    val comboKey = previewMapper.combinationKey(slots, selection)
    val exists = comboKey in dialogHandle.existingCombinationKeys
    val actionLabel = if (exists) "Use this" else "Create this"
    val actionEnabled = exists

    ScrollableContentDialog(
        onDismissRequested = onCancel,
        dismissOnOutsideTap = false,
        errorMessages = errorMessages,
        content = {
            Column(modifier = Modifier.padding(horizontal = PaddingDefault)) {
                Text(
                    text = "Pick variants for ${dialogHandle.archetypeDisplayName}",
                    style = MaterialTheme.typography.titleMedium,
                )
                Spacer(modifier = Modifier.height(PaddingDefault))
                slots.forEach { slot ->
                    val key = slot.slotKey
                    TemplateVariabilitySlotVariantDropdown(
                        slot = slot,
                        selectedVariantKey = selection[key] ?: slot.variants.first().variantKey,
                        onVariantKeySelected = { selection[key] = it },
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                }
            }
        },
        buttons = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(PaddingDefault),
                horizontalArrangement = Arrangement.End,
            ) {
                TextButton(onClick = onCancel) {
                    Text("Cancel")
                }
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = { onConfirm(selection.toMap()) },
                    enabled = actionEnabled,
                    shape = RoundedCornerShape(50.dp),
                ) {
                    Text(actionLabel)
                }
            }
        },
    )
}
