package dev.gaborbiro.dailymacros.features.widget

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import androidx.glance.state.GlanceStateDefinition
import java.io.File

object WidgetPreferences : GlanceStateDefinition<Preferences> {

    private const val filename = "notes"

    override suspend fun getDataStore(context: Context, fileKey: String): DataStore<Preferences> {
        return context.dataStore
    }

    override fun getLocation(context: Context, fileKey: String): File {
        return File(context.applicationContext.filesDir, "datastore/$filename")
    }

    private val Context.dataStore: DataStore<Preferences>
        by preferencesDataStore(name = filename)
}
