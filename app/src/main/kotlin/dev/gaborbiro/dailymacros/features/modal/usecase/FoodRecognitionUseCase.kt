package dev.gaborbiro.dailymacros.features.modal.usecase

import android.content.Context
import dev.gaborbiro.dailymacros.data.image.domain.ImageStore
import dev.gaborbiro.dailymacros.features.modal.inputStreamToBase64
import dev.gaborbiro.dailymacros.features.modal.model.RecognisedFood
import dev.gaborbiro.dailymacros.repositories.chatgpt.domain.ChatGPTRepository
import dev.gaborbiro.dailymacros.repositories.chatgpt.domain.model.FoodRecognitionRequest
import dev.gaborbiro.dailymacros.repositories.chatgpt.service.model.ChatGPTApiError
import dev.gaborbiro.dailymacros.repositories.chatgpt.toDomainModel
import dev.gaborbiro.dailymacros.util.showTextNotification
import kotlin.random.Random

internal class FoodRecognitionUseCase(
    private val appContext: Context,
    private val imageStore: ImageStore,
    private val chatGPTRepository: ChatGPTRepository,
) {

    private val coverPhotoLock = Any()
    private var lastCoverPhotoSnapshot: CoverPhotoSnapshot? = null

    /**
     * Per-image `coverPhoto` for the next template save, or **null** if nothing should be written
     * (no recognition, or final image list no longer matches the recognized set).
     * Non-null entries are only `true`/`false` when the model returned `cover_photo` for that index.
     */
    fun consumeCoverPhotoFlagsForSave(images: List<String>): List<Boolean?>? {
        synchronized(coverPhotoLock) {
            val snap = lastCoverPhotoSnapshot ?: return null
            lastCoverPhotoSnapshot = null
            return snap.flagsForSave(images)
        }
    }

    fun discardRememberedCoverPhotoFlags() {
        synchronized(coverPhotoLock) {
            lastCoverPhotoSnapshot = null
        }
    }

    suspend fun execute(images: List<String>): RecognisedFood {
        val response = try {
            val base64Images = images.map {
                val inputStream = imageStore.open(it, thumbnail = false)
                inputStreamToBase64(inputStream)
            }
            chatGPTRepository.recogniseFood(
                request = FoodRecognitionRequest(
                    base64Images = base64Images,
                )
            )
        } catch (apiError: ChatGPTApiError) {
            throw apiError
                .toDomainModel()
        }
        val cachedTokens = "Cached tokens: ${response.cachedTokens}"
        appContext.showTextNotification(Random(564).nextLong(), cachedTokens)
        synchronized(coverPhotoLock) {
            lastCoverPhotoSnapshot = CoverPhotoSnapshot(
                images = images.toList(),
                flags = response.coverPhotoByImageIndex,
            )
        }
        return RecognisedFood(
            title = response.title,
            description = response.description,
        )
    }

    private data class CoverPhotoSnapshot(
        val images: List<String>,
        val flags: List<Boolean?>,
    ) {
        fun flagsForSave(images: List<String>): List<Boolean?>? {
            if (images.isEmpty()) return emptyList()
            return when {
                this.images == images -> alignFlags(flags, images.size)
                images.size < this.images.size &&
                    this.images.take(images.size) == images -> alignFlags(flags, images.size)
                images.size > this.images.size &&
                    images.take(this.images.size) == this.images -> alignFlags(flags, this.images.size) +
                    List(images.size - this.images.size) { null }
                else -> null
            }
        }

        private fun alignFlags(flags: List<Boolean?>, size: Int): List<Boolean?> =
            List(size) { i -> flags.getOrNull(i) }
    }
}
