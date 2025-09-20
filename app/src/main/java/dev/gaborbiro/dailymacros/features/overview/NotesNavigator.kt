package dev.gaborbiro.dailymacros.features.overview

import android.content.Context
import androidx.navigation.NavHostController
import dev.gaborbiro.dailymacros.features.main.SETTINGS_ROUTE
import dev.gaborbiro.dailymacros.features.modal.ModalActivity

interface OverviewNavigator {

    fun editRecord(recordId: Long)

    fun viewImage(recordId: Long)

    fun openSettingsScreen()
}

class OverviewNavigatorImpl(
    private val appContext: Context,
    private val navController: NavHostController,
) : OverviewNavigator {

    override fun editRecord(recordId: Long) {
        ModalActivity.launchViewRecordDetails(appContext, recordId)
    }

    override fun viewImage(recordId: Long) {
        ModalActivity.launchToShowRecordImage(appContext, recordId)
    }

    override fun openSettingsScreen() {
        navController.navigate(SETTINGS_ROUTE)
    }
}
