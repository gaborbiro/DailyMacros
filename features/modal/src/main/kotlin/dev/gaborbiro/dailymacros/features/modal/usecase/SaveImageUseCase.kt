package dev.gaborbiro.dailymacros.features.modal.usecase

import android.content.Context
import android.graphics.ImageDecoder
import android.net.Uri
import android.provider.MediaStore
import dev.gaborbiro.dailymacros.data.image.FoodPicMaxSize
import dev.gaborbiro.dailymacros.data.image.generateFoodPicFilename
import dev.gaborbiro.dailymacros.data.image.domain.ImageStore
import dev.gaborbiro.dailymacros.repositories.settings.domain.SettingsRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

class SaveImageUseCase @Inject constructor(
    @ApplicationContext private val appContext: Context,
    private val imageStore: ImageStore,
    private val settingsRepository: SettingsRepository,
) {

    suspend fun execute(uri: Uri): String {
        return withContext(Dispatchers.IO) {
            val source = ImageDecoder.createSource(
                appContext.contentResolver,
                uri
            )
            val bitmap = ImageDecoder.decodeBitmap(
                source
            ) { decoder, imageInfo, _ ->
                val size = imageInfo.size           // android.util.Size
                val w = size.width
                val h = size.height
                val scale = FoodPicMaxSize / maxOf(w, h).toFloat()
                if (scale < 1f) {
                    decoder.setTargetSize((w * scale).toInt(), (h * scale).toInt())
                }
                decoder.isMutableRequired = false
            }
            val filename = generateFoodPicFilename()
            imageStore.write(filename, bitmap)
            markAsManuallyAdded(uri)
            filename
        }
    }

    private fun markAsManuallyAdded(uri: Uri) {
        val mediaStoreId = mediaStoreId(uri) ?: return
        settingsRepository.setManuallyAddedMediaStoreIds(
            settingsRepository.getManuallyAddedMediaStoreIds() + mediaStoreId
        )
    }

    /**
     * Best-effort MediaStore id extraction. Covers direct MediaStore uris
     * (content://media/external/images/media/123) and photo-picker uris for local images
     * (content://media/picker/0/…/media/123). Cloud picker items and uris from other
     * providers have no local MediaStore id and return null.
     */
    private fun mediaStoreId(uri: Uri): Long? =
        uri.takeIf { it.authority == MediaStore.AUTHORITY }
            ?.lastPathSegment
            ?.toLongOrNull()
}
