package dev.gaborbiro.dailymacros.features.modal.usecase

import dev.gaborbiro.dailymacros.features.modal.model.PastPhoto
import dev.gaborbiro.dailymacros.repositories.records.domain.RecordsRepository
import dev.gaborbiro.dailymacros.repositories.records.domain.model.PhotoUsage
import javax.inject.Inject

/** Every photo used by an earlier meal, once each, however many meals share it. */
class GetPastPhotosUseCase @Inject constructor(
    private val recordsRepository: RecordsRepository,
) {

    suspend fun execute(): List<PastPhoto> =
        recordsRepository.getPhotoUsages()
            .groupBy { it.filename }
            .map { (filename, usages) -> usages.toPastPhoto(filename) }
}

internal fun List<PhotoUsage>.toPastPhoto(filename: String): PastPhoto {
    val mostRecentFirst = sortedByDescending { it.lastUsedEpochMillis }
    val latest = mostRecentFirst.first()
    return PastPhoto(
        filename = filename,
        caption = latest.mealTitle.ifBlank { latest.mealDescription }.trim(),
        mealTitles = mostRecentFirst.map { it.mealTitle.trim() }.filter { it.isNotEmpty() }.distinct(),
        searchText = mostRecentFirst
            .flatMap { listOf(it.mealTitle, it.mealDescription) }
            .filter { it.isNotBlank() }
            .distinct()
            .joinToString("\n")
            .lowercase(),
        isPackagingShot = any { it.isRepresentativeOfMeal == false },
        lastUsedEpochMillis = latest.lastUsedEpochMillis,
    )
}

/**
 * The photos matching [query], best first.
 *
 * Every word of the query must appear somewhere in the titles or descriptions of the meals that
 * used the photo. Photos whose meal titles contain every word come before those that only match on
 * a description; within that, packaging and label shots come first (they are what gets reused,
 * e.g. the same soy milk carton across many breakfasts), then the most recently used.
 */
fun filterPastPhotos(
    photos: List<PastPhoto>,
    query: String,
    excludedFilenames: Collection<String>,
): List<PastPhoto> {
    val words = query.lowercase().split(Regex("\\s+")).filter { it.isNotEmpty() }
    return photos
        .asSequence()
        .filter { it.filename !in excludedFilenames }
        .filter { photo -> words.all { it in photo.searchText } }
        .sortedWith(
            compareBy<PastPhoto> { photo ->
                val titles = photo.mealTitles.joinToString("\n").lowercase()
                if (words.all { it in titles }) 0 else 1
            }
                .thenBy { if (it.isPackagingShot) 0 else 1 }
                .thenByDescending { it.lastUsedEpochMillis }
        )
        .toList()
}
