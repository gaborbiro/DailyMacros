package dev.gaborbiro.dailymacros.features.modal.usecase

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import dev.gaborbiro.dailymacros.data.image.DefaultFoodPicExt
import dev.gaborbiro.dailymacros.data.image.domain.ImageStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import javax.inject.Inject

/**
 * Copies an in-app food photo into shared device storage via [MediaStore] so it survives
 * uninstall and appears in gallery apps.
 */
class ExportImageToGalleryUseCase @Inject constructor(
    @ApplicationContext private val appContext: Context,
    private val imageStore: ImageStore,
) {

    suspend fun execute(imageFilename: String): Uri = withContext(Dispatchers.IO) {
        val displayName = galleryDisplayName(imageFilename)
        val contentValues = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, displayName)
            put(MediaStore.Images.Media.MIME_TYPE, MIME_TYPE)
            put(
                MediaStore.Images.Media.RELATIVE_PATH,
                "${Environment.DIRECTORY_PICTURES}/$GALLERY_ALBUM_FOLDER",
            )
            put(MediaStore.Images.Media.IS_PENDING, 1)
        }

        val resolver = appContext.contentResolver
        val collection = MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        val destinationUri = resolver.insert(collection, contentValues)
            ?: throw IOException("Could not create gallery entry for $displayName")

        try {
            imageStore.open(imageFilename, thumbnail = false).use { input ->
                resolver.openOutputStream(destinationUri)?.use { output ->
                    input.copyTo(output)
                } ?: throw IOException("Could not write image to gallery")
            }
            ContentValues().apply {
                put(MediaStore.Images.Media.IS_PENDING, 0)
            }.let { readyValues ->
                resolver.update(destinationUri, readyValues, null, null)
            }
        } catch (e: Exception) {
            resolver.delete(destinationUri, null, null)
            throw e
        }

        destinationUri
    }

    companion object {
        /** Folder under [Environment.DIRECTORY_PICTURES] shown in gallery apps. */
        const val GALLERY_ALBUM_FOLDER = "Daily Macros"

        private const val MIME_TYPE = "image/jpeg"

        internal fun galleryDisplayName(imageFilename: String): String {
            val sanitizedBase = imageFilename
                .substringBeforeLast('.')
                .replace(':', '-')
                .replace('/', '-')
                .ifBlank { "daily-macros-photo" }
            return "$sanitizedBase.$DefaultFoodPicExt"
        }
    }
}
