package dev.gaborbiro.dailymacros.features.overview.useCases

import dev.gaborbiro.dailymacros.data.records.domain.RecordsRepository
import dev.gaborbiro.dailymacros.data.records.domain.model.Record
import dev.gaborbiro.dailymacros.features.overview.model.NutrientProgress
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

internal class ObserveNutrientProgressUseCase(
    private val recordsRepository: RecordsRepository,
    private val overviewUIMapper: OverviewUIMapper,
) {

    suspend fun execute(): Flow<Pair<NutrientProgress, NutrientProgress>> {
        val today = LocalDateTime.now().truncatedTo(ChronoUnit.DAYS)
        val todaysRecords = recordsRepository
            .getRecordsFlow(since = today)
            .distinctUntilChanged()
        val yesterday = LocalDateTime.now().minusDays(1).truncatedTo(ChronoUnit.DAYS)
        val yesterdaysRecords: Flow<List<Record>> = recordsRepository
            .getRecordsFlow(since = yesterday, until = today)
            .distinctUntilChanged()
        return combine(todaysRecords, yesterdaysRecords) { a, b ->
            overviewUIMapper.mapNutrientProgress(a) to overviewUIMapper.mapNutrientProgress(b)
        }
    }
}
