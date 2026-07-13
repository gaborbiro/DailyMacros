package dev.gaborbiro.dailymacros.features.settings.promptEditor.views

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.clickable
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
import androidx.compose.material3.PrimaryScrollableTabRow
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
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import dev.gaborbiro.dailymacros.features.common.utils.verticalScrollWithBar
import dev.gaborbiro.dailymacros.features.settings.R
import dev.gaborbiro.dailymacros.features.settings.promptEditor.PromptEditorViewModel.Companion.TAB_ANALYSIS
import dev.gaborbiro.dailymacros.features.settings.promptEditor.PromptEditorViewModel.Companion.TAB_ONGOING_WEEK_INSIGHTS
import dev.gaborbiro.dailymacros.features.settings.promptEditor.PromptEditorViewModel.Companion.TAB_RECOGNITION
import dev.gaborbiro.dailymacros.features.settings.promptEditor.PromptEditorViewModel.Companion.TAB_WEEKLY_INSIGHTS
import dev.gaborbiro.dailymacros.features.settings.promptEditor.model.PromptEditorUiState
import dev.gaborbiro.dailymacros.repositories.chatgpt.domain.model.PromptSegment
import dev.gaborbiro.dailymacros.repositories.settings.domain.model.PromptVersion
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val ALL_TABS = listOf(
    TAB_RECOGNITION to "Recognition",
    TAB_ANALYSIS to "Analysis",
    TAB_WEEKLY_INSIGHTS to "Week on Week",
    TAB_ONGOING_WEEK_INSIGHTS to "Ongoing Week",
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun PromptEditorView(
    viewState: PromptEditorUiState,
    onDismissRequested: () -> Unit,
    onValueChanged: (String, String) -> Unit,
    onSaveTapped: (tabType: String) -> Unit,
    onVersionSelected: (tabType: String, index: Int) -> Unit,
    onDeleteVersion: (tabType: String, index: Int) -> Unit,
    onExitDialogSaveTapped: () -> Unit,
    onExitDialogDiscardTapped: () -> Unit,
    onExitDialogDismissed: () -> Unit,
    onApiKeyDraftChanged: (String) -> Unit,
    onUnlockTapped: () -> Unit,
    onClearApiKeyTapped: () -> Unit,
) {
    val visibleTabs = remember(viewState.aiInsightsEnabled) {
        if (viewState.aiInsightsEnabled) ALL_TABS
        else ALL_TABS.filter { (type, _) -> type != TAB_WEEKLY_INSIGHTS && type != TAB_ONGOING_WEEK_INSIGHTS }
    }

    var selectedTab by remember { mutableIntStateOf(0) }
    val currentTabType = visibleTabs[selectedTab].first
    val currentSegments = when (currentTabType) {
        TAB_RECOGNITION -> viewState.recognitionSegments
        TAB_ANALYSIS -> viewState.analysisSegments
        TAB_WEEKLY_INSIGHTS -> viewState.weeklyInsightsSegments
        else -> viewState.ongoingWeekInsightsSegments
    }

    val recognitionScrollState = rememberScrollState()
    val analysisScrollState = rememberScrollState()
    val insightsScrollState = rememberScrollState()
    val ongoingInsightsScrollState = rememberScrollState()
    val activeScrollState: ScrollState = when (currentTabType) {
        TAB_RECOGNITION -> recognitionScrollState
        TAB_ANALYSIS -> analysisScrollState
        TAB_WEEKLY_INSIGHTS -> insightsScrollState
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
                        title = { Text(stringResource(R.string.settings_prompt_editor_title)) },
                        navigationIcon = {
                            IconButton(onClick = onDismissRequested) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = stringResource(R.string.settings_prompt_editor_back_cd),
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
                    // Collapsing section: API key only
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
                        val email = "nomadworkz@gmail.com"
                        val uriHandler = LocalUriHandler.current
                        val primaryColor = MaterialTheme.colorScheme.primary
                        val infoText = if (viewState.promptsEnabled) {
                            stringResource(R.string.settings_prompt_editor_info_enabled, email)
                        } else {
                            stringResource(R.string.settings_prompt_editor_info_disabled, email)
                        }
                        val emailStart = infoText.indexOf(email)
                        val annotated = buildAnnotatedString {
                            append(infoText)
                            if (emailStart >= 0) {
                                addStyle(
                                    SpanStyle(
                                        color = primaryColor,
                                        textDecoration = TextDecoration.Underline,
                                    ),
                                    emailStart,
                                    emailStart + email.length,
                                )
                            }
                        }
                        Text(
                            text = annotated,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier
                                .padding(horizontal = 16.dp, vertical = 4.dp)
                                .clickable { uriHandler.openUri("mailto:$email") },
                        )
                        Spacer(Modifier.height(4.dp))
                    }

                    PrimaryScrollableTabRow(selectedTabIndex = selectedTab) {
                        visibleTabs.forEachIndexed { index, (_, label) ->
                            Tab(
                                selected = selectedTab == index,
                                onClick = { selectedTab = index },
                                text = { Text(label) },
                            )
                        }
                    }

                    // Per-tab version picker
                    VersionPicker(
                        versions = viewState.tabVersions[currentTabType] ?: emptyList(),
                        selectedIndex = viewState.tabSelectedVersionIndex[currentTabType] ?: 0,
                        enabled = viewState.promptsEnabled,
                        onVersionSelected = { onVersionSelected(currentTabType, it) },
                        onDeleteVersion = { onDeleteVersion(currentTabType, it) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                    )

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
                        Button(
                            onClick = { onSaveTapped(currentTabType) },
                            enabled = viewState.promptsEnabled && viewState.currentValues != viewState.originalValues,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp),
                        ) {
                            Text(stringResource(R.string.settings_prompt_editor_save))
                        }
                    }
                }
            }
        }
    }

    if (viewState.showExitDialog) {
        AlertDialog(
            onDismissRequest = onExitDialogDismissed,
            title = { Text(stringResource(R.string.settings_prompt_editor_unsaved_changes_title)) },
            text = { Text(stringResource(R.string.settings_prompt_editor_unsaved_changes_message)) },
            confirmButton = {
                if (viewState.hasAnyUnsavedChanges) {
                    TextButton(onClick = onExitDialogSaveTapped) { Text(stringResource(R.string.settings_prompt_editor_save)) }
                }
            },
            dismissButton = {
                Row {
                    TextButton(onClick = onExitDialogDiscardTapped) { Text(stringResource(R.string.settings_prompt_editor_discard)) }
                    TextButton(onClick = onExitDialogDismissed) { Text(stringResource(R.string.settings_prompt_editor_cancel)) }
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
            label = { Text(stringResource(R.string.settings_prompt_editor_api_key_label)) },
            readOnly = viewState.isApiKeyOverridden,
            singleLine = true,
            modifier = Modifier.weight(1f),
        )
        Spacer(Modifier.width(8.dp))
        if (viewState.isApiKeyOverridden) {
            OutlinedButton(onClick = onClearApiKeyTapped) {
                Text(stringResource(R.string.settings_prompt_editor_clear))
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
                    Text(stringResource(R.string.settings_prompt_editor_unlock))
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

    val versionDefault = stringResource(R.string.settings_prompt_editor_version_default)
    val displayText = if (selectedIndex == 0) versionDefault else versions.getOrNull(selectedIndex - 1)?.label() ?: versionDefault

    ExposedDropdownMenuBox(
        expanded = expanded && enabled,
        onExpandedChange = { if (enabled) expanded = !expanded },
        modifier = modifier,
    ) {
        OutlinedTextField(
            value = displayText,
            onValueChange = {},
            readOnly = true,
            label = { Text(stringResource(R.string.settings_prompt_editor_version_label)) },
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
                text = { Text(versionDefault) },
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
                                contentDescription = stringResource(R.string.settings_prompt_editor_version_delete_cd, version.version),
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
