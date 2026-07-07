package dev.gaborbiro.dailymacros.features.shared.photodiary

import dev.gaborbiro.dailymacros.data.image.domain.ImageStore
import javax.inject.Inject

class DenyAutoPhotoEntryUseCase @Inject constructor(
    private val imageStore: ImageStore,
) {

    suspend fun execute(imageFilename: String) {
        imageStore.delete(imageFilename)
    }
}
