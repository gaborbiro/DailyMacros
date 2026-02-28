package dev.gaborbiro.dailymacros

import android.app.Application
import android.content.Context
import dev.gaborbiro.dailymacros.data.db.AppDatabase
import dev.gaborbiro.dailymacros.util.createNotificationChannels

class App : Application() {

    companion object {
        lateinit var appContext: Context
    }

    override fun onCreate() {
        super.onCreate()
        appContext = this
        AppDatabase.init(this)
        createNotificationChannels()
    }
}
