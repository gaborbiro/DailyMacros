package dev.gaborbiro.dailymacros.features.modal.views

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.gaborbiro.dailymacros.repositories.records.domain.model.TemplateVariabilityPreviewContent
import dev.gaborbiro.dailymacros.repositories.records.domain.model.TemplateVariabilitySlotPreview

/**
 * Slot dropdowns and banner for mined template variability (same UI as the standalone preview dialog).
 */
@Composable
fun TemplateVariabilitySlotsBlock(
    preview: TemplateVariabilityPreviewContent,
    modifier: Modifier = Modifier,
    showHeader: Boolean = true,
) {
    val slots = preview.slots
    val selectedVariantKeys = remember(slots) {
        mutableStateMapOf<Int, String>().apply {
            slots.forEachIndexed { index, slot ->
                put(index, slot.variants.first().variantKey)
            }
        }
    }

    Column(modifier = modifier.fillMaxWidth()) {
        if (showHeader) {
            Text(
                text = "Variability for this template",
                style = MaterialTheme.typography.titleMedium,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = preview.bannerText,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (slots.isNotEmpty()) {
            if (showHeader) {
                Spacer(modifier = Modifier.height(12.dp))
            }
            // No nested scroll: parent (e.g. record details) is already scrollable.
            slots.forEachIndexed { index, slot ->
                TemplateVariabilitySlotVariantDropdown(
                    slot = slot,
                    selectedVariantKey = selectedVariantKeys[index]!!,
                    onVariantKeySelected = { key -> selectedVariantKeys[index] = key },
                )
                Spacer(modifier = Modifier.height(12.dp))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TemplateVariabilitySlotVariantDropdown(
    slot: TemplateVariabilitySlotPreview,
    selectedVariantKey: String,
    onVariantKeySelected: (String) -> Unit,
) {
    val variants = slot.variants
    val selectedLabel = variants.firstOrNull { it.variantKey == selectedVariantKey }?.variantLabel
        ?: variants.firstOrNull()?.variantLabel
        ?: ""

    var expanded by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "${slot.archetypeDisplayName} — ${slot.role}",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.height(4.dp))
        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = !expanded },
        ) {
            TextField(
                modifier = Modifier
                    .menuAnchor()
                    .fillMaxWidth(),
                readOnly = true,
                value = selectedLabel,
                onValueChange = {},
                trailingIcon = {
                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                },
                colors = ExposedDropdownMenuDefaults.textFieldColors(),
                singleLine = true,
            )
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
            ) {
                variants.forEach { v ->
                    DropdownMenuItem(
                        text = { Text(v.variantLabel) },
                        onClick = {
                            onVariantKeySelected(v.variantKey)
                            expanded = false
                        },
                    )
                }
            }
        }
    }
}
