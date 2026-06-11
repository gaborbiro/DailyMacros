package dev.gaborbiro.dailymacros.features.settings.promptEditor

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.gaborbiro.dailymacros.features.settings.promptEditor.views.PromptEditorBottomSheet

@Composable
fun PromptEditorScreen(
    viewModel: PromptEditorViewModel,
    onCloseRequested: () -> Unit,
) {
    val viewState by viewModel.uiState.collectAsStateWithLifecycle()

    PromptEditorBottomSheet(
        viewState = viewState,
        events = viewModel.uiUpdates,
        onDismissRequested = viewModel::onBottomSheetDismissRequested,
        onValueChanged = viewModel::onValueChanged,
        onResetSegment = viewModel::onResetSegment,
        onSaveTapped = viewModel::onSaveTapped,
        onExitDialogSaveTapped = viewModel::onExitDialogSaveTapped,
        onExitDialogDiscardTapped = viewModel::onExitDialogDiscardTapped,
        onExitDialogDismissed = viewModel::onExitDialogDismissed,
    )

    LaunchedEffect(viewModel) {
        viewModel.uiUpdates.collect { event ->
            when (event) {
                PromptEditorUiUpdates.Close -> onCloseRequested()
                else -> Unit
            }
        }
    }
}
