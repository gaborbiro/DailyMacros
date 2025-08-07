package dev.gaborbiro.nutri.features.overview

import android.content.Context
import dev.gaborbiro.nutri.features.modal.ModalActivity

interface OverviewNavigator {

    fun updateRecordPhoto(recordId: Long)

    fun editRecord(recordId: Long)

    fun viewImage(recordId: Long)
}

class OverviewNavigatorImpl(private val appContext: Context) : OverviewNavigator {

    override fun updateRecordPhoto(recordId: Long) {
        ModalActivity.launchRedoImage(appContext, recordId)
    }

    override fun editRecord(recordId: Long) {
        ModalActivity.launchEdit(appContext, recordId)
    }

    override fun viewImage(recordId: Long) {
        ModalActivity.launchShowImage(appContext, recordId)
    }
}
