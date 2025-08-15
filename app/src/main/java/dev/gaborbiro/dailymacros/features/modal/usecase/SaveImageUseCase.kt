package dev.gaborbiro.dailymacros.features.modal.usecase

import android.content.Context
import android.graphics.ImageDecoder
import android.net.Uri
import dev.gaborbiro.dailymacros.FoodPicMaxSize
import dev.gaborbiro.dailymacros.generateFoodPicFilename
import dev.gaborbiro.dailymacros.data.bitmap.ImageStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

internal class SaveImageUseCase(
    private val appContext: Context,
    private val imageStore: ImageStore,
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
            filename
        }
    }
}
