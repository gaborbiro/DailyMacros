package dev.gaborbiro.dailymacros.features.settings.model

data class SettingsUiState(
    val showTargetsSettings: Boolean,
    val bottomLabel: String,
    val exportDataInProgress: Boolean = false,
    val importDataInProgress: Boolean = false,
    val diaryDayStartHour: Int = 0,
    val showDiaryDayStartDialog: Boolean = false,
    val showPromptEditor: Boolean = false,
)
