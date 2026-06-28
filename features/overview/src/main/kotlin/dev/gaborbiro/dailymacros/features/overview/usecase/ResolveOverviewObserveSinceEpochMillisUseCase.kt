package dev.gaborbiro.dailymacros.features.overview.usecase

import javax.inject.Inject

/** Chooses the lower bound passed to [observeRecords] for overview timeline vs search. */
class ResolveOverviewObserveSinceEpochMillisUseCase @Inject constructor() {

    fun execute(searchBlank: Boolean, windowStartEpochMillis: Long): Long =
        if (searchBlank) windowStartEpochMillis else 0L
}
