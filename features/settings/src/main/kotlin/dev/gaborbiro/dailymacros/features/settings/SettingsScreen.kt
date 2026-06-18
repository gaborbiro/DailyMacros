package dev.gaborbiro.dailymacros.features.settings

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.Scope
import dev.gaborbiro.dailymacros.features.settings.export.ProcessRestarter
import dev.gaborbiro.dailymacros.features.settings.export.rememberCreatePublicDocumentUseCase
import dev.gaborbiro.dailymacros.features.settings.export.rememberOpenPublicDocumentUseCase
import dev.gaborbiro.dailymacros.features.settings.model.SettingsUiUpdates
import dev.gaborbiro.dailymacros.features.settings.promptEditor.PromptEditorScreen
import dev.gaborbiro.dailymacros.features.settings.promptEditor.PromptEditorViewModel
import dev.gaborbiro.dailymacros.features.settings.targetsSettings.TargetsSettingsScreen
import dev.gaborbiro.dailymacros.features.settings.targetsSettings.TargetsSettingsViewModel
import dev.gaborbiro.dailymacros.features.settings.views.SettingsView

@Composable
fun SettingsScreen(
    settingsViewModel: SettingsViewModel,
    targetsSettingsViewModel: TargetsSettingsViewModel,
    promptEditorViewModel: PromptEditorViewModel,
    navController: NavHostController,
) {
    val settingsUiState by settingsViewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    val createPublicDocumentUseCase = rememberCreatePublicDocumentUseCase()
    val openPublicDocumentUseCase = rememberOpenPublicDocumentUseCase()

    val signInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val data = result.data
        if (data != null) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            try {
                val account = task.getResult(ApiException::class.java)
                val email = account?.email
                if (email != null) {
                    settingsViewModel.onGoogleSignInSuccess(email)
                } else {
                    settingsViewModel.onGoogleSignInFailed("No email returned")
                }
            } catch (e: ApiException) {
                settingsViewModel.onGoogleSignInFailed(e.message ?: e.statusCode.toString())
            }
        }
    }

    LaunchedEffect(settingsViewModel, context) {
        settingsViewModel.uiUpdates.collect { event ->
            when (event) {
                SettingsUiUpdates.NavigateBack -> navController.popBackStack()
                SettingsUiUpdates.RestartApplication -> context.findActivity()?.let {
                    ProcessRestarter.restartApplication(it)
                }
                is SettingsUiUpdates.ShowSnackbar -> snackbarHostState.showSnackbar(
                    event.message,
                    withDismissAction = true,
                    duration = SnackbarDuration.Indefinite,
                )
                SettingsUiUpdates.RequestGoogleSignIn -> {
                    val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                        .requestEmail()
                        .requestScopes(Scope("https://www.googleapis.com/auth/drive.appdata"))
                        .build()
                    signInLauncher.launch(GoogleSignIn.getClient(context, gso).signInIntent)
                }
            }
        }
    }

    SettingsView(
        viewState = settingsUiState,
        snackbarHostState = snackbarHostState,
        onBackNavigateRequested = settingsViewModel::onBackNavigateRequested,
        onTargetsSettingTapped = settingsViewModel::onTargetsSettingsTapped,
        onPromptEditorTapped = settingsViewModel::onPromptEditorTapped,
        onDiaryDayStartTapped = settingsViewModel::onDiaryDayStartRowTapped,
        onDiaryDayStartDialogDismissed = settingsViewModel::onDiaryDayStartDialogDismissed,
        onDiaryDayStartHourSelected = settingsViewModel::onDiaryDayStartHourSelected,
        onExportSettingTapped = { settingsViewModel.onExportSettingsTapped(createPublicDocumentUseCase) },
        onExportDbTapped = { settingsViewModel.onExportDbTapped(createPublicDocumentUseCase) },
        onImportDbTapped = { settingsViewModel.onImportDbTapped(openPublicDocumentUseCase) },
        onCloudSyncTapped = settingsViewModel::onCloudSyncRowTapped,
        onSyncTapped = settingsViewModel::onSyncTapped,
        onRestoreConfirmed = settingsViewModel::onRestoreConfirmed,
        onRestoreDialogDismissed = settingsViewModel::onRestoreDialogDismissed,
    )

    if (settingsUiState.showTargetsSettings) {
        TargetsSettingsScreen(
            viewModel = targetsSettingsViewModel,
            onCloseRequested = settingsViewModel::onTargetsSettingsCloseRequested,
        )
    }

    if (settingsUiState.showPromptEditor) {
        PromptEditorScreen(
            viewModel = promptEditorViewModel,
            onCloseRequested = settingsViewModel::onPromptEditorCloseRequested,
        )
    }
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
