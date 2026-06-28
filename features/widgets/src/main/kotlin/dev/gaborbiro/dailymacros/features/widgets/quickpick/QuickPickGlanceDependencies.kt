package dev.gaborbiro.dailymacros.features.widgets.quickpick

import dev.gaborbiro.dailymacros.core.analytics.AnalyticsLogger
import dev.gaborbiro.dailymacros.data.image.domain.ImageStore
import dev.gaborbiro.dailymacros.features.widgets.WidgetNavigator
import dev.gaborbiro.dailymacros.features.widgets.WidgetUiMapper
import dev.gaborbiro.dailymacros.repositories.records.domain.RecordsRepository
import javax.inject.Inject

class QuickPickGlanceDependencies @Inject constructor(
    val imageStore: ImageStore,
    val widgetUiMapper: WidgetUiMapper,
    val widgetNavigator: WidgetNavigator,
    val recordsRepository: RecordsRepository,
    val analyticsLogger: AnalyticsLogger,
)
