package dev.gaborbiro.nutrition.core.navigation

import androidx.compose.ui.Modifier
import androidx.navigation.NavGraphBuilder


/**
 * Example usage:
 *
 * ```
 *object FancyScreenNavHost : NavigatorHost {
 *
 *     const val NAV_ROUTE = "journal"
 *
 *     override fun buildGraph(
 *         builder: NavGraphBuilder,
 *         navDispatcher: NavigationDispatcher,
 *         modifier: Modifier,
 *     ) {
 *         builder.composable(route = NAV_ROUTE) {
 *             AppTheme {
 *                 FancyScreen(modifier, navDispatcher)
 *             }
 *         }
 *     }
 * }
 * ```
 */
interface NavigatorHost {

    fun buildGraph(
        builder: NavGraphBuilder,
        navDispatcher: NavigationDispatcher,
        modifier: Modifier = Modifier
    )
}