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
import androidx.work.OneTimeWorkRequest
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import dev.gaborbiro.dailymacros.features.shared.R
import dev.gaborbiro.dailymacros.features.shared.notifications.CHANNEL_ID_GENERAL
import dev.gaborbiro.dailymacros.repositories.records.domain.RecordsRepository
import dev.gaborbiro.dailymacros.repositories.settings.domain.SettingsRepository
import java.util.concurrent.TimeUnit
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.seconds

@HiltWorker
class PhotoMonitorWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted params: WorkerParameters,
    private val autoPhotoRecognitionUseCase: AutoPhotoRecognitionUseCase,
    private val settingsRepository: SettingsRepository,
    private val recordsRepository: RecordsRepository,
) : CoroutineWorker(appContext, params) {

    private data class Candidate(
        val id: Long,
        val uri: Uri,
        val mimeType: String,
        val pixels: Long,
        val dateTaken: Long,
        val orientation: Int,
    )

    override suspend fun doWork(): Result {
        if (!settingsRepository.getAutoPhotoRecognitionEnabled()) {
            // Feature was turned off; let the chain die. Re-enabling calls enqueue() again.
            return Result.success()
        }

        if (settingsRepository.getLastProcessedMediaStoreId() == -1L) {
            initHighWaterMark()
            reenqueue(applicationContext)
            return Result.success()
        }

        var scansLeft = MAX_SCANS_PER_RUN
        var deferred = false

        // No content observer is registered while this worker runs, so photos taken during a
        // scan wouldn't re-trigger us. Keep draining until a query comes back empty.
        drain@ while (true) {
            val bursts = queryCameraPhotoBurstsSince(settingsRepository.getLastProcessedMediaStoreId())
            if (bursts.isEmpty()) break

            val loggedPhotoIds = recordsRepository.getSourceMediaStoreIds()
            val now = System.currentTimeMillis()

            for (burst in bursts) {
                val burstMaxId = burst.maxOf { it.id }
                val alreadyAdded = burst.any { it.id in loggedPhotoIds }
                // Age guard: a restored/synced gallery can flood MediaStore with old camera
                // photos under fresh ids; don't burn API calls scanning last month's meals.
                val tooOld = burst.all { it.dateTaken > 0 && now - it.dateTaken > SCAN_LOOKBACK.inWholeMilliseconds }
                if (alreadyAdded || tooOld) {
                    settingsRepository.setLastProcessedMediaStoreId(burstMaxId)
                    continue
                }
                if (scansLeft <= 0 || cooldownRemainingMs() > 0) {
                    // Out of budget for this run. The high-water mark stays below this burst,
                    // so a delayed retry picks it up — nothing is dropped.
                    deferred = true
                    break@drain
                }
                scansLeft--
                settingsRepository.setLastPhotoRecognitionRequestEpochMs(System.currentTimeMillis())
                val best = pickBestOf(burst)
                showStatusNotification(applicationContext, applicationContext.getString(R.string.photo_recognition_scanning))
                runCatching { autoPhotoRecognitionUseCase.execute(best.uri) }
                    .onFailure { Log.e(TAG, "Recognition failed for ${best.uri}", it) }
                cancelStatusNotification(applicationContext)
                settingsRepository.setLastProcessedMediaStoreId(burstMaxId)
            }
        }

        if (deferred) {
            reenqueueDelayed(applicationContext, maxOf(cooldownRemainingMs(), CONTINUE_DELAY_MS))
        } else {
            reenqueue(applicationContext)
        }
        return Result.success()
    }

    private fun cooldownRemainingMs(): Long {
        val elapsed = System.currentTimeMillis() - settingsRepository.getLastPhotoRecognitionRequestEpochMs()
        return (SCAN_COOLDOWN.inWholeMilliseconds - elapsed).coerceAtLeast(0L)
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

    /**
     * Camera photos with id > [lastId], grouped into bursts (photos taken within
     * [BURST_WINDOW_MS] of each other), oldest burst first.
     */
    private fun queryCameraPhotoBurstsSince(lastId: Long): List<List<Candidate>> {
        val projection = arrayOf(
            MediaStore.Images.Media._ID,
            MediaStore.Images.Media.MIME_TYPE,
            MediaStore.Images.Media.WIDTH,
            MediaStore.Images.Media.HEIGHT,
            MediaStore.Images.Media.DATE_TAKEN,
            MediaStore.Images.Media.ORIENTATION,
        )
        val selection = "${MediaStore.Images.Media._ID} > ? AND ${MediaStore.Images.Media.BUCKET_DISPLAY_NAME} = ?"
        val selectionArgs = arrayOf(lastId.toString(), CAMERA_BUCKET_NAME)
        val sortOrder = "${MediaStore.Images.Media._ID} ASC"

        val candidates = runCatching {
            applicationContext.contentResolver.query(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                projection,
                selection,
                selectionArgs,
                sortOrder,
            )?.use { cursor ->
                val idCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
                val mimeCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.MIME_TYPE)
                val widthCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.WIDTH)
                val heightCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.HEIGHT)
                val dateCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_TAKEN)
                val orientationCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.ORIENTATION)
                val results = mutableListOf<Candidate>()
                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idCol)
                    results.add(
                        Candidate(
                            id = id,
                            uri = ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id),
                            mimeType = cursor.getString(mimeCol) ?: "",
                            pixels = cursor.getLong(widthCol) * cursor.getLong(heightCol),
                            dateTaken = cursor.getLong(dateCol),
                            orientation = cursor.getInt(orientationCol),
                        )
                    )
                }
                results
            }
        }.onFailure { Log.e(TAG, "MediaStore query failed", it) }
            .getOrNull() ?: return emptyList()

        val bursts = mutableListOf<MutableList<Candidate>>()
        for (candidate in candidates) {
            val lastBurst = bursts.lastOrNull()
            val previous = lastBurst?.last()
            // DATE_TAKEN comes from EXIF and can be missing (0) or out of order relative to
            // ids; only group when both timestamps are sane, otherwise start a new burst.
            val sameBurst = previous != null &&
                previous.dateTaken > 0 && candidate.dateTaken > 0 &&
                (candidate.dateTaken - previous.dateTaken) in 0..BURST_WINDOW_MS
            if (sameBurst) {
                lastBurst.add(candidate)
            } else {
                bursts.add(mutableListOf(candidate))
            }
        }
        return bursts
    }

    /**
     * Best candidate of a burst: prefer standard formats (JPEG/HEIC) over RAW, then upright
     * (orientation == 0), then largest pixel area (handles cover-crop variants).
     */
    private fun pickBestOf(burst: List<Candidate>): Candidate {
        val preferredMimeTypes = setOf("image/jpeg", "image/jpg", "image/heic", "image/heif", "image/png", "image/webp")
        val preferred = burst.filter { it.mimeType in preferredMimeTypes }.ifEmpty { burst }
        val upright = preferred.filter { it.orientation == 0 }.ifEmpty { preferred }
        return upright.maxByOrNull { it.pixels } ?: upright.last()
    }

    companion object {
        private const val TAG = "PhotoMonitorWorker"
        private const val CAMERA_BUCKET_NAME = "Camera"
        private const val BURST_WINDOW_MS = 3_000L

        /** Max ChatGPT scans per worker run; the rest is picked up by a delayed follow-up run. */
        private const val MAX_SCANS_PER_RUN = 5

        /** Delay before a follow-up run when a run stops with unscanned photos remaining. */
        private const val CONTINUE_DELAY_MS = 30_000L

        /** Minimum spacing between ChatGPT requests (trigger-storm guard). */
        private val SCAN_COOLDOWN = 10.seconds

        /** Photos older than this are never auto-scanned (gallery restore/sync flood guard). */
        private val SCAN_LOOKBACK = 48.hours

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

        private fun contentTriggerRequest(): OneTimeWorkRequest =
            OneTimeWorkRequestBuilder<PhotoMonitorWorker>()
                .setConstraints(
                    Constraints.Builder()
                        .addContentUriTrigger(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, true)
                        // Fire promptly after a new photo instead of letting the system batch
                        // content changes for minutes.
                        .setTriggerContentUpdateDelay(1, TimeUnit.SECONDS)
                        .setTriggerContentMaxDelay(15, TimeUnit.SECONDS)
                        .build()
                )
                .build()

        /** Safe to call any time (e.g. app start): keeps an already-armed monitor untouched. */
        fun enqueue(context: Context) {
            WorkManager.getInstance(context).enqueueUniqueWork(
                WORK_NAME,
                ExistingWorkPolicy.KEEP,
                contentTriggerRequest(),
            )
        }

        fun cancel(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
        }

        private fun reenqueue(context: Context) {
            WorkManager.getInstance(context).enqueueUniqueWork(
                WORK_NAME,
                ExistingWorkPolicy.REPLACE,
                contentTriggerRequest(),
            )
        }

        /** Time-based follow-up (no content trigger) so leftover photos don't need another MediaStore change to get scanned. */
        private fun reenqueueDelayed(context: Context, delayMs: Long) {
            val request = OneTimeWorkRequestBuilder<PhotoMonitorWorker>()
                .setInitialDelay(delayMs, TimeUnit.MILLISECONDS)
                .build()
            WorkManager.getInstance(context).enqueueUniqueWork(
                WORK_NAME,
                ExistingWorkPolicy.REPLACE,
                request,
            )
        }
    }
}
