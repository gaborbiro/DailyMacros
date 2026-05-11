package dev.gaborbiro.dailymacros.di

import android.content.Context
import dev.gaborbiro.dailymacros.features.widget.DiaryWidgetScreen
import dev.gaborbiro.dailymacros.features.widget.FoodDiaryWidgetReloader
import javax.inject.Inject

class FoodDiaryWidgetReloaderImpl @Inject constructor() : FoodDiaryWidgetReloader {
    override fun scheduleReload(context: Context) {
        DiaryWidgetScreen.reload(context.applicationContext)
    }
}
