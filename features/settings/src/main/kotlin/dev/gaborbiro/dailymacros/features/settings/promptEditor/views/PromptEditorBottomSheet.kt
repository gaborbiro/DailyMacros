package dev.gaborbiro.dailymacros.features.settings.promptEditor.views

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import dev.gaborbiro.dailymacros.features.settings.promptEditor.model.PromptEditorUiState
import dev.gaborbiro.dailymacros.features.settings.util.verticalScrollWithBar
import dev.gaborbiro.dailymacros.repositories.chatgpt.domain.model.PromptSegment
import dev.gaborbiro.dailymacros.repositories.settings.domain.model.PromptVersion
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun PromptEditorView(
    viewState: PromptEditorUiState,
    onDismissRequested: () -> Unit,
    onValueChanged: (String, String) -> Unit,
    onResetTab: (List<String>) -> Unit,
    onSaveTapped: () -> Unit,
    onVersionSelected: (Int) -> Unit,
    onExitDialogSaveTapped: () -> Unit,
    onExitDialogDiscardTapped: () -> Unit,
    onExitDialogDismissed: () -> Unit,
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Recognition", "Analysis")
    val currentSegments = if (selectedTab == 0) viewState.recognitionSegments else viewState.analysisSegments
    val editableIds = currentSegments.filterIsInstance<PromptSegment.Editable>().map { it.id }

    Dialog(
        onDismissRequest = onDismissRequested,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background,
        ) {
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = { Text("AI Prompts") },
                        navigationIcon = {
                            IconButton(onClick = onDismissRequested) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Back",
                                )
                            }
                        },
                        actions = {
                            IconButton(
                                onClick = { onResetTab(editableIds) },
                                enabled = editableIds.any { viewState.currentValues.containsKey(it) },
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = "Reset tab to defaults",
                                )
                            }
                            TextButton(
                                onClick = onSaveTapped,
                                enabled = viewState.canSave,
                            ) {
                                Text("Save")
                            }
                        },
                    )
                },
            ) { paddingValues ->
                Column(
                    modifier = Modifier
                        .padding(paddingValues)
                        .fillMaxSize(),
                ) {
                    Text(
                        text = "Changes take effect on the next AI query.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                    )

                    VersionPicker(
                        versions = viewState.versions,
                        selectedIndex = viewState.selectedVersionIndex,
                        onVersionSelected = onVersionSelected,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                    )

                    PrimaryTabRow(selectedTabIndex = selectedTab) {
                        tabs.forEachIndexed { index, title ->
                            Tab(
                                selected = selectedTab == index,
                                onClick = { selectedTab = index },
                                text = { Text(title) },
                            )
                        }
                    }

                    Column(
                        modifier = Modifier
                            .verticalScrollWithBar(autoFade = false)
                            .padding(16.dp)
                            .imePadding(),
                    ) {
                        currentSegments.forEach { segment ->
                            when (segment) {
                                is PromptSegment.Locked -> Unit
                                is PromptSegment.Editable -> {
                                    val currentText = viewState.currentValues[segment.id] ?: segment.defaultText
                                    OutlinedTextField(
                                        value = currentText,
                                        onValueChange = { onValueChanged(segment.id, it) },
                                        label = { Text(segment.label) },
                                        placeholder = if (segment.hint.isNotBlank()) {
                                            { Text(segment.hint, style = MaterialTheme.typography.bodySmall) }
                                        } else null,
                                        modifier = Modifier.fillMaxWidth(),
                                        minLines = currentText.lines().size.coerceIn(3, 20),
                                    )
                                    Spacer(Modifier.height(16.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (viewState.showExitDialog) {
        AlertDialog(
            onDismissRequest = onExitDialogDismissed,
            title = { Text("Unsaved changes") },
            text = { Text("You have unsaved prompt changes. Save or discard them?") },
            confirmButton = {
                if (viewState.canSave) {
                    TextButton(onClick = onExitDialogSaveTapped) { Text("Save") }
                }
            },
            dismissButton = {
                Row {
                    TextButton(onClick = onExitDialogDiscardTapped) { Text("Discard") }
                    TextButton(onClick = onExitDialogDismissed) { Text("Cancel") }
                }
            },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun VersionPicker(
    versions: List<PromptVersion>,
    selectedIndex: Int,
    onVersionSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }
    val dateFormatter = remember { SimpleDateFormat("MMM d, yyyy", Locale.US) }

    fun PromptVersion.label() = "v${version} · ${dateFormatter.format(Date(createdAt))}"

    val displayText = when {
        versions.isEmpty() -> "No saved versions yet"
        selectedIndex >= 0 -> versions[selectedIndex].label()
        else -> "Select a version"
    }

    ExposedDropdownMenuBox(
        expanded = expanded && versions.isNotEmpty(),
        onExpandedChange = { if (versions.isNotEmpty()) expanded = !expanded },
        modifier = modifier,
    ) {
        OutlinedTextField(
            value = displayText,
            onValueChange = {},
            readOnly = true,
            label = { Text("Version") },
            trailingIcon = {
                if (versions.isNotEmpty()) {
                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(MenuAnchorType.PrimaryNotEditable),
            enabled = versions.isNotEmpty(),
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            versions.forEachIndexed { index, version ->
                DropdownMenuItem(
                    text = { Text(version.label()) },
                    onClick = {
                        expanded = false
                        onVersionSelected(index)
                    },
                )
            }
        }
    }
}
