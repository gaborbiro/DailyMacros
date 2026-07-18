package dev.gaborbiro.dailymacros.features.shared.photodiary

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class PhotoRecognitionActionReceiver : BroadcastReceiver() {

    @Inject lateinit var confirmUseCase: ConfirmAutoPhotoEntryUseCase
    @Inject lateinit var denyUseCase: DenyAutoPhotoEntryUseCase

    override fun onReceive(context: Context, intent: Intent) {
        val imageFilename = intent.getStringExtra(EXTRA_IMAGE_FILENAME) ?: return
        val notificationId = intent.getIntExtra(EXTRA_NOTIFICATION_ID, -1)

        context.getSystemService(NotificationManager::class.java).cancel(notificationId)

        val pendingResult = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                when (intent.action) {
                    ACTION_CONFIRM -> {
                        val title = intent.getStringExtra(EXTRA_RECOGNISED_TITLE) ?: return@launch
                        val sourceMediaStoreId = intent
                            .takeIf { it.hasExtra(EXTRA_SOURCE_MEDIA_STORE_ID) }
                            ?.getLongExtra(EXTRA_SOURCE_MEDIA_STORE_ID, -1L)
                            ?.takeIf { it >= 0 }
                        confirmUseCase.execute(imageFilename, title, sourceMediaStoreId)
                    }
                    ACTION_DENY -> denyUseCase.execute(imageFilename)
                }
            } finally {
                pendingResult.finish()
            }
        }
    }

    companion object {
        const val ACTION_CONFIRM = "dev.gaborbiro.dailymacros.ACTION_CONFIRM_PHOTO_ENTRY"
        const val ACTION_DENY = "dev.gaborbiro.dailymacros.ACTION_DENY_PHOTO_ENTRY"
        const val EXTRA_IMAGE_FILENAME = "extra_image_filename"
        const val EXTRA_RECOGNISED_TITLE = "extra_recognised_title"
        const val EXTRA_NOTIFICATION_ID = "extra_notification_id"
        const val EXTRA_SOURCE_MEDIA_STORE_ID = "extra_source_media_store_id"
    }
}
