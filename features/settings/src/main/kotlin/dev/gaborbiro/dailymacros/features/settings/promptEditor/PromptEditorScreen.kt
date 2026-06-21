package dev.gaborbiro.dailymacros.features.settings.promptEditor

import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.gaborbiro.dailymacros.features.settings.promptEditor.views.PromptEditorView

@Composable
fun PromptEditorScreen(
    viewModel: PromptEditorViewModel,
    onCloseRequested: () -> Unit,
) {
    val viewState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    PromptEditorView(
        viewState = viewState,
        onDismissRequested = viewModel::onBottomSheetDismissRequested,
        onValueChanged = viewModel::onValueChanged,
        onSaveTapped = viewModel::onSaveTapped,
        onVersionSelected = viewModel::onVersionSelected,
        onDeleteVersion = viewModel::onDeleteVersion,
        onExitDialogSaveTapped = viewModel::onExitDialogSaveTapped,
        onExitDialogDiscardTapped = viewModel::onExitDialogDiscardTapped,
        onExitDialogDismissed = viewModel::onExitDialogDismissed,
        onApiKeyDraftChanged = viewModel::onApiKeyDraftChanged,
        onUnlockTapped = viewModel::onUnlockTapped,
        onClearApiKeyTapped = viewModel::onClearApiKeyTapped,
    )

    LaunchedEffect(viewModel) {
        viewModel.uiUpdates.collect { event ->
            when (event) {
                PromptEditorUiUpdates.Hide, PromptEditorUiUpdates.Close -> onCloseRequested()
                is PromptEditorUiUpdates.ShowToast -> Toast.makeText(context, event.message, Toast.LENGTH_LONG).show()
                else -> Unit
            }
        }
    }
}
