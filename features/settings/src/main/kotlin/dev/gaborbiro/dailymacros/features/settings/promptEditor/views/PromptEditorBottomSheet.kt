package dev.gaborbiro.dailymacros.features.settings.promptEditor.views

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.ModalBottomSheetProperties
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.gaborbiro.dailymacros.features.settings.promptEditor.PromptEditorUiUpdates
import dev.gaborbiro.dailymacros.features.settings.promptEditor.model.PromptEditorUiState
import dev.gaborbiro.dailymacros.features.settings.util.verticalScrollWithBar
import dev.gaborbiro.dailymacros.repositories.chatgpt.domain.model.PromptSegment
import kotlinx.coroutines.flow.Flow

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun PromptEditorBottomSheet(
    viewState: PromptEditorUiState,
    events: Flow<PromptEditorUiUpdates>,
    onDismissRequested: () -> Unit,
    onValueChanged: (String, String) -> Unit,
    onResetSegment: (String) -> Unit,
    onSaveTapped: () -> Unit,
    onExitDialogSaveTapped: () -> Unit,
    onExitDialogDiscardTapped: () -> Unit,
    onExitDialogDismissed: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val systemBarHeight = with(LocalDensity.current) { WindowInsets.systemBars.getTop(this) }

    LaunchedEffect(Unit) {
        events.collect { event ->
            when (event) {
                PromptEditorUiUpdates.Show -> sheetState.show()
                PromptEditorUiUpdates.Hide -> {
                    sheetState.hide()
                    onDismissRequested()
                }
                PromptEditorUiUpdates.Close -> Unit
            }
        }
    }

    ModalBottomSheet(
        containerColor = MaterialTheme.colorScheme.surface,
        sheetState = sheetState,
        contentWindowInsets = { WindowInsets(0, systemBarHeight, 0, 0) },
        onDismissRequest = onDismissRequested,
        properties = ModalBottomSheetProperties(shouldDismissOnBackPress = true),
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "AI Prompts",
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.padding(end = 16.dp),
            )
            Spacer(Modifier.weight(1f))
            TextButton(
                onClick = onSaveTapped,
                enabled = viewState.canSave,
                contentPadding = ButtonDefaults.TextButtonContentPadding,
            ) {
                Text(
                    text = "Save",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                )
            }
        }
        Text(
            text = "Edit the system message to change AI behaviour. Grey sections are locked.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        )

        // Tabs
        var selectedTab by remember { mutableIntStateOf(0) }
        val tabs = listOf("Recognition", "Analysis")
        PrimaryTabRow(selectedTabIndex = selectedTab) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { selectedTab = index },
                    text = { Text(title) },
                )
            }
        }

        val segments = if (selectedTab == 0) viewState.recognitionSegments else viewState.analysisSegments

        Column(
            modifier = Modifier
                .verticalScrollWithBar(autoFade = false)
                .padding(16.dp)
                .imePadding()
        ) {
            segments.forEach { segment ->
                when (segment) {
                    is PromptSegment.Locked -> {
                        Text(
                            text = segment.text,
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp,
                            ),
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 2.dp),
                        )
                    }
                    is PromptSegment.Editable -> {
                        Spacer(Modifier.height(8.dp))
                        val currentText = viewState.currentValues[segment.id] ?: segment.defaultText
                        val isOverridden = viewState.currentValues.containsKey(segment.id) &&
                            viewState.currentValues[segment.id] != segment.defaultText
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.Top,
                        ) {
                            OutlinedTextField(
                                value = currentText,
                                onValueChange = { onValueChanged(segment.id, it) },
                                label = { Text(segment.label) },
                                placeholder = if (segment.hint.isNotBlank()) {
                                    { Text(segment.hint, style = MaterialTheme.typography.bodySmall) }
                                } else null,
                                modifier = Modifier.weight(1f),
                                minLines = if (currentText.contains('\n')) {
                                    currentText.lines().size.coerceIn(3, 10)
                                } else 1,
                            )
                            if (isOverridden) {
                                Spacer(Modifier.width(4.dp))
                                IconButton(
                                    onClick = { onResetSegment(segment.id) },
                                    modifier = Modifier.padding(top = 4.dp),
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Refresh,
                                        contentDescription = "Reset to default",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                        }
                        Spacer(Modifier.height(8.dp))
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
