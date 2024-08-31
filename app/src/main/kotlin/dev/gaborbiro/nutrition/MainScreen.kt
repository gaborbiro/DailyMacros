package dev.gaborbiro.nutrition

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import dev.gaborbiro.nutrition.core.navigation.NavigationDispatcher
import dev.gaborbiro.nutrition.core.navigation.NavigatorHost
import dev.gaborbiro.nutrition.ui.theme.NutriTheme
import dev.gaborbiro.nutrition.ui.theme.Padding


@Composable
fun MainScreen(navDispatcher: NavigationDispatcher, modifier: Modifier) {
    Column(modifier = modifier) {
        val focuser = remember { FocusRequester() }
        var queryText by remember { mutableStateOf("") }
        BuildConfig.CHATGPT_API_KEY
        TextField(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Padding.medium)
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

        LaunchedEffect(Unit) {
            focuser.requestFocus()
        }
    }
}

object MainScreenNavHost : NavigatorHost {

    const val NAV_ROUTE = "main"

    override fun buildGraph(
        builder: NavGraphBuilder,
        navDispatcher: NavigationDispatcher,
        modifier: Modifier,
    ) {
        builder.composable(route = NAV_ROUTE) {
            NutriTheme {
                MainScreen(navDispatcher, modifier)
            }
        }
    }
}


@Preview(showBackground = true)
@Composable
fun MainScreenPreview() {
    NutriTheme {
        MainScreen(
            navDispatcher = NavigationDispatcher.DUMMY_IMPLEMENTATION,
            Modifier,
        )
    }
}