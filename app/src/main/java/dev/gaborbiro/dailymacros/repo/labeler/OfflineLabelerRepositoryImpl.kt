package dev.gaborbiro.dailymacros.repo.labeler

import dev.gaborbiro.dailymacros.data.image.ImageStore

internal class OfflineLabelerRepositoryImpl(
    private val imageStore: ImageStore,
) : OfflineLabelerRepository {

    override fun labelImage(image: String): List<Pair<String, Float>> {
        return emptyList()
    }
}
