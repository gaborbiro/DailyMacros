package dev.gaborbiro.dailymacros.features.shared

import android.app.PendingIntent
import android.content.Context

interface ModalNavigator {

    fun launchToShowRecordImage(context: Context, recordId: Long)

    fun launchViewRecordDetails(context: Context, recordId: Long)

    fun launchToAddRecordFromPhotoRecognition(context: Context, imageFilename: String, recognisedTitle: String)

    fun photoRecognitionDetailsPendingIntent(
        context: Context,
        requestCode: Int,
        imageFilename: String,
        recognisedTitle: String,
        notificationId: Int,
    ): PendingIntent
}
