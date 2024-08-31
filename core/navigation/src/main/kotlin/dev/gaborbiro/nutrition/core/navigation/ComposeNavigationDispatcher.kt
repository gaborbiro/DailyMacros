package dev.gaborbiro.nutrition.core.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavOptionsBuilder


/**
 * Example usage:
 *
 * ```
 *         setContent {
 *             AppTheme {
 *                 val navController = rememberNavController()
 *                 val navDispatcher = remember {
 *                     ComposeNavigationDispatcher(
 *                         navController
 *                     )
 *                 }
 *
 *                 NavHost(
 *                     navController = navController,
 *                     startDestination = FancyScreen1NavHost.NAV_ROUTE,
 *                 ) {
 *                     FancyScreen1NavHost.buildGraph(this, navDispatcher)
 *                     FancyScreen2NavHost.buildGraph(this, navDispatcher)
 *                 }
 *             }
 *         }
 * ```
 */
class ComposeNavigationDispatcher(
    private val navController: NavController
) : NavigationDispatcher {

    override fun navigateTo(command: NavigationCommand) {
        when (command) {
            is NavigationCommand.ClearStack -> navController.navigate(
                route = command.feature,
                builder = clearStack(navController)
            )

            is NavigationCommand.PopToRoot -> navController.navigate(
                route = command.feature,
                builder = popToRoot(navController)
            )

            is NavigationCommand.NoPop -> navController.navigate(route = command.feature)

            is NavigationCommand.Back -> navController.popBackStack()

            is NavigationCommand.PopIfVisible -> {
                navController.popBackStack()
            }
        }
    }

    override fun closeIfOnTop(feature: String) {
        if (navController.visibleEntries.value.lastOrNull()?.destination?.route == feature) {
            navController.popBackStack()
        }
    }

    private fun popToRoot(navController: NavController): NavOptionsBuilder.() -> Unit = {
        popUpTo(navController.graph.findStartDestination().id) {
            this.inclusive = false
            saveState = true
        }
        launchSingleTop = true
        restoreState = true
    }

    private fun clearStack(navController: NavController): NavOptionsBuilder.() -> Unit = {
        var popResult = navController.popBackStack()
        while (popResult) {
            popResult = navController.popBackStack()
        }
    }
}