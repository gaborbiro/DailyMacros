package dev.gaborbiro.dailymacros.features.shared.mealreminders

import android.app.PendingIntent
import android.content.Context
import dev.gaborbiro.dailymacros.features.shared.ModalNavigator
import dev.gaborbiro.dailymacros.repositories.common.model.Nutrients
import dev.gaborbiro.dailymacros.repositories.common.model.TopContributors
import dev.gaborbiro.dailymacros.repositories.common.model.ZonedInstant
import dev.gaborbiro.dailymacros.repositories.records.domain.RecordsRepository
import dev.gaborbiro.dailymacros.repositories.records.domain.model.MealComponent
import dev.gaborbiro.dailymacros.repositories.records.domain.model.Record
import dev.gaborbiro.dailymacros.repositories.records.domain.model.Template
import dev.gaborbiro.dailymacros.repositories.records.domain.model.TemplateImageUpdate
import dev.gaborbiro.dailymacros.repositories.records.domain.model.TemplateToSave
import dev.gaborbiro.dailymacros.repositories.settings.domain.MealReminderRepository
import dev.gaborbiro.dailymacros.repositories.settings.domain.SettingsRepository
import dev.gaborbiro.dailymacros.repositories.settings.domain.model.MealReminderLastPass
import dev.gaborbiro.dailymacros.repositories.settings.domain.model.MealReminderSlotHistory
import dev.gaborbiro.dailymacros.repositories.settings.domain.model.PromptVersion
import dev.gaborbiro.dailymacros.repositories.settings.domain.model.Targets
import kotlinx.coroutines.flow.Flow
import kotlin.time.Instant

internal class FakeMealReminderRepository : MealReminderRepository {

    private var enabled = false
    private var lastPass: MealReminderLastPass? = null

    /** What has been saved, by slot key. */
    val slotHistory = mutableMapOf<String, MealReminderSlotHistory>()

    override fun isEnabled() = enabled

    override fun setEnabled(enabled: Boolean) {
        this.enabled = enabled
    }

    override fun getSlotHistory(slotKeys: Collection<String>) =
        slotKeys.associateWith { slotHistory[it] ?: MealReminderSlotHistory() }

    override fun saveSlotHistory(history: Map<String, MealReminderSlotHistory>) {
        slotHistory.putAll(history)
    }

    override fun getLastPass() = lastPass

    override fun setLastPass(lastPass: MealReminderLastPass) {
        this.lastPass = lastPass
    }
}

/** Serves [records] from [getRecords] and remembers the lookback it was asked for. */
internal class FakeRecordsRepository(private val records: List<Record> = emptyList()) : RecordsRepository {

    var requestedSince: Instant? = null

    override suspend fun getRecords(since: Instant?): List<Record> {
        requestedSince = since
        return records
    }

    override suspend fun getTemplateIdsInSameVariantFamily(templateId: Long): List<Long> = error("unused")
    override suspend fun countTemplates(): Int = error("unused")
    override fun getMostRecentRecord(): Record? = error("unused")
    override suspend fun getQuickPicks(count: Int): List<Template> = error("unused")
    override fun observeQuickPicks(count: Int): Flow<List<Template>> = error("unused")
    override suspend fun getRecordsByTemplate(templateId: Long): List<Record> = error("unused")
    override suspend fun countRecordsForTemplate(templateId: Long): Int = error("unused")
    override fun observeRecords(searchTerm: String?, sinceEpochMillis: Long): Flow<List<Record>> = error("unused")
    override suspend fun get(recordId: Long): Record? = error("unused")
    override fun observe(recordId: Long): Flow<Record?> = error("unused")
    override suspend fun getTemplate(templateId: Long): Template = error("unused")
    override suspend fun saveTemplate(templateToSave: TemplateToSave): Long = error("unused")
    override suspend fun saveRecord(templateId: Long, timestamp: ZonedInstant): Long = error("unused")
    override suspend fun updateRecord(record: Record) = error("unused")
    override suspend fun deleteRecord(recordId: Long): Record = error("unused")
    override suspend fun deleteTemplateIfUnused(templateId: Long, imageToo: Boolean): Pair<Boolean, Boolean> = error("unused")
    override suspend fun updateTemplate(
        templateId: Long,
        name: String?,
        description: String?,
        templateImages: List<TemplateImageUpdate>?,
        nutrients: Pair<Nutrients, TopContributors>?,
        notes: String?,
        mealComponents: List<MealComponent>?,
    ) = error("unused")
    override suspend fun addQuickPickOverride(templateId: Long, type: Template.QuickPickOverride) = error("unused")
    override suspend fun removeQuickPickOverride(templateId: Long) = error("unused")
}

/** Only the diary day start is read by the meal reminder use cases. */
internal class FakeSettingsRepository(private val diaryDayStartHour: Int = 0) : SettingsRepository {
    override fun getDiaryDayStartHour() = diaryDayStartHour

    override fun getTargets(): Targets = error("unused")
    override fun setTargets(targets: Targets) = error("unused")
    override fun setDiaryDayStartHour(hourOfDay: Int) = error("unused")
    override fun getPromptCustomisations(): Map<String, String> = error("unused")
    override fun setPromptCustomisations(values: Map<String, String>) = error("unused")
    override fun clearPromptCustomisations() = error("unused")
    override fun getPromptVersions(type: String): List<PromptVersion> = error("unused")
    override fun savePromptVersion(type: String, customisations: Map<String, String>): PromptVersion = error("unused")
    override fun deletePromptVersion(version: Int) = error("unused")
    override fun getApiKeyOverride(): String? = error("unused")
    override fun setApiKeyOverride(key: String) = error("unused")
    override fun clearApiKeyOverride() = error("unused")
}

/** The notifier only needs a [ModalNavigator] to post; none of these tests post. */
internal object UnusedModalNavigator : ModalNavigator {
    override fun launchToShowRecordImage(context: Context, recordId: Long) = error("unused")
    override fun launchViewRecordDetails(context: Context, recordId: Long) = error("unused")
    override fun viewRecordDetailsPendingIntent(
        context: Context,
        requestCode: Int,
        recordId: Long,
        notificationId: Int,
    ): PendingIntent = error("unused")
    override fun launchToAddRecordFromPhotoRecognition(context: Context, imageFilename: String, recognisedTitle: String) =
        error("unused")
    override fun photoRecognitionDetailsPendingIntent(
        context: Context,
        requestCode: Int,
        imageFilename: String,
        recognisedTitle: String,
        notificationId: Int,
    ): PendingIntent = error("unused")
    override fun addRecordPendingIntent(
        context: Context,
        requestCode: Int,
        mode: ModalNavigator.AddRecordMode,
        notificationId: Int,
    ): PendingIntent = error("unused")
}
