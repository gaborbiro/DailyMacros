package dev.gaborbiro.dailymacros.features.trends

import dev.gaborbiro.dailymacros.features.common.AppPrefs
import dev.gaborbiro.dailymacros.features.trends.model.DailyAggregationMode

internal class TrendsMapper {

    fun map(mode: DailyAggregationMode): String {
        return when (mode) {
            DailyAggregationMode.CALENDAR_DAYS -> AppPrefs.AGGREGATION_MODE_CALENDAR_DAYS
            DailyAggregationMode.LOGGED_DAYS -> AppPrefs.AGGREGATION_MODE_LOGGED_DAYS
            DailyAggregationMode.QUALIFIED_DAYS -> AppPrefs.AGGREGATION_MODE_QUALIFIED_DAYS
        }
    }

    fun map(@AppPrefs.Companion.AggregationMode mode: String): DailyAggregationMode {
        return when (mode) {
            AppPrefs.AGGREGATION_MODE_CALENDAR_DAYS -> DailyAggregationMode.CALENDAR_DAYS
            AppPrefs.AGGREGATION_MODE_LOGGED_DAYS -> DailyAggregationMode.LOGGED_DAYS
            AppPrefs.AGGREGATION_MODE_QUALIFIED_DAYS -> DailyAggregationMode.QUALIFIED_DAYS
            else -> throw IllegalArgumentException("Unknown aggregation mode: $mode")
        }
    }
}