package dev.gaborbiro.dailymacros.features.shared

import android.app.PendingIntent
import android.content.Context

interface ModalNavigator {
    fun launchToAddRecordWithCamera(context: Context)

    fun launchToAddRecordWithImagePicker(context: Context)

    fun launchToShowRecordImage(context: Context, recordId: Long)

    fun launchToShowRecordImageNoApp(context: Context, recordId: Long)

    fun launchToShowTemplateImage(context: Context, templateId: Long)

    fun launchToAddRecord(context: Context)

    fun launchViewRecordDetails(context: Context, recordId: Long)

    fun launchViewRecordDetailsFromWidget(context: Context, recordId: Long)

    fun launchViewTemplateDetailsFromWidget(context: Context, templateId: Long)

    fun launchQuickPickWidgetConfirmDialog(context: Context, templateId: Long, templateName: String)

    fun launchToAddRecordFromPhotoRecognition(context: Context, imageFilename: String, recognisedTitle: String)

    fun photoRecognitionDetailsPendingIntent(
        context: Context,
        requestCode: Int,
        imageFilename: String,
        recognisedTitle: String,
        notificationId: Int,
        sourceMediaStoreId: Long?,
    ): PendingIntent
}
