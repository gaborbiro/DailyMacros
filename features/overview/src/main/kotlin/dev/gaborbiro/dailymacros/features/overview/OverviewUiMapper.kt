package dev.gaborbiro.dailymacros.features.overview

import android.icu.text.DecimalFormat
import dev.gaborbiro.dailymacros.features.shared.TemplateUiMapper
import dev.gaborbiro.dailymacros.features.shared.RecordsUiMapper
import dev.gaborbiro.dailymacros.features.common.utils.diaryDayStartTime
import dev.gaborbiro.dailymacros.features.common.utils.diaryDayWindowStart
import dev.gaborbiro.dailymacros.features.common.utils.logicalDiaryDate
import dev.gaborbiro.dailymacros.features.common.utils.logicalDiaryToday
import dev.gaborbiro.dailymacros.features.overview.model.DailySummaryEntry
import dev.gaborbiro.dailymacros.features.overview.model.ChangeDirection
import dev.gaborbiro.dailymacros.features.overview.model.ChangeIndicator
import dev.gaborbiro.dailymacros.features.shared.model.ListUiModelBase
import dev.gaborbiro.dailymacros.features.overview.model.ListUiModelDailySummary
import dev.gaborbiro.dailymacros.features.overview.model.ListUiModelSetTargetsCta
import dev.gaborbiro.dailymacros.features.overview.model.ListUiModelWeeklySummary
import dev.gaborbiro.dailymacros.features.overview.model.NutrientSummaryStatEntry
import dev.gaborbiro.dailymacros.features.shared.model.TravelDay
import dev.gaborbiro.dailymacros.repositories.records.domain.model.Record
import dev.gaborbiro.dailymacros.repositories.settings.domain.SettingsRepository
import dev.gaborbiro.dailymacros.repositories.settings.domain.model.Target
import dev.gaborbiro.dailymacros.repositories.settings.domain.model.Targets
import java.time.Duration
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.WeekFields
import java.util.Locale
import kotlin.math.absoluteValue
import kotlin.math.roundToInt
import javax.inject.Inject

