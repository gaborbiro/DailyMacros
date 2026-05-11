package dev.gaborbiro.dailymacros.features.modal

import android.content.Context

interface ModalNavigator {
    fun launchToAddRecordWithCamera(context: Context)

    fun launchToAddRecordWithImagePicker(context: Context)

    fun launchToShowRecordImage(context: Context, recordId: Long)

    fun launchToShowRecordImageNoApp(context: Context, recordId: Long)

    fun launchToShowTemplateImage(context: Context, templateId: Long)

    fun launchToAddRecord(context: Context)

    fun launchViewRecordDetails(context: Context, recordId: Long)

    fun launchToSelectRecordAction(context: Context, recordId: Long)

    fun launchToSelectTemplateAction(context: Context, templateId: Long)
}
