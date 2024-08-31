package dev.gaborbiro.feature.home.screens.home

import android.widget.Toast
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.gaborbiro.feature.home.screens.home.model.HomeUIUpdates
import dev.gaborbiro.feature.home.screens.home.model.HomeViewState
import dev.gaborbiro.nutrition.core.clause.resolve
import dev.gaborbiro.nutrition.core.compose.Padding
import dev.gaborbiro.nutrition.core.compose.PreviewContext
import dev.gaborbiro.nutrition.core.navigation.NavigationDispatcher


@Composable
fun HomeScreen(navDispatcher: NavigationDispatcher, modifier: Modifier) {
    val viewModel: HomeViewModel = viewModel()
    val viewState by viewModel.viewState.collectAsStateWithLifecycle()

    HomeContent(
        modifier = modifier,
        viewState = viewState,
        onButtonTapped = {
            viewModel.onButtonTapped()
        }
    )

    val uiUpdates by viewModel.uiUpdates.collectAsStateWithLifecycle()
    val context = LocalContext.current
    uiUpdates.forEach { uiUpdate ->
        when (val update = uiUpdate.get()) {
            is HomeUIUpdates.Toast -> {
                Toast.makeText(context, update.message.resolve(), Toast.LENGTH_SHORT).show()
            }

            null -> {}
        }
    }
}

@Composable
private fun HomeContent(
    modifier: Modifier,
    viewState: HomeViewState,
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
            {}
        )
    }
}