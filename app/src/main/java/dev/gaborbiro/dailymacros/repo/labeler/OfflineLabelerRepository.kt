package dev.gaborbiro.dailymacros.repo.labeler

interface OfflineLabelerRepository {

    fun labelImage(image: String): List<Pair<String, Float>>
}
