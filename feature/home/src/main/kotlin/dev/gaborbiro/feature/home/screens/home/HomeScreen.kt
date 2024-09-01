package dev.gaborbiro.feature.home.screens.home

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.Button
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.gaborbiro.feature.home.screens.home.model.HomeUIUpdates
import dev.gaborbiro.feature.home.screens.home.model.HomeViewState
import dev.gaborbiro.nutrition.core.clause.resolve
import dev.gaborbiro.nutrition.core.compose.Padding
import dev.gaborbiro.nutrition.core.compose.PreviewContext
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
                onButtonTapped = {
                    viewModel.onButtonTapped()
                }
            )
        },
    )

    val uiUpdates by viewModel.uiUpdates.collectAsStateWithLifecycle()

    uiUpdates.forEach { uiUpdate ->
        when (val update = uiUpdate.get()) {
            is HomeUIUpdates.Toast -> {
                val message = update.message.resolve()
                scope.launch {
                    snackbarHostState.showSnackbar(
                        message = message,
                        withDismissAction = true,
                        duration = SnackbarDuration.Short,
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
    onButtonTapped: () -> Unit,
) {
    Column(modifier = modifier) {
        val text = viewState.text.resolve()
        val focuser = remember { FocusRequester() }
        var queryText by remember(text) { mutableStateOf(text) }
        TextField(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Padding.normal)
                .focusRequester(focuser),
            singleLine = false,
            label = {
                Text(text = "What did you eat today")
            },
            value = queryText,
            onValueChange = {
                onTextChanged(it)
                queryText = it
            }
        )
        Button(
            modifier = Modifier.padding(Padding.normal),
            onClick = onButtonTapped
        ) {
            Text(text = "Click me")
        }

        LaunchedEffect(Unit) {
            focuser.requestFocus()
        }
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
        )
    }
}