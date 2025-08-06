package dev.gaborbiro.nutri

import android.app.Application
import android.content.Context
import dev.gaborbiro.nutri.store.db.AppDatabase
import dev.gaborbiro.nutri.util.createNotificationChannels

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
