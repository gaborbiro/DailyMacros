package dev.gaborbiro.dailymacros.features.shared.photodiary

import android.app.NotificationManager
import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.hilt.work.HiltWorker
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import dev.gaborbiro.dailymacros.features.shared.R
import dev.gaborbiro.dailymacros.features.shared.notifications.CHANNEL_ID_GENERAL
import dev.gaborbiro.dailymacros.repositories.settings.domain.SettingsRepository
import kotlin.math.ceil
import kotlin.time.Duration.Companion.hours

@HiltWorker
class PhotoMonitorWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted params: WorkerParameters,
    private val autoPhotoRecognitionUseCase: AutoPhotoRecognitionUseCase,
    private val settingsRepository: SettingsRepository,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val lastId = settingsRepository.getLastProcessedMediaStoreId()

        if (lastId == -1L) {
            initHighWaterMark()
            reenqueue(applicationContext)
            return Result.success()
        }

        val newPhotos = queryCameraPhotosSince(lastId)
        if (newPhotos.isNotEmpty()) {
            val maxId = newPhotos.maxOf { it.first }
            settingsRepository.setLastProcessedMediaStoreId(maxId)

            val now = System.currentTimeMillis()
            val lastRequestMs = settingsRepository.getLastPhotoRecognitionRequestEpochMs()
            val elapsed = now - lastRequestMs
            if (elapsed < 1.hours.inWholeMilliseconds) {
                val minutesRemaining = ceil((1.hours.inWholeMilliseconds - elapsed) / 60_000.0).toInt()
                showStatusNotification(applicationContext, applicationContext.getString(R.string.photo_recognition_rate_limited, minutesRemaining))
                Log.d(TAG, "Rate-limited: skipping photo recognition ($minutesRemaining min remaining)")
            } else {
                settingsRepository.setLastPhotoRecognitionRequestEpochMs(now)
                val latestUri = newPhotos.last().second
                showStatusNotification(applicationContext, applicationContext.getString(R.string.photo_recognition_scanning))
                runCatching { autoPhotoRecognitionUseCase.execute(latestUri) }
                    .onFailure { Log.e(TAG, "Recognition failed for $latestUri", it) }
                cancelStatusNotification(applicationContext)
            }
        }

        reenqueue(applicationContext)
        return Result.success()
    }

    private fun initHighWaterMark() {
        val projection = arrayOf(MediaStore.Images.Media._ID)
        val sortOrder = "${MediaStore.Images.Media._ID} DESC"
        applicationContext.contentResolver.query(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            projection,
            null,
            null,
            sortOrder,
        )?.use { cursor ->
            val id = if (cursor.moveToFirst()) {
                cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID))
            } else {
                0L
            }
            settingsRepository.setLastProcessedMediaStoreId(id)
        }
    }

    private fun queryCameraPhotosSince(lastId: Long): List<Pair<Long, Uri>> {
        val projection = arrayOf(MediaStore.Images.Media._ID)
        val selection = "${MediaStore.Images.Media._ID} > ? AND ${MediaStore.Images.Media.BUCKET_DISPLAY_NAME} = ?"
        val selectionArgs = arrayOf(lastId.toString(), CAMERA_BUCKET_NAME)
        val sortOrder = "${MediaStore.Images.Media._ID} ASC"

        return applicationContext.contentResolver.query(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            projection,
            selection,
            selectionArgs,
            sortOrder,
        )?.use { cursor ->
            val results = mutableListOf<Pair<Long, Uri>>()
            val idCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
            while (cursor.moveToNext()) {
                val id = cursor.getLong(idCol)
                val uri = ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id)
                results.add(id to uri)
            }
            results
        } ?: emptyList()
    }

    companion object {
        private const val TAG = "PhotoMonitorWorker"
        private const val CAMERA_BUCKET_NAME = "Camera"
        internal const val WORK_NAME = "photo_monitor"
        private val STATUS_NOTIFICATION_ID = "photo_monitor_status".hashCode()

        private fun showStatusNotification(context: Context, message: String) {
            val notification = NotificationCompat.Builder(context, CHANNEL_ID_GENERAL)
                .setSmallIcon(R.drawable.ic_nutrition)
                .setContentText(message)
                .setAutoCancel(true)
                .build()
            context.getSystemService(NotificationManager::class.java)
                .notify(STATUS_NOTIFICATION_ID, notification)
        }

        private fun cancelStatusNotification(context: Context) {
            context.getSystemService(NotificationManager::class.java)
                .cancel(STATUS_NOTIFICATION_ID)
        }

        fun enqueue(context: Context) {
            val request = OneTimeWorkRequestBuilder<PhotoMonitorWorker>()
                .setConstraints(
                    Constraints.Builder()
                        .addContentUriTrigger(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, true)
                        .build()
                )
                .build()
            WorkManager.getInstance(context).enqueueUniqueWork(
                WORK_NAME,
                ExistingWorkPolicy.KEEP,
                request,
            )
        }

        fun cancel(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
        }

        private fun reenqueue(context: Context) {
            val request = OneTimeWorkRequestBuilder<PhotoMonitorWorker>()
                .setConstraints(
                    Constraints.Builder()
                        .addContentUriTrigger(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, true)
                        .build()
                )
                .build()
            WorkManager.getInstance(context).enqueueUniqueWork(
                WORK_NAME,
                ExistingWorkPolicy.REPLACE,
                request,
            )
        }
    }
}
