package dev.gaborbiro.dailymacros.features.settings.promptEditor

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.gaborbiro.dailymacros.features.settings.promptEditor.views.PromptEditorView

@Composable
fun PromptEditorScreen(
    viewModel: PromptEditorViewModel,
    onCloseRequested: () -> Unit,
) {
    val viewState by viewModel.uiState.collectAsStateWithLifecycle()

    PromptEditorView(
        viewState = viewState,
        onDismissRequested = viewModel::onBottomSheetDismissRequested,
        onValueChanged = viewModel::onValueChanged,
        onResetTab = viewModel::onResetTab,
        onSaveTapped = viewModel::onSaveTapped,
        onVersionSelected = viewModel::onVersionSelected,
        onDeleteVersion = viewModel::onDeleteVersion,
        onExitDialogSaveTapped = viewModel::onExitDialogSaveTapped,
        onExitDialogDiscardTapped = viewModel::onExitDialogDiscardTapped,
        onExitDialogDismissed = viewModel::onExitDialogDismissed,
    )

    LaunchedEffect(viewModel) {
        viewModel.uiUpdates.collect { event ->
            when (event) {
                PromptEditorUiUpdates.Hide, PromptEditorUiUpdates.Close -> onCloseRequested()
                else -> Unit
            }
        }
    }
}
