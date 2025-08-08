package dev.gaborbiro.dailymacros.features.overview

import android.content.Context
import dev.gaborbiro.dailymacros.features.modal.ModalActivity

interface OverviewNavigator {

    fun updateRecordPhoto(recordId: Long)

    fun editRecord(recordId: Long)

    fun viewImage(recordId: Long)
}

class OverviewNavigatorImpl(private val appContext: Context) : OverviewNavigator {

    override fun updateRecordPhoto(recordId: Long) {
        ModalActivity.launchChangeImage(appContext, recordId)
    }

    override fun editRecord(recordId: Long) {
        ModalActivity.launchViewRecordDetails(appContext, recordId)
    }

    override fun viewImage(recordId: Long) {
        ModalActivity.launchViewImage(appContext, recordId)
    }
}
