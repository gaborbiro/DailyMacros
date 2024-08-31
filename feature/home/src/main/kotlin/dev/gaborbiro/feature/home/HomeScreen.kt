package dev.gaborbiro.feature.home

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
import dev.gaborbiro.nutrition.core.compose.Padding
import dev.gaborbiro.nutrition.core.compose.PreviewContext
import dev.gaborbiro.nutrition.core.navigation.NavigationDispatcher
import dev.gaborbiro.nutrition.core.navigation.NavigatorHost


@Composable
fun HomeScreen(navDispatcher: NavigationDispatcher, modifier: Modifier) {
    Column(modifier = modifier) {
        val focuser = remember { FocusRequester() }
        var queryText by remember { mutableStateOf("") }
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

        LaunchedEffect(Unit) {
            focuser.requestFocus()
        }
    }
}

object HomeScreenNavHost : NavigatorHost {

    const val NAV_ROUTE = "home"

    override fun buildGraph(
        builder: NavGraphBuilder,
        navDispatcher: NavigationDispatcher,
        modifier: Modifier,
    ) {
        builder.composable(route = NAV_ROUTE) {
            dev.gaborbiro.nutrition.core.compose.theme.NutriTheme {
                HomeScreen(navDispatcher, modifier)
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun HomeScreenPreview() {
    PreviewContext { modifier ->
        HomeScreen(
            navDispatcher = NavigationDispatcher.DUMMY_IMPLEMENTATION,
            modifier,
        )
    }
}