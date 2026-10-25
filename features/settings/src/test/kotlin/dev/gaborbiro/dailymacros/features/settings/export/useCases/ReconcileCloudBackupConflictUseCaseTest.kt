package dev.gaborbiro.dailymacros.features.settings.export.useCases

import dev.gaborbiro.dailymacros.repositories.backup.domain.model.DriveBackupInfo
import dev.gaborbiro.dailymacros.repositories.settings.domain.SettingsRepository
import dev.gaborbiro.dailymacros.repositories.settings.domain.model.PromptVersion
import dev.gaborbiro.dailymacros.repositories.settings.domain.model.Targets
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ReconcileCloudBackupConflictUseCaseTest {

    private val settingsRepository = FakeSettingsRepository()
    private val useCase = ReconcileCloudBackupConflictUseCase(settingsRepository)

    private fun driveInfo(modifiedTimeMs: Long, writtenByThisInstall: Boolean) = DriveBackupInfo(
        fileId = "id",
        modifiedTimeMs = modifiedTimeMs,
        sizeBytes = 1L,
        writtenByThisInstall = writtenByThisInstall,
    )

    @Test
    fun `no cloud backup is not a conflict`() {
        assertFalse(useCase.execute(null))
    }

    @Test
    fun `cloud backup not newer than the last sync is not a conflict`() {
        settingsRepository.lastSynced = 1_000L

        assertFalse(useCase.execute(driveInfo(modifiedTimeMs = 1_000L, writtenByThisInstall = false)))
    }

    @Test
    fun `newer cloud backup from another install is a conflict and leaves the last sync untouched`() {
        settingsRepository.lastSynced = 1_000L

        assertTrue(useCase.execute(driveInfo(modifiedTimeMs = 2_000L, writtenByThisInstall = false)))
        assertEquals(1_000L, settingsRepository.lastSynced)
    }

    @Test
    fun `any cloud backup from another install is a conflict when this install never synced`() {
        assertTrue(useCase.execute(driveInfo(modifiedTimeMs = 2_000L, writtenByThisInstall = false)))
    }

    @Test
    fun `newer cloud backup written by this install is adopted instead of reported as a conflict`() {
        settingsRepository.lastSynced = 1_000L

        assertFalse(useCase.execute(driveInfo(modifiedTimeMs = 2_000L, writtenByThisInstall = true)))
        assertEquals(2_000L, settingsRepository.lastSynced)
    }

    private class FakeSettingsRepository : SettingsRepository {
        var lastSynced: Long? = null

        override fun getLastSyncedEpochMs(): Long? = lastSynced
        override fun setLastSyncedEpochMs(epochMs: Long?) {
            lastSynced = epochMs
        }

        override fun getTargets(): Targets = TODO()
        override fun setTargets(targets: Targets) = TODO()
        override fun getDiaryDayStartHour(): Int = TODO()
        override fun setDiaryDayStartHour(hourOfDay: Int) = TODO()
        override fun getPromptCustomisations(): Map<String, String> = TODO()
        override fun setPromptCustomisations(values: Map<String, String>) = TODO()
        override fun clearPromptCustomisations() = TODO()
        override fun getPromptVersions(type: String): List<PromptVersion> = TODO()
        override fun savePromptVersion(type: String, customisations: Map<String, String>): PromptVersion = TODO()
        override fun deletePromptVersion(version: Int) = TODO()
        override fun getApiKeyOverride(): String? = TODO()
        override fun setApiKeyOverride(key: String) = TODO()
        override fun clearApiKeyOverride() = TODO()
    }
}
