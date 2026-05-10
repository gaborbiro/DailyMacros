package dev.gaborbiro.dailymacros.features.overview.usecase

/** Chooses the lower bound passed to [observeRecords] for overview timeline vs search. */
internal class ResolveOverviewObserveSinceEpochMillisUseCase {

    fun execute(searchBlank: Boolean, windowStartEpochMillis: Long): Long =
        if (searchBlank) windowStartEpochMillis else 0L
}
