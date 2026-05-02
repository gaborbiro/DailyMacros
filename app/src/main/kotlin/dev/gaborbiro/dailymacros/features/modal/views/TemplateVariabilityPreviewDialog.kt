package dev.gaborbiro.dailymacros.features.modal.views

import android.content.res.Configuration
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import dev.gaborbiro.dailymacros.design.PaddingDefault
import dev.gaborbiro.dailymacros.features.common.views.ViewPreviewContext
import dev.gaborbiro.dailymacros.repositories.records.domain.model.TemplateVariabilityPreviewContent
import dev.gaborbiro.dailymacros.repositories.records.domain.model.TemplateVariabilitySlotPreview
import dev.gaborbiro.dailymacros.repositories.records.domain.model.TemplateVariabilityVariantPreview

@Composable
fun TemplateVariabilityPreviewDialog(
    preview: TemplateVariabilityPreviewContent,
    onAddConfirmed: () -> Unit,
    onDismissRequested: () -> Unit,
) {
    val slots = preview.slots
    val selectedVariantKeys = remember(slots) {
        mutableStateMapOf<Int, String>().apply {
            slots.forEachIndexed { index, slot ->
                put(index, slot.variants.first().variantKey)
            }
        }
    }

    Dialog(onDismissRequest = onDismissRequested) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            shadowElevation = 4.dp,
        ) {
            Column(
                modifier = Modifier
                    .padding(PaddingDefault)
                    .fillMaxWidth(),
            ) {
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
                if (slots.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Column(
                        modifier = Modifier
                            .heightIn(max = 320.dp)
                            .verticalScroll(rememberScrollState()),
                    ) {
                        slots.forEachIndexed { index, slot ->
                            SlotVariantDropdown(
                                slot = slot,
                                selectedVariantKey = selectedVariantKeys[index]!!,
                                onVariantKeySelected = { key -> selectedVariantKeys[index] = key },
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                        }
                    }
                }
                Spacer(modifier = Modifier.height(PaddingDefault))
                Button(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = onAddConfirmed,
                    colors = normalButtonColors,
                    shape = RoundedCornerShape(50.dp),
                ) {
                    Text(text = "Add")
                }
                TextButton(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = onDismissRequested,
                ) {
                    Text(text = "Cancel")
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SlotVariantDropdown(
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

@Preview
@Composable
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
private fun TemplateVariabilityPreviewDialogPreview() {
    ViewPreviewContext {
        TemplateVariabilityPreviewDialog(
            preview = TemplateVariabilityPreviewContent(
                bannerText = "Pick a variant per slot (demo — not saved yet).",
                slots = listOf(
                    TemplateVariabilitySlotPreview(
                        archetypeKey = "toast",
                        archetypeDisplayName = "Toast breakfast",
                        slotKey = "spread",
                        role = "Spread",
                        variants = listOf(
                            TemplateVariabilityVariantPreview("butter", "Butter"),
                            TemplateVariabilityVariantPreview("jam", "Jam"),
                        ),
                    ),
                ),
            ),
            onAddConfirmed = {},
            onDismissRequested = {},
        )
    }
}
