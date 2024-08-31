package dev.gaborbiro.nutrition.core.navigation


/**
 * Example usage:
 *
 * ```
 * navDispatcher.navigateTo(
 *     NavigationCommand.ClearStack("login")
 * )
 * ```
 * ```
 * navDispatcher.navigateTo(
 *     NavigationCommand.ClearStack(FancyScreenNavHost.NAV_ROUTE)
 * )
 * ```
 */
sealed class NavigationCommand {

    data class NoPop(val feature: String) : NavigationCommand()

    data class PopToRoot(val feature: String) : NavigationCommand()

    data class ClearStack(val feature: String) : NavigationCommand()

    data class PopIfVisible(val feature: String) : NavigationCommand()

    object Back : NavigationCommand()
}