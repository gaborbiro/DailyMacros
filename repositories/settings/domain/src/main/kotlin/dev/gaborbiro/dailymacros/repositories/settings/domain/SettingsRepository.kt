package dev.gaborbiro.dailymacros.repositories.settings.domain

import dev.gaborbiro.dailymacros.repositories.settings.domain.model.Targets

interface SettingsRepository {

    fun getTargets(): Targets

    fun setTargets(targets: Targets)

    /** Hour of day (0–23) when the food diary day rolls over; 0 means midnight. */
    fun getDiaryDayStartHour(): Int

    fun setDiaryDayStartHour(hourOfDay: Int)

    /** Returns user overrides for AI prompt editable segments, keyed by segment ID. */
    fun getPromptCustomizations(): Map<String, String>

    fun setPromptCustomizations(values: Map<String, String>)
}
