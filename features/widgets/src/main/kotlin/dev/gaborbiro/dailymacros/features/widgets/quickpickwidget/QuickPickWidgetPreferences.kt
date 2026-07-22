package dev.gaborbiro.dailymacros.features.widgets.quickpickwidget

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import androidx.glance.state.GlanceStateDefinition
import java.io.File

object QuickPickWidgetPreferences : GlanceStateDefinition<Preferences> {

    private const val FILENAME = "quick_pick_widget"

    override suspend fun getDataStore(context: Context, fileKey: String): DataStore<Preferences> {
        return context.dataStore
    }

    override fun getLocation(context: Context, fileKey: String): File {
        return File(context.applicationContext.filesDir, "datastore/$FILENAME")
    }

    private val Context.dataStore: DataStore<Preferences>
        by preferencesDataStore(name = FILENAME)
}
