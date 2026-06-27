package dev.gaborbiro.dailymacros.features.settings.promptEditor.views

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.layout
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.Constraints
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
    onSaveTapped: () -> Unit,
    onVersionSelected: (Int) -> Unit,
    onDeleteVersion: (Int) -> Unit,
    onExitDialogSaveTapped: () -> Unit,
    onExitDialogDiscardTapped: () -> Unit,
    onExitDialogDismissed: () -> Unit,
    onApiKeyDraftChanged: (String) -> Unit,
    onUnlockTapped: () -> Unit,
    onClearApiKeyTapped: () -> Unit,
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Recognition", "Analysis", "Week on Week", "Ongoing Week")
    val currentSegments = when (selectedTab) {
        0 -> viewState.recognitionSegments
        1 -> viewState.analysisSegments
        2 -> viewState.insightsSegments
        else -> viewState.ongoingInsightsSegments
    }

    val recognitionScrollState = rememberScrollState()
    val analysisScrollState = rememberScrollState()
    val insightsScrollState = rememberScrollState()
    val ongoingInsightsScrollState = rememberScrollState()
    val activeScrollState: ScrollState = when (selectedTab) {
        0 -> recognitionScrollState
        1 -> analysisScrollState
        2 -> insightsScrollState
        else -> ongoingInsightsScrollState
    }

    var hiddenPx by remember { mutableFloatStateOf(0f) }
    var collapsingMaxPx by remember { mutableFloatStateOf(0f) }
    val nestedScrollConnection = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                val max = collapsingMaxPx
                if (max <= 0f || available.y >= 0f) return Offset.Zero
                val prev = hiddenPx
                hiddenPx = (prev - available.y).coerceIn(0f, max)
                return Offset(0f, prev - hiddenPx)
            }

            override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset {
                if (available.y <= 0f) return Offset.Zero
                val prev = hiddenPx
                hiddenPx = (prev - available.y).coerceAtLeast(0f)
                return Offset(0f, prev - hiddenPx)
            }
        }
    }

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
                        title = { Text("AI Customisation") },
                        navigationIcon = {
                            IconButton(onClick = onDismissRequested) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Back",
                                )
                            }
                        },
                    )
                },
            ) { paddingValues ->
                Column(
                    modifier = Modifier
                        .padding(paddingValues)
                        .fillMaxSize()
                        .nestedScroll(nestedScrollConnection),
                ) {
                    Column(
                        modifier = Modifier
                            .clipToBounds()
                            .layout { measurable, constraints ->
                                val placeable = measurable.measure(
                                    constraints.copy(maxHeight = Constraints.Infinity)
                                )
                                collapsingMaxPx = placeable.height.toFloat()
                                val clampedH = (placeable.height - hiddenPx.toInt()).coerceAtLeast(0)
                                layout(placeable.width, clampedH) {
                                    placeable.placeRelative(0, 0)
                                }
                            },
                    ) {
                        Spacer(Modifier.height(8.dp))
                        ApiKeyRow(
                            viewState = viewState,
                            onApiKeyDraftChanged = onApiKeyDraftChanged,
                            onUnlockTapped = onUnlockTapped,
                            onClearApiKeyTapped = onClearApiKeyTapped,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 4.dp),
                        )
                        if (viewState.promptsEnabled) {
                            Text(
                                text = "Changes take effect on the next AI query.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                            )
                        }
                        VersionPicker(
                            versions = viewState.versions,
                            selectedIndex = viewState.selectedVersionIndex,
                            enabled = viewState.promptsEnabled,
                            onVersionSelected = onVersionSelected,
                            onDeleteVersion = onDeleteVersion,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                        )
                    }

                    ScrollableTabRow(selectedTabIndex = selectedTab) {
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
                            .weight(1f)
                            .verticalScrollWithBar(scrollState = activeScrollState, autoFade = false)
                            .padding(16.dp)
                            .imePadding(),
                    ) {
                        currentSegments.forEach { segment ->
                            when (segment) {
                                is PromptSegment.Locked -> {
                                    Text(
                                        text = segment.text,
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontFamily = FontFamily.Monospace,
                                        ),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 12.dp),
                                    )
                                }
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
                                        minLines = if (segment.singleLine) 1 else currentText.lines().size.coerceIn(3, 20),
                                        maxLines = if (segment.singleLine) 1 else Int.MAX_VALUE,
                                        singleLine = segment.singleLine,
                                        enabled = viewState.promptsEnabled,
                                    )
                                    Spacer(Modifier.height(16.dp))
                                }
                            }
                        }
                    }

                    androidx.compose.material3.Surface(
                        shadowElevation = 8.dp,
                        color = MaterialTheme.colorScheme.background,
                    ) {
                        Button(
                            onClick = onSaveTapped,
                            enabled = viewState.canSave,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                        ) {
                            Text("Save")
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

@Composable
private fun ApiKeyRow(
    viewState: PromptEditorUiState,
    onApiKeyDraftChanged: (String) -> Unit,
    onUnlockTapped: () -> Unit,
    onClearApiKeyTapped: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        OutlinedTextField(
            value = if (viewState.isApiKeyOverridden) viewState.storedApiKeyOverride!! else viewState.apiKeyDraft,
            onValueChange = { if (!viewState.isApiKeyOverridden) onApiKeyDraftChanged(it) },
            label = { Text("ChatGPT API key") },
            readOnly = viewState.isApiKeyOverridden,
            singleLine = true,
            modifier = Modifier.weight(1f),
        )
        Spacer(Modifier.width(8.dp))
        if (viewState.isApiKeyOverridden) {
            OutlinedButton(onClick = onClearApiKeyTapped) {
                Text("Clear")
            }
        } else {
            Button(
                onClick = onUnlockTapped,
                enabled = !viewState.isUnlocking && viewState.apiKeyDraft.isNotBlank(),
            ) {
                if (viewState.isUnlocking) {
                    CircularProgressIndicator(
                        modifier = Modifier
                            .height(18.dp)
                            .width(18.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary,
                    )
                } else {
                    Text("Unlock")
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun VersionPicker(
    versions: List<PromptVersion>,
    selectedIndex: Int,
    enabled: Boolean,
    onVersionSelected: (Int) -> Unit,
    onDeleteVersion: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }
    val dateFormatter = remember { SimpleDateFormat("MMM d, yyyy", Locale.US) }

    fun PromptVersion.label() = "v${version} · ${dateFormatter.format(Date(createdAt))}"

    // Index 0 is always "v0 (default)"; user versions occupy indices 1+
    val displayText = if (selectedIndex == 0) "v0 (default)" else versions.getOrNull(selectedIndex - 1)?.label() ?: "v0 (default)"

    ExposedDropdownMenuBox(
        expanded = expanded && enabled,
        onExpandedChange = { if (enabled) expanded = !expanded },
        modifier = modifier,
    ) {
        OutlinedTextField(
            value = displayText,
            onValueChange = {},
            readOnly = true,
            label = { Text("Version") },
            trailingIcon = { if (enabled) ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(MenuAnchorType.PrimaryNotEditable),
            enabled = enabled,
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            DropdownMenuItem(
                text = { Text("v0 (default)") },
                onClick = {
                    expanded = false
                    onVersionSelected(0)
                },
            )
            versions.forEachIndexed { index, version ->
                DropdownMenuItem(
                    text = { Text(version.label()) },
                    trailingIcon = {
                        IconButton(onClick = {
                            expanded = false
                            onDeleteVersion(index + 1)
                        }) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Delete version ${version.version}",
                                tint = MaterialTheme.colorScheme.error,
                            )
                        }
                    },
                    onClick = {
                        expanded = false
                        onVersionSelected(index + 1)
                    },
                )
            }
        }
    }
}
