package dev.gaborbiro.dailymacros.features.shared

import android.app.PendingIntent
import android.content.Context

interface ModalNavigator {

    fun launchToShowRecordImage(context: Context, recordId: Long)

    fun launchViewRecordDetails(context: Context, recordId: Long)

    fun viewRecordDetailsPendingIntent(
        context: Context,
        requestCode: Int,
        recordId: Long,
        notificationId: Int,
    ): PendingIntent

    fun launchToAddRecordFromPhotoRecognition(context: Context, imageFilename: String, recognisedTitle: String)

    fun photoRecognitionDetailsPendingIntent(
        context: Context,
        requestCode: Int,
        imageFilename: String,
        recognisedTitle: String,
        notificationId: Int,
    ): PendingIntent

    /** Opens the add-meal flow in [mode] and dismisses [notificationId] (e.g. from a meal reminder). */
    fun addRecordPendingIntent(
        context: Context,
        requestCode: Int,
        mode: AddRecordMode,
        notificationId: Int,
    ): PendingIntent

    enum class AddRecordMode { CAMERA, BROWSE_IMAGES, TEXT_ONLY }
}
