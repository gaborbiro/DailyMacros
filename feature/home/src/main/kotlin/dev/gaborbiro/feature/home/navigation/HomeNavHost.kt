package dev.gaborbiro.feature.home.navigation

import androidx.compose.ui.Modifier
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import dev.gaborbiro.feature.home.screens.home.HomeScreen
import dev.gaborbiro.nutrition.core.compose.theme.NutriTheme
import dev.gaborbiro.nutrition.core.navigation.NavigationDispatcher
import dev.gaborbiro.nutrition.core.navigation.NavigatorHost

object HomeNavHost : NavigatorHost {

    const val NAV_ROUTE = "home"

    override fun buildGraph(
        builder: NavGraphBuilder,
        navDispatcher: NavigationDispatcher,
        modifier: Modifier,
    ) {
        builder.composable(route = NAV_ROUTE) {
            NutriTheme {
                HomeScreen(navDispatcher, modifier)
            }
        }
    }
}