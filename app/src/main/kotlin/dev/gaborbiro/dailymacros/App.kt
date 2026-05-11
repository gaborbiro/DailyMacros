package dev.gaborbiro.dailymacros

import android.app.Application
import android.content.Context
import dev.gaborbiro.dailymacros.data.db.AppDatabase
import dev.gaborbiro.dailymacros.features.widget.WidgetNavigatorDependency
import dev.gaborbiro.dailymacros.util.createNotificationChannels

class App : Application() {

    companion object {
        lateinit var appContext: Context
    }

    override fun onCreate() {
        super.onCreate()
        appContext = this
        WidgetNavigatorDependency.factory = { WidgetNavigatorImpl() }
        AppDatabase.init(this)
        createNotificationChannels()
    }
}
