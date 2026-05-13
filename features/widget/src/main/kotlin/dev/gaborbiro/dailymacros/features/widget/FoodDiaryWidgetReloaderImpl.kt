package dev.gaborbiro.dailymacros.features.widget

import android.content.Context
import dev.gaborbiro.dailymacros.features.shared.widget.FoodDiaryWidgetReloader
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FoodDiaryWidgetReloaderImpl @Inject constructor() : FoodDiaryWidgetReloader {
    override fun scheduleReload(context: Context) {
        DiaryWidgetScreen.reload(context.applicationContext)
    }
}
