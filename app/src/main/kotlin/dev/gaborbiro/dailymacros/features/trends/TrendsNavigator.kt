package dev.gaborbiro.dailymacros.features.trends

import androidx.navigation.NavHostController

interface TrendsNavigator {
    fun navigateBack()
}

internal class TrendsNavigatorImpl(
    private val navController: NavHostController,
) : TrendsNavigator {

    override fun navigateBack() {
        navController.popBackStack()
    }
}
