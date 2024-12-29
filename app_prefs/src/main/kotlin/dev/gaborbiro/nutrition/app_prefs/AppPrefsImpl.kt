package dev.gaborbiro.nutrition.app_prefs

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import dev.gaborbiro.nutrition.app_prefs.domain.AppPrefs
import dev.gaborbiro.nutrition.preferences.StoreBase
import dev.gaborbiro.nutrition.preferences.domain.StoreItem
import javax.inject.Inject
import javax.inject.Named


internal class AppPrefsImpl @Inject constructor(@Named("appPrefs") store: DataStore<Preferences>) :
    StoreBase(store), AppPrefs {

    override val lastQuery = stringDelegate("lastQuery")

    override val googleApiAccessToken = stringDelegate("googleApiAccessToken")
}
