package dev.gaborbiro.dailymacros.repositories.settings.domain

import dev.gaborbiro.dailymacros.repositories.settings.domain.model.Targets

interface SettingsRepository {

    fun getTargets(): Targets

    fun setTargets(targets: Targets)
}
