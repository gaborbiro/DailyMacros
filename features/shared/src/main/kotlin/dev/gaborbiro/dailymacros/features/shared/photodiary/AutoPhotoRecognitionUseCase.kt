package dev.gaborbiro.dailymacros.features.shared.photodiary

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.ImageDecoder
import android.net.Uri
import androidx.core.app.NotificationCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.gaborbiro.dailymacros.data.image.FoodPicMaxSize
import dev.gaborbiro.dailymacros.data.image.domain.ImageStore
import dev.gaborbiro.dailymacros.data.image.generateFoodPicFilename
import dev.gaborbiro.dailymacros.features.common.utils.inputStreamToBase64
import dev.gaborbiro.dailymacros.features.shared.R
import dev.gaborbiro.dailymacros.features.shared.notifications.CHANNEL_ID_GENERAL
import dev.gaborbiro.dailymacros.repositories.chatgpt.domain.ChatGPTRepository
import dev.gaborbiro.dailymacros.repositories.chatgpt.domain.ForImageUploadChatGpt
import dev.gaborbiro.dailymacros.repositories.chatgpt.domain.model.FoodRecognitionRequest
import dev.gaborbiro.dailymacros.repositories.settings.domain.SettingsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Locale
import javax.inject.Inject

class AutoPhotoRecognitionUseCase @Inject constructor(
    @ApplicationContext private val appContext: Context,
    private val imageStore: ImageStore,
    @ForImageUploadChatGpt private val chatGPTRepository: ChatGPTRepository,
    private val settingsRepository: SettingsRepository,
) {

    suspend fun execute(photoUri: Uri) {
        val filename = withContext(Dispatchers.IO) {
            val source = ImageDecoder.createSource(appContext.contentResolver, photoUri)
            val bitmap = ImageDecoder.decodeBitmap(source) { decoder, imageInfo, _ ->
                val size = imageInfo.size
                val scale = FoodPicMaxSize / maxOf(size.width, size.height).toFloat()
                if (scale < 1f) {
                    decoder.setTargetSize((size.width * scale).toInt(), (size.height * scale).toInt())
                }
                decoder.isMutableRequired = false
            }
            val name = generateFoodPicFilename()
            imageStore.write(name, bitmap)
            name
        }

        val base64 = withContext(Dispatchers.IO) {
            inputStreamToBase64(imageStore.open(filename, thumbnail = false))
        }

        val response = chatGPTRepository.recogniseFood(
            FoodRecognitionRequest(
                base64Images = listOf(base64),
                customisations = settingsRepository.getEffectiveCustomisations(),
                phoneLanguage = Locale.getDefault().getDisplayLanguage(Locale.ENGLISH),
            )
        )

        if (response.title != null) {
            showConfirmationNotification(
                notificationId = filename.hashCode(),
                recognisedTitle = response.title,
                imageFilename = filename,
            )
        } else {
            imageStore.delete(filename)
        }
    }

    private fun showConfirmationNotification(
        notificationId: Int,
        recognisedTitle: String,
        imageFilename: String,
    ) {
        val confirmIntent = pendingBroadcast(
            requestCode = notificationId,
            action = PhotoRecognitionActionReceiver.ACTION_CONFIRM,
            imageFilename = imageFilename,
            recognisedTitle = recognisedTitle,
            notificationId = notificationId,
        )
        val denyIntent = pendingBroadcast(
            requestCode = notificationId + 1,
            action = PhotoRecognitionActionReceiver.ACTION_DENY,
            imageFilename = imageFilename,
            recognisedTitle = null,
            notificationId = notificationId,
        )
        val notification = NotificationCompat.Builder(appContext, CHANNEL_ID_GENERAL)
            .setSmallIcon(R.drawable.ic_nutrition)
            .setContentTitle(recognisedTitle)
            .setContentText(appContext.getString(R.string.photo_recognition_add_to_diary_prompt))
            .setAutoCancel(false)
            .addAction(0, appContext.getString(R.string.photo_recognition_action_add), confirmIntent)
            .addAction(0, appContext.getString(R.string.photo_recognition_action_skip), denyIntent)
            .build()
        appContext.getSystemService(NotificationManager::class.java).notify(notificationId, notification)
    }

    private fun pendingBroadcast(
        requestCode: Int,
        action: String,
        imageFilename: String,
        recognisedTitle: String?,
        notificationId: Int,
    ): PendingIntent = PendingIntent.getBroadcast(
        appContext,
        requestCode,
        Intent(appContext, PhotoRecognitionActionReceiver::class.java).apply {
            this.action = action
            putExtra(PhotoRecognitionActionReceiver.EXTRA_IMAGE_FILENAME, imageFilename)
            putExtra(PhotoRecognitionActionReceiver.EXTRA_NOTIFICATION_ID, notificationId)
            recognisedTitle?.let { putExtra(PhotoRecognitionActionReceiver.EXTRA_RECOGNISED_TITLE, it) }
        },
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )
}
