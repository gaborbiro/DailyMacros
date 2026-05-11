package dev.gaborbiro.dailymacros.features.widget

import android.content.Context

/**
 * Triggers a refresh of the diary home-screen widget data (typically after food diary changes).
 *
 * Implementations live in the app module so feature code does not need to reference
 * [DiaryWidgetScreen] directly.
 */
fun interface FoodDiaryWidgetReloader {
    fun scheduleReload(context: Context)
}
