package dev.gaborbiro.dailymacros.repositories.records

import dev.gaborbiro.dailymacros.data.db.VariabilityDao
import dev.gaborbiro.dailymacros.repositories.records.domain.VariabilityRepository
import dev.gaborbiro.dailymacros.repositories.records.domain.model.variability.MealVariabilityPersistedProfile
import dev.gaborbiro.dailymacros.repositories.records.domain.model.variability.MealVariabilityProfileSnapshot

class VariabilityRepositoryImpl(
    private val variabilityDao: VariabilityDao,
    private val profileMapper: VariabilityProfileMapper,
) : VariabilityRepository {

    override suspend fun getLatestProfile(): MealVariabilityProfileSnapshot? {
        val row = variabilityDao.getLatestSnapshot() ?: return null
        return MealVariabilityProfileSnapshot(
            minedAtEpochMs = row.minedAtEpochMs,
            profileJson = row.profileJson,
        )
    }

    override suspend fun replaceProfile(profile: MealVariabilityPersistedProfile) {
        val (snapshot, inserts) = profileMapper.toSnapshotAndInserts(profile)
        variabilityDao.replaceEntireProfile(snapshot, inserts)
    }

    override suspend fun replaceProfileFromModelJson(profileJson: String, minedAtEpochMs: Long) {
        replaceProfile(profileMapper.parseProfileJson(profileJson, minedAtEpochMs))
    }
}
