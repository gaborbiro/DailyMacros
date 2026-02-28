package dev.gaborbiro.dailymacros.features.settings

import androidx.navigation.NavHostController

interface SettingsNavigator {
    fun navigateBack()
}

internal class SettingsNavigatorImpl(
    private val navController: NavHostController,
) : SettingsNavigator {

    override fun navigateBack() {
        navController.popBackStack()
    }
}
