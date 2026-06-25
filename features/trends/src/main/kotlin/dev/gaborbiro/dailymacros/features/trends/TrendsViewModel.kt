package dev.gaborbiro.dailymacros.features.trends

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.gaborbiro.dailymacros.features.shared.diaryDayStartTime
import dev.gaborbiro.dailymacros.features.shared.diaryDayWindowStart
import dev.gaborbiro.dailymacros.features.shared.logicalDiaryDate
import dev.gaborbiro.dailymacros.features.shared.logicalDiaryToday
import dev.gaborbiro.dailymacros.features.trends.model.DayQualifier
import dev.gaborbiro.dailymacros.features.trends.model.Timescale
import dev.gaborbiro.dailymacros.features.trends.model.TrendsSettingsUIModel
import dev.gaborbiro.dailymacros.features.trends.model.TrendsUiState
import dev.gaborbiro.dailymacros.features.trends.model.TrendsUiUpdates
import dev.gaborbiro.dailymacros.repositories.chatgpt.di.ForJsonBodyChatGpt
import dev.gaborbiro.dailymacros.repositories.chatgpt.domain.ChatGPTRepository
import dev.gaborbiro.dailymacros.repositories.chatgpt.domain.model.WeeklyInsightsRequest
import dev.gaborbiro.dailymacros.repositories.records.domain.RecordsRepository
import dev.gaborbiro.dailymacros.repositories.records.domain.model.Record
import dev.gaborbiro.dailymacros.repositories.settings.domain.SettingsRepository
import dev.gaborbiro.dailymacros.repositories.settings.domain.model.Target
import dev.gaborbiro.dailymacros.repositories.settings.domain.model.Targets
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.time.temporal.WeekFields
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class TrendsViewModel @Inject constructor(
    application: Application,
    private val recordsRepository: RecordsRepository,
    private val settingsRepository: SettingsRepository,
    private val preferences: TrendsPreferences,
    private val mapper: TrendsUiMapper,
    @ForJsonBodyChatGpt private val chatGPTRepository: ChatGPTRepository,
) : AndroidViewModel(application) {

    private var recordsJob: Job

    private val _uiState = MutableStateFlow(TrendsUiState())
    val uiState: StateFlow<TrendsUiState> = _uiState.asStateFlow()

    private val _uiUpdates = MutableSharedFlow<TrendsUiUpdates>()
    val uiUpdates: SharedFlow<TrendsUiUpdates> = _uiUpdates.asSharedFlow()

    init {
        recordsJob = observeRecords(Timescale.DAYS)
    }

    fun onTimescaleSelected(timescale: Timescale) {
        recordsJob.cancel()
        recordsJob = observeRecords(timescale)
    }

    fun onBackNavigate() {
        viewModelScope.launch {
            _uiUpdates.emit(TrendsUiUpdates.NavigateBack)
        }
    }

    fun onSettingsActionButtonClicked() {
        _uiState.update {
            it.copy(
                settings = TrendsSettingsUIModel.Show(
                    dayQualifier = mapper.map(preferences.dayQualificationMode),
                    qualifiedDaysThreshold = preferences.qualifyingCalorieThreshold,
                )
            )
        }
    }

    fun onSettingsCloseRequested() {
        _uiState.update {
            it.copy(settings = TrendsSettingsUIModel.Hidden)
        }
    }

    fun onDailyTargetsFromTrendsSettingsTapped() {
        _uiState.update {
            it.copy(
                settings = TrendsSettingsUIModel.Hidden,
                showTargetsSettings = true,
            )
        }
    }

    fun onTargetsSettingsCloseRequested() {
        _uiState.update {
            it.copy(showTargetsSettings = false)
        }
    }

    fun onAggregationModeChanged(dayQualifier: DayQualifier, timescale: Timescale) {
        preferences.dayQualificationMode = mapper.map(dayQualifier)
        recordsJob.cancel()
        recordsJob = observeRecords(timescale)
    }

    fun onAggregationThresholdChanged(threshold: Long, timescale: Timescale) {
        preferences.qualifyingCalorieThreshold = threshold
        recordsJob.cancel()
        recordsJob = observeRecords(timescale)
    }

    fun onGetInsightsTapped() {
        viewModelScope.launch {
            _uiState.update { it.copy(insightsLoading = true, insightsError = null) }
            try {
                val zone = ZoneId.systemDefault()
                val dayStart = diaryDayStartTime(settingsRepository.getDiaryDayStartHour())
                val today = logicalDiaryToday(zone, dayStart)
                val weekFields = WeekFields.of(Locale.getDefault())
                val lastCompleteWeekStart = today.with(weekFields.dayOfWeek(), 1).minusWeeks(1)
                val prevWeekStart = lastCompleteWeekStart.minusWeeks(1)
                val since = diaryDayWindowStart(prevWeekStart, dayStart, zone)
                val records = recordsRepository.getRecords(since = since)
                val targets = settingsRepository.getTargets()
                val customizations = settingsRepository.getPromptCustomizations()
                val diary = formatDiary(records, targets, zone, dayStart)
                val result = chatGPTRepository.getWeeklyInsights(WeeklyInsightsRequest(diary, customizations))
                _uiState.update { it.copy(insights = result, insightsLoading = false) }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        insightsLoading = false,
                        insightsError = e.message ?: "Failed to get insights",
                    )
                }
            }
        }
    }

    private fun observeRecords(timescale: Timescale): Job {
        return viewModelScope.launch {
            recordsRepository.observeRecords(null).collect { records ->
                if (records.isEmpty()) {
                    _uiState.update {
                        it.copy(charts = emptyList())
                    }
                } else {
                    val aggregationMode = mapper.map(preferences.dayQualificationMode)
                    val targets = settingsRepository.getTargets()
                    val charts = when (timescale) {
                        Timescale.DAYS -> mapper.mapDaysCharts(records, aggregationMode, targets)
                        Timescale.WEEKS -> mapper.mapWeeksCharts(records, aggregationMode, targets)
                        Timescale.MONTHS -> mapper.mapMonthsCharts(records, aggregationMode, targets)
                    }
                    _uiState.update {
                        it.copy(charts = charts)
                    }
                }
            }
        }
    }

    private fun formatDiary(
        records: List<Record>,
        targets: Targets,
        zone: ZoneId,
        dayStart: LocalTime,
    ): String {
        val sb = StringBuilder()
        val today = logicalDiaryToday(zone, dayStart)
        val weekFields = WeekFields.of(Locale.getDefault())
        val lastCompleteWeekStart = today.with(weekFields.dayOfWeek(), 1).minusWeeks(1)
        val prevWeekStart = lastCompleteWeekStart.minusWeeks(1)

        val targetParts = buildList {
            targets.calories.formatTarget("calories", "kcal")?.let { add(it) }
            targets.protein.formatTarget("protein", "g")?.let { add(it) }
            targets.fat.formatTarget("fat", "g")?.let { add(it) }
            targets.ofWhichSaturated.formatTarget("ofWhichSaturated", "g")?.let { add(it) }
            targets.carbs.formatTarget("carbs", "g")?.let { add(it) }
            targets.ofWhichSugar.formatTarget("ofWhichSugar", "g")?.let { add(it) }
            targets.salt.formatTarget("salt", "g")?.let { add(it) }
            targets.fibre.formatTarget("fibre", "g")?.let { add(it) }
        }
        if (targetParts.isNotEmpty()) {
            sb.appendLine("DAILY TARGETS: ${targetParts.joinToString(", ")}")
            sb.appendLine()
        }

        val byDay = records
            .sortedBy { it.timestamp }
            .groupBy { it.timestamp.logicalDiaryDate(dayStart) }

        val prevWeekDays = byDay.filterKeys { it >= prevWeekStart && it < lastCompleteWeekStart }.toSortedMap()
        val lastWeekDays = byDay.filterKeys { it >= lastCompleteWeekStart }.toSortedMap()

        if (prevWeekDays.isNotEmpty()) {
            sb.appendLine("=== ${formatWeekRange(prevWeekStart)} ===")
            prevWeekDays.forEach { (day, recs) -> appendDay(sb, day, recs, zone) }
            sb.appendLine()
        }
        if (lastWeekDays.isNotEmpty()) {
            sb.appendLine("=== ${formatWeekRange(lastCompleteWeekStart)} ===")
            lastWeekDays.forEach { (day, recs) -> appendDay(sb, day, recs, zone) }
        }

        return sb.toString().trim()
    }

    private fun formatWeekRange(weekStart: LocalDate): String {
        val weekEnd = weekStart.plusDays(6)
        val startMonth = weekStart.month.getDisplayName(TextStyle.SHORT, Locale.getDefault())
        val endMonth = weekEnd.month.getDisplayName(TextStyle.SHORT, Locale.getDefault())
        return if (weekStart.month == weekEnd.month) {
            "${weekStart.dayOfMonth}–${weekEnd.dayOfMonth} $endMonth"
        } else {
            "${weekStart.dayOfMonth} $startMonth – ${weekEnd.dayOfMonth} $endMonth"
        }
    }

    private fun Target.formatTarget(name: String, unit: String): String? {
        if (!enabled) return null
        return when {
            min != null && max != null -> "$name: $min–$max$unit"
            min != null -> "$name: ≥$min$unit"
            max != null -> "$name: ≤$max$unit"
            else -> null
        }
    }

    private fun appendDay(sb: StringBuilder, day: LocalDate, records: List<Record>, zone: ZoneId) {
        val dayFmt = DateTimeFormatter.ofPattern("EEE d MMM", Locale.getDefault())
        val timeFmt = DateTimeFormatter.ofPattern("HH:mm")
        sb.appendLine(day.format(dayFmt))
        records.forEach { record ->
            val time = record.timestamp.withZoneSameInstant(zone).format(timeFmt)
            sb.appendLine("  • ${record.template.name} ($time)")
            record.template.mealComponents.takeIf { it.isNotEmpty() }?.let { components ->
                val ingredientsList = components.joinToString(", ") {
                    buildString {
                        append(it.name)
                        if (it.estimatedAmount.isNotBlank()) append(" ${it.estimatedAmount}")
                    }
                }
                sb.appendLine("    Ingredients: $ingredientsList")
            }
            val n = record.template.nutrients
            val parts = buildList {
                n.calories?.let { add("calories: ${it}kcal") }
                n.protein?.let { add("protein: ${it}g") }
                n.fat?.let { add("fat: ${it}g") }
                n.ofWhichSaturated?.let { add("ofWhichSaturated: ${it}g") }
                n.carbs?.let { add("carbs: ${it}g") }
                n.ofWhichSugar?.let { add("ofWhichSugar: ${it}g") }
                n.ofWhichAddedSugar?.let { add("ofWhichAddedSugar: ${it}g") }
                n.salt?.let { add("salt: ${it}g") }
                n.fibre?.let { add("fibre: ${it}g") }
            }
            if (parts.isNotEmpty()) {
                sb.appendLine("    Nutrients: ${parts.joinToString(", ")}")
            }
        }
    }
}
