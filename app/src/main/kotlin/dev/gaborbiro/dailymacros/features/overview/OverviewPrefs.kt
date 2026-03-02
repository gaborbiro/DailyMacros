package dev.gaborbiro.dailymacros.features.overview

import android.content.Context
import androidx.core.content.edit

internal class OverviewPrefs(context: Context) {

    companion object {
        private const val KEY_SHOW_COACH_MARK = "show_coach_mark"
    }

    private val prefs = context.getSharedPreferences("overview_prefs", Context.MODE_PRIVATE)

    var showCoachMark: Boolean
        get() = prefs.getBoolean(KEY_SHOW_COACH_MARK, true)
        set(value) = prefs.edit { putBoolean(KEY_SHOW_COACH_MARK, value) }
}
