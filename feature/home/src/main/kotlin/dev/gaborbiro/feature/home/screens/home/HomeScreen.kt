package dev.gaborbiro.feature.home.screens.home

import android.app.Activity.RESULT_OK
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.ActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScaffoldDefaults
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.android.gms.auth.api.identity.AuthorizationRequest
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.common.api.Scope
import com.google.api.services.keep.v1.KeepScopes
import dev.gaborbiro.feature.home.screens.home.model.HomeUIUpdates
import dev.gaborbiro.feature.home.screens.home.model.HomeViewState
import dev.gaborbiro.nutrition.core.clause.resolve
import dev.gaborbiro.nutrition.core.compose.Padding
import dev.gaborbiro.nutrition.core.compose.PreviewContext
import dev.gaborbiro.nutrition.core.compose.theme.NutriColor
import dev.gaborbiro.nutrition.core.navigation.NavigationDispatcher
import kotlinx.coroutines.launch


@Composable
fun HomeScreen(
    navDispatcher: NavigationDispatcher,
    modifier: Modifier,
    viewModel: HomeViewModel,
) {
    val viewState by viewModel.viewState.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    Scaffold(
        contentWindowInsets = ScaffoldDefaults.contentWindowInsets.union(WindowInsets.ime),
        modifier = Modifier
            .fillMaxSize(),
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState)
        },
        content = { innerPadding ->
            HomeContent(
                modifier = modifier
                    .padding(innerPadding),
                viewState = viewState,
                onTextChanged = {
                    viewModel.onTextChanged(it)
                },
                onAskChatGPTButtonTapped = {
                    viewModel.onAskChatGPTButtonTapped()
                },
                onFetchKeepNotesButtonTapped = {
                    viewModel.onFetchKeepNotesButtonTapped()
                }
            )
        },
    )

    val uiUpdates by viewModel.uiUpdates.collectAsStateWithLifecycle()
    val context = LocalContext.current
//    val credentialManager = remember { CredentialManager.create(context) }
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult()
    ) { result: ActivityResult ->
        if (result.resultCode == RESULT_OK) {
            val authorizationResult =
                Identity.getAuthorizationClient(context)
                    .getAuthorizationResultFromIntent(result.data)
            viewModel.onAuthorizationResult(authorizationResult)
        } else {
            viewModel.handleSignInFailure(null)
        }
    }

    viewState.authorizationRequest?.let {
        LaunchedEffect(it) {
            Identity
                .getAuthorizationClient(context)
                .authorize(it)
                .addOnSuccessListener { authorizationResult ->
                    if (authorizationResult.hasResolution()) {
                        val pendingIntent = authorizationResult.pendingIntent
                        launcher.launch(
                            IntentSenderRequest.Builder(pendingIntent!!.intentSender).build()
                        )
                    } else {
                        viewModel.onAuthorizationResult(authorizationResult)
                    }
                }
//            try {
//                val result: GetCredentialResponse = credentialManager.getCredential(
//                    request = it,
//                    context = context,
//                )
//                viewModel.handleSignIn(result)
//            } catch (e: GetCredentialException) {
//                viewModel.handleSignInFailure(e)
//            } catch (e: Throwable) {
//                viewModel.handleSignInFailure(e)
//            }
        }
    }

    uiUpdates.forEach { uiUpdate ->
        when (val update = uiUpdate.get()) {
            is HomeUIUpdates.Toast -> {
                val message = update.message.resolve()
                scope.launch {
                    snackbarHostState.showSnackbar(
                        message = message,
                        withDismissAction = true,
                        duration = SnackbarDuration.Long,
                    )
                }
            }

            null -> {}
        }
    }
}

@Composable
private fun HomeContent(
    modifier: Modifier,
    viewState: HomeViewState,
    onTextChanged: (String) -> Unit,
    onAskChatGPTButtonTapped: () -> Unit,
    onFetchKeepNotesButtonTapped: () -> Unit,
) {
    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
    ) {
        val focuser = remember { FocusRequester() }
        var queryText by remember(viewState.question) { mutableStateOf(viewState.question) }
        TextField(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Padding.normal)
                .focusRequester(focuser),
            singleLine = false,
            label = {
                Text(text = "What did you eat today")
            },
            value = queryText ?: "",
            onValueChange = {
                onTextChanged(it)
                queryText = it
            }
        )
        LaunchedEffect(Unit) {
            focuser.requestFocus()
        }
        Box(contentAlignment = Alignment.Center) {
            Button(
                modifier = Modifier.padding(
                    start = Padding.normal,
                    end = Padding.normal,
                    top = Padding.normal
                ),
                onClick = onAskChatGPTButtonTapped
            ) {
                Text(text = "Ask ChatGPT")
            }
            if (viewState.showQueryProgress) {
                CircularProgressIndicator()
            }
        }
        Row(
            modifier = Modifier
                .fillMaxWidth(),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Button(
                    modifier = Modifier
                        .padding(
                            start = Padding.normal,
                            end = Padding.normal,
                            top = Padding.medium
                        ),
                    colors = ButtonDefaults.buttonColors().copy(
                        containerColor = NutriColor.Purple40,
                        contentColor = Color.White,
                    ),
                    onClick = onFetchKeepNotesButtonTapped,
                ) {
                    Text(text = "Fetch Keep notes")
                }
                if (viewState.showFetchKeepNotesProgress) {
                    CircularProgressIndicator()
                }
            }
        }
        Text(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Padding.normal),
            text = viewState.answer ?: ""
        )
    }
}

@Preview(showBackground = true)
@Composable
fun HomeScreenPreview() {
    PreviewContext { modifier ->
        HomeContent(
            modifier,
            HomeViewState(),
            {},
            {},
            {},
        )
    }
}