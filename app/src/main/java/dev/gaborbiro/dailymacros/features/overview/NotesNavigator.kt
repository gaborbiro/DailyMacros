package dev.gaborbiro.dailymacros.features.overview

import android.content.Context
import dev.gaborbiro.dailymacros.features.modal.ModalActivity

interface OverviewNavigator {

    fun editRecord(recordId: Long)

    fun viewImage(recordId: Long)
}

class OverviewNavigatorImpl(private val appContext: Context) : OverviewNavigator {

    override fun editRecord(recordId: Long) {
        ModalActivity.launchViewRecordDetails(appContext, recordId)
    }

    override fun viewImage(recordId: Long) {
        ModalActivity.launchViewRecordImage(appContext, recordId)
    }
}
