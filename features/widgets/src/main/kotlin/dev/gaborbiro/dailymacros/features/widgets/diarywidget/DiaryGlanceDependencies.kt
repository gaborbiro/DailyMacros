package dev.gaborbiro.dailymacros.features.widgets.diarywidget

import dev.gaborbiro.dailymacros.core.analytics.AnalyticsLogger
import dev.gaborbiro.dailymacros.data.image.domain.ImageStore
import dev.gaborbiro.dailymacros.features.shared.RecordsUiMapper
import dev.gaborbiro.dailymacros.features.widgets.WidgetNavigator
import dev.gaborbiro.dailymacros.features.widgets.WidgetUiMapper
import javax.inject.Inject

class DiaryGlanceDependencies @Inject constructor(
    val imageStore: ImageStore,
    val recordsUiMapper: RecordsUiMapper,
    val widgetUiMapper: WidgetUiMapper,
    val widgetNavigator: WidgetNavigator,
    val analyticsLogger: AnalyticsLogger,
)
