package dev.gaborbiro.dailymacros.repositories.records.domain

import dev.gaborbiro.dailymacros.repositories.records.domain.model.variability.MealVariabilityPersistedProfile
import dev.gaborbiro.dailymacros.repositories.records.domain.model.variability.MealVariabilityProfileSnapshot

interface VariabilityRepository {

    suspend fun getLatestProfile(): MealVariabilityProfileSnapshot?

    suspend fun replaceProfile(profile: MealVariabilityPersistedProfile)

    /** Parses [profileJson] into domain and persists normalized rows + snapshot. */
    suspend fun replaceProfileFromModelJson(profileJson: String, minedAtEpochMs: Long)
}
