package dev.gaborbiro.dailymacros.features.widget

import dev.gaborbiro.dailymacros.core.analytics.AnalyticsLogger
import dev.gaborbiro.dailymacros.data.image.domain.ImageStore
import dev.gaborbiro.dailymacros.features.shared.SharedRecordsUiMapper
import javax.inject.Inject

class WidgetGlanceDependencies @Inject constructor(
    val imageStore: ImageStore,
    val sharedRecordsUiMapper: SharedRecordsUiMapper,
    val widgetUiMapper: WidgetUiMapper,
    val widgetNavigator: WidgetNavigator,
    val analyticsLogger: AnalyticsLogger,
)
