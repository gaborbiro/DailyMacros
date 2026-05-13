package dev.gaborbiro.dailymacros.features.shared.widget

import android.content.Context

/**
 * Triggers a refresh of the diary home-screen widget data.
 *
 * The implementation lives in `:features:widget`. Consumers depend only on `:features:shared`
 * and inject this interface — they never need to reference the widget module directly.
 *
 * In normal flow, callers do **not** need to invoke this explicitly: the widget self-observes
 * the records repository and reloads on any data change. This SPI is kept available as a
 * fallback for force-reload scenarios (e.g. a user-initiated refresh action).
 */
fun interface FoodDiaryWidgetReloader {
    fun scheduleReload(context: Context)
}
