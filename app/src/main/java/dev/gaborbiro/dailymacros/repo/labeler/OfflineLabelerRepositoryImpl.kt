package dev.gaborbiro.dailymacros.repo.labeler

import dev.gaborbiro.dailymacros.data.mlkit.MLKitStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import dev.gaborbiro.dailymacros.data.image.ImageStore

internal class OfflineLabelerRepositoryImpl(
    private val mlKitStore: MLKitStore,
) : OfflineLabelerRepository {

    override suspend fun labelImage(image: String): Flow<List<String>> = channelFlow {
        val fastDeferred = async(Dispatchers.IO) {
            mlKitStore.fastTitleFromImage(image).joinToString { it.first }
    }
        val slowDeferred = async(Dispatchers.IO) {
            mlKitStore.titleFromImage(image)
        }

        val fast = fastDeferred.await()
        send(listOf(fast))

        val slow = slowDeferred.await()
        send(listOf(fast, slow))
    }
}