class OverviewUiMapper @Inject constructor(
    private val recordsUiMapper: RecordsUiMapper,
    private val templateUiMapper: TemplateUiMapper,
    private val settingsRepository: SettingsRepository,
) {

    fun mapSearchResults(
        records: List<Record>,
    ) = records
        .map(recordsUiMapper::map)
        .reversed()

    fun map(
        records: List<Record>,
        targets: Targets,
    ): List<ListUiModelBase> {
        val weekFields = WeekFields.of(Locale.getDefault())
        val firstDayOfWeek = weekFields.firstDayOfWeek
        val result = mutableListOf<ListUiModelBase>()
        val currentWeek = mutableListOf<TravelDay>()
        var previousWeek: List<TravelDay>? = null
        val dayStart = diaryDayStartTime(settingsRepository.getDiaryDayStartHour())
        val today = logicalDiaryToday(ZoneId.systemDefault(), dayStart)

        val grouped = records.groupByWallClockDay(dayStart)

        grouped.forEachIndexed { index, travelDay ->
            val previousTravelDay = grouped.getOrNull(index - 1)
            currentWeek += travelDay
            result += travelDay.records.map { recordsUiMapper.map(record = it, timeOnly = true) }
            result += mapDailyNutrientProgressTable(travelDay, previousTravelDay, targets)

            val lookAhead = grouped.getOrNull(index + 1)
            val weekEnded =
                // Case 1: the travel day is the last day of its week AND it’s fully past today
                (travelDay.day.dayOfWeek == firstDayOfWeek.minus(1) && travelDay.day.isBefore(today)) ||
                        // Case 2: next record belongs to a new week
                        (lookAhead != null && lookAhead.day.get(weekFields.weekOfWeekBasedYear()) != travelDay.day.get(weekFields.weekOfWeekBasedYear())) ||
                        // Case 3: last record, and that week has ended (not current ongoing week)
                        (lookAhead == null && travelDay.day.isBefore(today.with(firstDayOfWeek)))

            if (weekEnded) {
                mapWeeklySummary(
                    week = currentWeek,
                    previousWeek = previousWeek,
                    targets = targets,
                )?.let { result += it }
                previousWeek = currentWeek.toList()
                currentWeek.clear()
            }
        }

        return result.reversed()
    }

    private fun mapDailyNutrientProgressTable(
        day: TravelDay,
        previousDay: TravelDay?,
        targets: Targets,
    ): ListUiModelDailySummary {
        val records = day.records
        val totalCalories = records.sumOf { it.template.nutrients.calories ?: 0 }
        val totalProtein =
            records.sumOf { it.template.nutrients.protein?.toDouble() ?: 0.0 }.toFloat()
        val totalCarbs =
            records.sumOf { it.template.nutrients.carbs?.toDouble() ?: 0.0 }.toFloat()
        val totalSugar =
            records.sumOf { it.template.nutrients.ofWhichSugar?.toDouble() ?: 0.0 }.toFloat()
        val totalFat = records.sumOf { it.template.nutrients.fat?.toDouble() ?: 0.0 }.toFloat()
        val totalSaturated =
            records.sumOf { it.template.nutrients.ofWhichSaturated?.toDouble() ?: 0.0 }.toFloat()
        val totalSalt = records.sumOf { it.template.nutrients.salt?.toDouble() ?: 0.0 }.toFloat()
        val totalFibre = records.sumOf { it.template.nutrients.fibre?.toDouble() ?: 0.0 }.toFloat()

        val travelDelta = effectiveTravelDelta(day, previousDay)
        val effectiveTargets = if (travelDelta != null) {
            targets.scale((24.0 + travelDelta) / 24.0)
        } else {
            targets
        }

        val progressItems = buildDailyNutrientProgressItems(
            calories = totalCalories,
            protein = totalProtein,
            fat = totalFat,
            ofWhichSaturated = totalSaturated,
            carbs = totalCarbs,
            ofWhichSugar = totalSugar,
            salt = totalSalt,
            fibre = totalFibre,
            targets = effectiveTargets,
        )

        val infoMessage = buildTimezoneInfo(travelDelta)

        return ListUiModelDailySummary(
            listItemId = diaryDayWindowStart(day.day, day.diaryDayStart, day.startZone)
                .toInstant()
                .toEpochMilli(),
            dayTitle = mapDayTitleTimestamp(day.day),
            infoMessage = infoMessage,
            entries = progressItems,
        )
    }

    private fun mapDayTitleTimestamp(localDate: LocalDate): String {
        return localDate.format(DateTimeFormatter.ofPattern("E, dd MMM"))
    }

    private fun buildDailyNutrientProgressItems(
        calories: Int?,
        protein: Float?,
        fat: Float?,
        ofWhichSaturated: Float?,
        carbs: Float?,
        ofWhichSugar: Float?,
        salt: Float?,
        fibre: Float?,
        targets: Targets,
    ): List<DailySummaryEntry> {
        fun gramRangeLabel(target: Target): String =
            "${target.min ?: "?"}-${target.max ?: "?"}"

        return buildList {
            targets.calories.takeIf { it.enabled }?.let {
                val minVal = it.min
                val maxVal = it.max
                val min = if (minVal != null) {
                    DecimalFormat(".#").format(minVal / 1000f)
                } else {
                    "?"
                }
                val max = if (maxVal != null) {
                    DecimalFormat(".#").format(maxVal / 1000f)
                } else {
                    "?"
                }
                val rangeLabel = "${min}k-${max}k"
                add(
                    DailySummaryEntry(
                        title = "Calories",
                        progress0to1 = templateUiMapper.targetProgress(it, calories?.toFloat() ?: 0f) ?: 0f,
                        progressLabel = templateUiMapper.formatCalories(calories, withLabel = false),
                        targetRange0to1 = templateUiMapper.targetRange(it),
                        targetRangeLabel = rangeLabel,
                        color = { it.caloriesColor },
                    )
                )
            }
            targets.protein.takeIf { it.enabled }?.let {
                add(
                    DailySummaryEntry(
                        title = "Protein",
                        progress0to1 = templateUiMapper.targetProgress(it, protein ?: 0f) ?: 0f,
                        progressLabel = templateUiMapper.formatProtein(protein, withLabel = false),
                        targetRange0to1 = templateUiMapper.targetRange(it),
                        targetRangeLabel = gramRangeLabel(it),
                        color = { it.proteinColor },
                    )
                )
            }
            targets.fat.takeIf { it.enabled }?.let {
                add(
                    DailySummaryEntry(
                        title = "Fat",
                        progress0to1 = templateUiMapper.targetProgress(it, fat ?: 0f) ?: 0f,
                        progressLabel = templateUiMapper.formatFat(fat, saturated = null, withLabel = false),
                        targetRange0to1 = templateUiMapper.targetRange(it),
                        targetRangeLabel = gramRangeLabel(it),
                        color = { it.fatColor },
                    )
                )
            }
            targets.ofWhichSaturated.takeIf { it.enabled }?.let {
                add(
                    DailySummaryEntry(
                        title = "saturated",
                        progress0to1 = templateUiMapper.targetProgress(it, ofWhichSaturated ?: 0f) ?: 0f,
                        progressLabel = templateUiMapper.formatSaturatedFat(ofWhichSaturated, withLabel = false),
                        targetRange0to1 = templateUiMapper.targetRange(it),
                        targetRangeLabel = gramRangeLabel(it),
                        color = { it.fatColor },
                    )
                )
            }
            targets.carbs.takeIf { it.enabled }?.let {
                add(
                    DailySummaryEntry(
                        title = "Carbs",
                        progress0to1 = templateUiMapper.targetProgress(it, carbs ?: 0f) ?: 0f,
                        progressLabel = templateUiMapper.formatCarbs(carbs, sugar = null, addedSugar = null, withLabel = false),
                        targetRange0to1 = templateUiMapper.targetRange(it),
                        targetRangeLabel = gramRangeLabel(it),
                        color = { it.carbsColor },
                    )
                )
            }
            targets.ofWhichSugar.takeIf { it.enabled }?.let {
                add(
                    DailySummaryEntry(
                        title = "sugar",
                        progress0to1 = templateUiMapper.targetProgress(it, ofWhichSugar ?: 0f) ?: 0f,
                        progressLabel = templateUiMapper.formatSugar(ofWhichSugar, withLabel = false),
                        targetRange0to1 = templateUiMapper.targetRange(it),
                        targetRangeLabel = gramRangeLabel(it),
                        color = { it.carbsColor },
                    )
                )
            }
            targets.salt.takeIf { it.enabled }?.let {
                add(
                    DailySummaryEntry(
                        title = "Salt",
                        progress0to1 = templateUiMapper.targetProgress(it, salt ?: 0f) ?: 0f,
                        progressLabel = templateUiMapper.formatSalt(salt, withLabel = false),
                        targetRange0to1 = templateUiMapper.targetRange(it),
                        targetRangeLabel = gramRangeLabel(it),
                        color = { it.saltColor },
                    )
                )
            }
            targets.fibre.takeIf { it.enabled }?.let {
                add(
                    DailySummaryEntry(
                        title = "Fibre",
                        progress0to1 = templateUiMapper.targetProgress(it, fibre ?: 0f) ?: 0f,
                        progressLabel = templateUiMapper.formatFibre(fibre, withLabel = false),
                        targetRange0to1 = templateUiMapper.targetRange(it),
                        targetRangeLabel = gramRangeLabel(it),
                        color = { it.fibreColor },
                    )
                )
            }
        }
    }

    /** Returns deltaHours (negative = shorter/eastbound, positive = longer/westbound),
     *  or null if the day has no significant timezone shift.
     *
     *  If the previous day was already mid-flight (its startZone != endZone), we use the
     *  previous day's startZone as the anchor so multi-day flights trace back to the true
     *  departure timezone rather than a mid-flight zone the phone happened to be in at midnight. */
    private fun effectiveTravelDelta(day: TravelDay, previousDay: TravelDay?): Long? {
        val startZone = if (day.startZone != day.endZone
                            && previousDay != null
                            && previousDay.startZone != previousDay.endZone) {
            previousDay.startZone
        } else {
            day.startZone
        }
        val endZone = if (day.day == LocalDate.now(ZoneId.systemDefault())) {
            ZoneId.systemDefault()
        } else {
            day.endZone
        }
        if (startZone == endZone) return null
        val duration = Duration.between(
            diaryDayWindowStart(day.day, day.diaryDayStart, startZone).toInstant(),
            diaryDayWindowStart(day.day.plusDays(1), day.diaryDayStart, endZone).toInstant(),
        )
        val deltaHours = duration.toHours() - 24
        return if (deltaHours.absoluteValue > 2) deltaHours else null
    }

    private fun buildTimezoneInfo(deltaHours: Long?): String? {
        if (deltaHours == null) return null
        val absHours = deltaHours.absoluteValue
        val absPct = (absHours / 24f * 100).toInt()

        val advisory = if (deltaHours < 0) {
            "\uD83D\uDCA1 Timezone jump: your body clock is $absHours hrs behind local time ($absPct% shorter day).\n" +
                "Try to go to bed when locals do. Or as close as you can.\n" +
                "Meals tend to pile up on shorter days \u2014 consider a lighter meal during the journey."
        } else {
            val dinnerNote = if (LocalTime.now(ZoneId.systemDefault()).hour >= 15) {
                " Even if you ate during your journey, try not to skip local dinner \u2014 restaurants may be closed by the time hunger kicks in."
            } else ""
            "\uD83D\uDCA1 Timezone jump: your body clock is $deltaHours hrs ahead of local time ($absPct% longer day).\n" +
                "Try to go to bed when locals do \u2014 it will feel too late. " +
                "Follow local meal times if you can.$dinnerNote"
        }
        return "$advisory\nYour daily targets have been scaled to match this ${24 + deltaHours}-hr day."
    }

    private fun Targets.scale(factor: Double): Targets {
        fun Target.scaled() = copy(
            min = min?.let { (it * factor).roundToInt() },
            max = max?.let { (it * factor).roundToInt() },
        )
        return copy(
            calories = calories.scaled(),
            protein = protein.scaled(),
            salt = salt.scaled(),
            fat = fat.scaled(),
            carbs = carbs.scaled(),
            fibre = fibre.scaled(),
            ofWhichSaturated = ofWhichSaturated.scaled(),
            ofWhichSugar = ofWhichSugar.scaled(),
        )
    }

    private fun mapWeeklySummary(
        week: List<TravelDay>,
        previousWeek: List<TravelDay>?,
        targets: Targets,
    ): ListUiModelBase? {
        if (week.isEmpty()) return null

        val hasTargets = targets.run {
            calories.enabled || protein.enabled || fat.enabled || carbs.enabled ||
                salt.enabled || fibre.enabled || ofWhichSaturated.enabled || ofWhichSugar.enabled
        }
        if (!hasTargets) {
            val weekStart = week.minOf { it.day }
            return ListUiModelSetTargetsCta(listItemId = weekStart.toEpochDay())
        }

        // 1. Compute total macros per day for a given week
        fun computeDailyTotals(days: List<TravelDay>): List<DayTotal> {
            return days.mapNotNull { day ->
                val r = day.records
                if (r.isEmpty()) return@mapNotNull null
                DayTotal(
                    calories = r.mapNotNull { it.template.nutrients.calories }.takeIf { it.isNotEmpty() }?.sum(),
                    protein = r.mapNotNull { it.template.nutrients.protein }.takeIf { it.isNotEmpty() }?.sum(),
                    fat = r.mapNotNull { it.template.nutrients.fat }.takeIf { it.isNotEmpty() }?.sum(),
                    ofWhichSaturated = r.mapNotNull { it.template.nutrients.ofWhichSaturated }.takeIf { it.isNotEmpty() }?.sum(),
                    carbs = r.mapNotNull { it.template.nutrients.carbs }.takeIf { it.isNotEmpty() }?.sum(),
                    ofWhichSugar = r.mapNotNull { it.template.nutrients.ofWhichSugar }.takeIf { it.isNotEmpty() }?.sum(),
                    ofWhichAddedSugar = r.mapNotNull { it.template.nutrients.ofWhichAddedSugar }.takeIf { it.isNotEmpty() }?.sum(),
                    salt = r.mapNotNull { it.template.nutrients.salt }.takeIf { it.isNotEmpty() }?.sum(),
                    fibre = r.mapNotNull { it.template.nutrients.fibre }.takeIf { it.isNotEmpty() }?.sum(),
                    duration = day.duration,
                )
            }
        }

        val dailyTotals = computeDailyTotals(week)

        if (dailyTotals.isEmpty()) return null

        // 2. Weighted average helper (by TravelDay duration — long travel days contribute more)
        fun weightedAvg(dailyTotals: List<DayTotal>, selector: (DayTotal) -> Number?): Number? {
            var weightedSum = 0.0
            var totalHours = 0.0
            for (day in dailyTotals) {
                val value = selector(day)?.toDouble() ?: continue
                val hours = day.duration.toHours().coerceAtLeast(1).toDouble()
                weightedSum += value * hours
                totalHours += hours
            }
            return if (totalHours > 0) weightedSum / totalHours else null
        }

        fun computeWeeklyAverages(dailyTotals: List<DayTotal>): DayTotal {
            return DayTotal(
                calories = weightedAvg(dailyTotals) { it.calories }?.toInt(),
                protein = weightedAvg(dailyTotals) { it.protein }?.toFloat(),
                fat = weightedAvg(dailyTotals) { it.fat }?.toFloat(),
                ofWhichSaturated = weightedAvg(dailyTotals) { it.ofWhichSaturated }?.toFloat(),
                carbs = weightedAvg(dailyTotals) { it.carbs }?.toFloat(),
                ofWhichSugar = weightedAvg(dailyTotals) { it.ofWhichSugar }?.toFloat(),
                ofWhichAddedSugar = weightedAvg(dailyTotals) { it.ofWhichAddedSugar }?.toFloat(),
                salt = weightedAvg(dailyTotals) { it.salt }?.toFloat(),
                fibre = weightedAvg(dailyTotals) { it.fibre }?.toFloat(),
                duration = Duration.ofHours(24),
            )
        }

        // 3. Compute weekly weighted averages for current week
        val avgDayTotal = computeWeeklyAverages(dailyTotals)

        // 4. Compute previous week's weighted averages if available
        val previousWeekMacros = previousWeek
            ?.let { prev -> computeDailyTotals(prev) }
            ?.takeIf { it.isNotEmpty() }
            ?.let { computeWeeklyAverages(it) }

        // 5. Compare weekly averages against daily targets (no scaling) for adherence
        val avgAdherence = calculateAdherence(avgDayTotal, targets)

        val prevWeekAvgAdherence = previousWeekMacros?.let { calculateAdherence(it, targets) }

        val weekStart = week.minOf { it.day }

        return ListUiModelWeeklySummary(
            listItemId = weekStart.toEpochDay(),
            entries = buildWeeklySummaryEntries(avgDayTotal, targets, previousWeekMacros),
            averageAdherence100Percentage = (avgAdherence * 100).roundToInt(),
            adherenceChange = calculateChangeIndicator(avgAdherence, prevWeekAvgAdherence),
        )
    }

    /**
     * Builds nutrient change indicators.
     * Change is calculated relative to previous week (showing week-over-week change).
     */
    private fun buildWeeklySummaryEntries(
        dayTotal: DayTotal,
        targets: Targets,
        previousDayTotal: DayTotal? = null,
    ): List<NutrientSummaryStatEntry> {
        return buildList {
            targets.calories.takeIf { it.enabled }?.let {
                add(
                    NutrientSummaryStatEntry(
                        title = "Calories",
                        progress0to1 = templateUiMapper.targetProgress(it, dayTotal.calories?.toFloat() ?: 0f) ?: 0f,
                        progressLabel = templateUiMapper.formatCalories(dayTotal.calories, withLabel = false),
                        targetRange0to1 = templateUiMapper.targetRange(it),
                        changeIndicator = calculateChangeIndicator(
                            current = dayTotal.calories?.toFloat(),
                            previous = previousDayTotal?.calories?.toFloat(),
                        ),
                        color = { it.caloriesColor },
                    )
                )
            }
            targets.protein.takeIf { it.enabled }?.let {
                add(
                    NutrientSummaryStatEntry(
                        title = "Protein",
                        progress0to1 = templateUiMapper.targetProgress(it, dayTotal.protein ?: 0f) ?: 0f,
                        progressLabel = templateUiMapper.formatProtein(dayTotal.protein, withLabel = false),
                        targetRange0to1 = templateUiMapper.targetRange(it),
                        changeIndicator = calculateChangeIndicator(
                            current = dayTotal.protein,
                            previous = previousDayTotal?.protein,
                        ),
                        color = { it.proteinColor },
                    )
                )
            }
            targets.salt.takeIf { it.enabled }?.let {
                add(
                    NutrientSummaryStatEntry(
                        title = "Salt",
                        progress0to1 = templateUiMapper.targetProgress(it, dayTotal.salt ?: 0f) ?: 0f,
                        progressLabel = templateUiMapper.formatSalt(dayTotal.salt, withLabel = false),
                        targetRange0to1 = templateUiMapper.targetRange(it),
                        changeIndicator = calculateChangeIndicator(
                            current = dayTotal.salt,
                            previous = previousDayTotal?.salt,
                        ),
                        color = { it.saltColor },
                    )
                )
            }
            targets.fibre.takeIf { it.enabled }?.let {
                add(
                    NutrientSummaryStatEntry(
                        title = "Fibre",
                        progress0to1 = templateUiMapper.targetProgress(it, dayTotal.fibre ?: 0f) ?: 0f,
                        progressLabel = templateUiMapper.formatFibre(dayTotal.fibre, withLabel = false),
                        targetRange0to1 = templateUiMapper.targetRange(it),
                        changeIndicator = calculateChangeIndicator(
                            current = dayTotal.fibre,
                            previous = previousDayTotal?.fibre,
                        ),
                        color = { it.fibreColor },
                    )
                )
            }
            targets.carbs.takeIf { it.enabled }?.let {
                add(
                    NutrientSummaryStatEntry(
                        title = "Carbs",
                        progress0to1 = templateUiMapper.targetProgress(it, dayTotal.carbs ?: 0f) ?: 0f,
                        progressLabel = templateUiMapper.formatCarbs(dayTotal.carbs, sugar = null, addedSugar = null, withLabel = false),
                        targetRange0to1 = templateUiMapper.targetRange(it),
                        changeIndicator = calculateChangeIndicator(
                            current = dayTotal.carbs,
                            previous = previousDayTotal?.carbs,
                        ),
                        color = { it.carbsColor },
                    )
                )
            }
            targets.ofWhichSugar.takeIf { it.enabled }?.let {
                add(
                    NutrientSummaryStatEntry(
                        title = "sugar",
                        progress0to1 = templateUiMapper.targetProgress(it, dayTotal.ofWhichSugar ?: 0f) ?: 0f,
                        progressLabel = templateUiMapper.formatSugar(dayTotal.ofWhichSugar, withLabel = false),
                        targetRange0to1 = templateUiMapper.targetRange(it),
                        changeIndicator = calculateChangeIndicator(
                            current = dayTotal.ofWhichSugar,
                            previous = previousDayTotal?.ofWhichSugar,
                        ),
                        color = { it.carbsColor },
                    )
                )
            }
            targets.fat.takeIf { it.enabled }?.let {
                add(
                    NutrientSummaryStatEntry(
                        title = "Fat",
                        progress0to1 = templateUiMapper.targetProgress(it, dayTotal.fat ?: 0f) ?: 0f,
                        progressLabel = templateUiMapper.formatFat(dayTotal.fat, saturated = null, withLabel = false),
                        targetRange0to1 = templateUiMapper.targetRange(it),
                        changeIndicator = calculateChangeIndicator(
                            current = dayTotal.fat,
                            previous = previousDayTotal?.fat,
                        ),
                        color = { it.fatColor },
                    )
                )
            }
            targets.ofWhichSaturated.takeIf { it.enabled }?.let {
                add(
                    NutrientSummaryStatEntry(
                        title = "saturated",
                        progress0to1 = templateUiMapper.targetProgress(it, dayTotal.ofWhichSaturated ?: 0f) ?: 0f,
                        progressLabel = templateUiMapper.formatSaturatedFat(dayTotal.ofWhichSaturated, withLabel = false),
                        targetRange0to1 = templateUiMapper.targetRange(it),
                        changeIndicator = calculateChangeIndicator(
                            current = dayTotal.ofWhichSaturated,
                            previous = previousDayTotal?.ofWhichSaturated,
                        ),
                        color = { it.fatColor },
                    )
                )
            }
        }
    }

    /**
     * Calculates change indicator based on week-over-week comparison.
     * Compares current week macros to previous week macros.
     */
    internal fun calculateChangeIndicator(
        current: Float?,
        previous: Float?,
    ): ChangeIndicator {
        if (current == null || current == 0f) {
            return ChangeIndicator(ChangeDirection.NEUTRAL, "")
        }

        if (previous == null || previous == 0f) {
            return ChangeIndicator(ChangeDirection.NEUTRAL, "")
        }

        val deviation = current - previous
        val percentChange = deviation / previous

        val direction = when {
            percentChange * 100 > 2f -> ChangeDirection.UP
            percentChange * 100 < -2f -> ChangeDirection.DOWN
            else -> ChangeDirection.NEUTRAL
        }

        val formattedValue = DecimalFormat("+#%;-#%").format(percentChange)
        return ChangeIndicator(direction, formattedValue)
    }

    private fun calculateAdherence(
        dayTotal: DayTotal,
        targets: Targets,
    ): Float {
        val scores = mutableListOf<Float>()

        val factor = (dayTotal.duration.toHours().toFloat() / 24f).coerceAtLeast(0.01f) // avoid div-by-zero

        fun score(value: Float?, target: Target): Float? {
            if (!target.enabled || value == null) return null

            val min = target.min?.toFloat()?.times(factor)
            val max = target.max?.toFloat()?.times(factor)

            // No range defined → can't evaluate
            if (min == null && max == null) return null

            // Within scaled range → perfect
            if ((min == null || value >= min) && (max == null || value <= max)) return 1f

            // Below range
            if (min != null && value < min) {
                val diff = min - value
                val span = min.takeIf { it > 0f } ?: 1f
                return (1f - (diff / span)).coerceIn(0f, 1f)
            }

            // Above range
            if (max != null && value > max) {
                val diff = value - max
                val span = max.takeIf { it > 0f } ?: 1f
                return (1f - (diff / span)).coerceIn(0f, 1f)
            }

            return null
        }

        // Evaluate each nutrient against its scaled targets
        dayTotal.calories?.toFloat()?.let { score(it, targets.calories)?.let(scores::add) }
        dayTotal.protein?.let { score(it, targets.protein)?.let(scores::add) }
        dayTotal.fat?.let { score(it, targets.fat)?.let(scores::add) }
        dayTotal.carbs?.let { score(it, targets.carbs)?.let(scores::add) }
        dayTotal.ofWhichSaturated?.let { score(it, targets.ofWhichSaturated)?.let(scores::add) }
        dayTotal.ofWhichSugar?.let { score(it, targets.ofWhichSugar)?.let(scores::add) }
        dayTotal.salt?.let { score(it, targets.salt)?.let(scores::add) }
        dayTotal.fibre?.let { score(it, targets.fibre)?.let(scores::add) }

        return if (scores.isEmpty()) 0f else scores.average().toFloat()
    }

    private data class DayTotal(
        val calories: Int? = null,
        val protein: Float? = null,
        val fat: Float? = null,
        val ofWhichSaturated: Float? = null,
        val carbs: Float? = null,
        val ofWhichSugar: Float? = null,
        val ofWhichAddedSugar: Float? = null,
        val salt: Float? = null,
        val fibre: Float? = null,
        val duration: Duration,
    )

    private fun List<Record>.groupByWallClockDay(dayStart: LocalTime): List<TravelDay> {
        return this
            .sortedBy { it.timestamp.toInstant() }
            .groupByTo(
                destination = sortedMapOf(),
                keySelector = { it.timestamp.logicalDiaryDate(dayStart) },
                valueTransform = { it }
            )
            .map { (day, records) ->
                val start = records.minBy { it.timestamp.toInstant() }.timestamp
                val end = records.maxBy { it.timestamp.toInstant() }.timestamp
                TravelDay(
                    records = records.toList(),
                    day = day,
                    firstLog = start,
                    lastLog = end,
                    diaryDayStart = dayStart,
                )
            }
    }
}
