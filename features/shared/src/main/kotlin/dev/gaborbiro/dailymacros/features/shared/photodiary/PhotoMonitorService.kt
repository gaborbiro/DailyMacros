package dev.gaborbiro.dailymacros.features.shared.photodiary

import android.app.Service
import android.content.ContentUris
import android.content.Intent
import android.database.ContentObserver
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.provider.MediaStore
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.app.ServiceCompat.STOP_FOREGROUND_REMOVE
import dagger.hilt.android.AndroidEntryPoint
import dev.gaborbiro.dailymacros.features.shared.R
import dev.gaborbiro.dailymacros.features.shared.notifications.CHANNEL_ID_FOREGROUND
import dev.gaborbiro.dailymacros.repositories.settings.domain.SettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject

@AndroidEntryPoint
class PhotoMonitorService : Service() {

    @Inject lateinit var autoPhotoRecognitionUseCase: AutoPhotoRecognitionUseCase
    @Inject lateinit var settingsRepository: SettingsRepository

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val queryMutex = Mutex()
    private var observer: ContentObserver? = null

    override fun onCreate() {
        super.onCreate()
        val notification = NotificationCompat.Builder(this, CHANNEL_ID_FOREGROUND)
            .setSmallIcon(R.drawable.ic_nutrition)
            .setContentTitle(getString(R.string.photo_monitor_notification_title))
            .setOngoing(true)
            .setSilent(true)
            .build()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC,
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        registerObserver()
        return START_STICKY
    }

    private fun registerObserver() {
        if (observer != null) return
        // Initialise the high-water mark so we don't re-process existing photos on first start.
        if (settingsRepository.getLastProcessedMediaStoreId() == -1L) {
            initHighWaterMark()
        }
        observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean) = onMediaStoreChanged()
        }
        contentResolver.registerContentObserver(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            true,
            observer!!,
        )
    }

    private fun initHighWaterMark() {
        scope.launch {
            val projection = arrayOf(MediaStore.Images.Media._ID)
            val sortOrder = "${MediaStore.Images.Media._ID} DESC"
            contentResolver.query(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                projection,
                null,
                null,
                sortOrder,
            )?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val id = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID))
                    settingsRepository.setLastProcessedMediaStoreId(id)
                } else {
                    settingsRepository.setLastProcessedMediaStoreId(0L)
                }
            }
        }
    }

    private fun onMediaStoreChanged() {
        scope.launch {
            queryMutex.withLock {
                val lastId = settingsRepository.getLastProcessedMediaStoreId()
                if (lastId < 0L) return@launch

                val newPhotos = queryCameraPhotosSince(lastId)
                if (newPhotos.isEmpty()) return@launch

                settingsRepository.setLastProcessedMediaStoreId(newPhotos.maxOf { it.first })
                newPhotos
            }.forEach { (_, uri) ->
                scope.launch {
                    runCatching { autoPhotoRecognitionUseCase.execute(uri) }
                        .onFailure { Log.e(TAG, "Recognition failed for $uri", it) }
                }
            }
        }
    }

    private fun queryCameraPhotosSince(lastId: Long): List<Pair<Long, android.net.Uri>> {
        val projection = arrayOf(MediaStore.Images.Media._ID)
        val selection = "${MediaStore.Images.Media._ID} > ? AND ${MediaStore.Images.Media.BUCKET_DISPLAY_NAME} = ?"
        val selectionArgs = arrayOf(lastId.toString(), CAMERA_BUCKET_NAME)
        val sortOrder = "${MediaStore.Images.Media._ID} ASC"

        return contentResolver.query(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            projection,
            selection,
            selectionArgs,
            sortOrder,
        )?.use { cursor ->
            val results = mutableListOf<Pair<Long, android.net.Uri>>()
            val idCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
            while (cursor.moveToNext()) {
                val id = cursor.getLong(idCol)
                val uri = ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id)
                results.add(id to uri)
            }
            results
        } ?: emptyList()
    }

    override fun onDestroy() {
        observer?.let { contentResolver.unregisterContentObserver(it) }
        observer = null
        ServiceCompat.stopForeground(this, STOP_FOREGROUND_REMOVE)
        scope.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        private const val TAG = "PhotoMonitorService"
        private const val NOTIFICATION_ID = 9001
        private const val CAMERA_BUCKET_NAME = "Camera"
    }
}
