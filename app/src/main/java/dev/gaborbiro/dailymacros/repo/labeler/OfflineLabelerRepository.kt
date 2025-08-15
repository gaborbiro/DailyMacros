package dev.gaborbiro.dailymacros.repo.labeler

import kotlinx.coroutines.flow.Flow

interface OfflineLabelerRepository {

    suspend fun labelImage(image: String): Flow<List<String>>
}
