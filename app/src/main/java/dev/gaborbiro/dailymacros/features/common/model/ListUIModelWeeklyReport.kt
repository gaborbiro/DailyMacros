package dev.gaborbiro.dailymacros.features.common.model

internal data class ListUIModelWeeklyReport(
    override val listItemId: Long,
    val weeklyProgress: List<WeeklySummaryMacroProgressItem>,
    val averageAdherence100Percentage: Int,
    val adherenceChange: ChangeIndicator?,
) : ListUIModelBase(listItemId = listItemId, contentType = "weeklyReport")
