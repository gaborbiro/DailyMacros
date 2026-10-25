package dev.gaborbiro.dailymacros.features.shared.healthconnect

import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.records.MealType
import androidx.health.connect.client.records.NutritionRecord
import dev.gaborbiro.dailymacros.repositories.common.model.Nutrients
import dev.gaborbiro.dailymacros.repositories.common.model.TopContributors
import dev.gaborbiro.dailymacros.repositories.common.model.toZonedInstant
import dev.gaborbiro.dailymacros.repositories.records.domain.RecordsRepository
import dev.gaborbiro.dailymacros.repositories.records.domain.model.Record
import dev.gaborbiro.dailymacros.repositories.records.domain.model.Template
import dev.gaborbiro.dailymacros.repositories.settings.domain.SettingsRepository
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test
import java.time.ZoneId
import java.time.ZonedDateTime
import kotlin.time.Instant

class SyncRecordsToHealthConnectUseCaseTest {

    private val since = ZonedDateTime.of(2024, 1, 1, 0, 0, 0, 0, ZoneId.of("UTC"))

    private fun record(recordId: Long, calories: Int?) = Record(
        recordId = recordId,
        timestamp = since.plusHours(recordId).toZonedInstant(),
        template = Template(
            dbId = recordId,
            imageFilenames = emptyList(),
            isRepresentativeOfMealByImageIndex = emptyList(),
            name = "Meal $recordId",
            description = "",
            parentTemplateId = null,
            createdAtEpochMs = 0L,
            updatedAtEpochMs = 0L,
            isPending = false,
            nutrients = Nutrients(calories = calories, protein = 10f, salt = 2.5f),
            notes = "",
            mealComponents = emptyList(),
            topContributors = TopContributors(),
            quickPickOverride = null,
        ),
    )

    private class FakeRecordsRepository(
        private val records: List<Record>,
    ) : RecordsRepository by unimplemented() {

        override suspend fun getRecords(since: Instant?): List<Record> = records
    }

    private class FakeSettingsRepository(
        private val diaryDayStartHour: Int = 0,
    ) : SettingsRepository by unimplemented() {

        override fun getDiaryDayStartHour(): Int = diaryDayStartHour
    }

    private fun useCase(provider: HealthConnectClientProvider, records: List<Record> = emptyList()): SyncRecordsToHealthConnectUseCase {
        val getStatus = GetHealthConnectStatusUseCase(provider)
        return SyncRecordsToHealthConnectUseCase(
            clientProvider = provider,
            recordsRepository = FakeRecordsRepository(records),
            settingsRepository = FakeSettingsRepository(),
            getHealthConnectStatusUseCase = getStatus,
            hasHealthConnectPermissionUseCase = HasHealthConnectPermissionUseCase(provider, getStatus),
        )
    }

    @Test
    fun `NotAvailable without creating a client when Health Connect needs installing or updating`() = runBlocking {
        val provider = FakeHealthConnectClientProvider(
            sdkStatus = HealthConnectClient.SDK_UNAVAILABLE_PROVIDER_UPDATE_REQUIRED,
        )

        assertEquals(HealthConnectSyncResult.NotAvailable, useCase(provider).execute(since))
        assertFalse(provider.clientRequested)
    }

    @Test
    fun `NotAvailable when the device doesn't support Health Connect`() = runBlocking {
        val provider = FakeHealthConnectClientProvider(sdkStatus = HealthConnectClient.SDK_UNAVAILABLE)

        assertEquals(HealthConnectSyncResult.NotAvailable, useCase(provider).execute(since))
        assertFalse(provider.clientRequested)
    }

    @Test
    fun `PermissionRequired and nothing written when the permission is missing`() = runBlocking {
        val client = FakeHealthConnectClient(grantedPermissions = emptySet())
        val provider = FakeHealthConnectClientProvider(client = client)

        val result = useCase(provider, records = listOf(record(recordId = 1, calories = 500))).execute(since)

        assertEquals(HealthConnectSyncResult.PermissionRequired, result)
        assertEquals(emptyList<Any>(), client.insertedRecords)
    }

    @Test
    fun `writes only records with calories, keyed by their record id`() = runBlocking {
        val client = FakeHealthConnectClient()
        val provider = FakeHealthConnectClientProvider(client = client)
        val records = listOf(
            record(recordId = 1, calories = 500),
            record(recordId = 2, calories = null),
            record(recordId = 3, calories = 300),
        )

        val result = useCase(provider, records).execute(since)

        assertEquals(HealthConnectSyncResult.Success(count = 2), result)
        val written = client.insertedRecords.map { it as NutritionRecord }
        assertEquals(
            listOf("dailymacros-record-1", "dailymacros-record-3"),
            written.map { it.metadata.clientRecordId },
        )
        assertEquals(500.0, written[0].energy!!.inKilocalories, 0.001)
        assertEquals(1.0, written[0].sodium!!.inGrams, 0.001)
    }

    @Test
    fun `sets the meal type from the logged time`() = runBlocking {
        val client = FakeHealthConnectClient()
        val provider = FakeHealthConnectClientProvider(client = client)
        val records = listOf(
            record(recordId = 9, calories = 400), // 09:00
            record(recordId = 19, calories = 700), // 19:00
        )

        useCase(provider, records).execute(since)

        assertEquals(
            listOf(MealType.MEAL_TYPE_BREAKFAST, MealType.MEAL_TYPE_DINNER),
            client.insertedRecords.map { (it as NutritionRecord).mealType },
        )
    }

    @Test
    fun `Success with zero and no write when nothing has calories`() = runBlocking {
        val client = FakeHealthConnectClient()
        val provider = FakeHealthConnectClientProvider(client = client)

        val result = useCase(provider, records = listOf(record(recordId = 1, calories = null))).execute(since)

        assertEquals(HealthConnectSyncResult.Success(count = 0), result)
        assertEquals(emptyList<Any>(), client.insertedRecords)
    }

    @Test
    fun `Error carries the message when the write fails`() = runBlocking {
        val client = FakeHealthConnectClient(insertFailure = IllegalStateException("quota exceeded"))
        val provider = FakeHealthConnectClientProvider(client = client)

        val result = useCase(provider, records = listOf(record(recordId = 1, calories = 500))).execute(since)

        assertEquals(HealthConnectSyncResult.Error("quota exceeded"), result)
    }
}
