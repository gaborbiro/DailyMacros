package dev.gaborbiro.nutrition.datastore

import android.content.Context
import android.util.LruCache
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dev.gaborbiro.nutrition.datastore.domain.MapStoreItem
import dev.gaborbiro.nutrition.datastore.domain.StoreItem
import com.google.gson.Gson
import kotlinx.coroutines.CoroutineScope


private val Context.dataStore: DataStore<Preferences> by preferencesDataStore("preferences")

abstract class StoreBase(context: Context, protected val scope: CoroutineScope) {

    protected val gson = Gson()

    protected val prefs: DataStore<Preferences> = context.dataStore

    protected inline fun <reified T> gsonSerializablePrefsDelegate(
        key: String,
        size: Int = 1 * 1024 * 1024, // 1MB
    ): StoreItem<T> {
        return StoreItemImpl(
            key = stringPreferencesKey(key),
            scope = scope,
            prefs = prefs,
            mapper = getPrefsDelegateMapper(size, T::class.java),
        )
    }

    protected inline fun <reified T> gsonSerializableMapPrefsDelegate(
        keyBase: String,
        size: Int = 1 * 1024 * 1024, // 1MB
    ): MapStoreItem<T> {
        return MapStoreItemImpl(
            key = keyBase,
            createKey = { stringPreferencesKey(it) },
            scope = scope,
            prefs = prefs,
            mapper = getPrefsDelegateMapper(size, T::class.java),
        )
    }

    fun <T> getPrefsDelegateMapper(size: Int, type: Class<T>): Mapper<T, String> =
        object : Mapper<T, String> {

            private val cache: LruCache<String, T> = LruCache(size)

            override fun toStoreType(value: T?): String? {
                return value?.let {
                    val serialised = gson.toJson(it)
                    cache.put(serialised, it)
                    serialised
                }
            }

            override fun fromStoreType(serialised: String?): T? {
                return serialised?.let { serialised ->
                    cache[serialised]
                        ?: gson.fromJson(serialised, type)
                            .also {
                                cache.put(serialised, it)
                            }
                }
            }
        }

    protected fun intDelegate(key: String): StoreItem<Int> {
        return StoreItemImpl(
            key = intPreferencesKey(key),
            scope = scope,
            prefs = prefs,
        )
    }

    protected fun doubleDelegate(key: String): StoreItem<Double> {
        return StoreItemImpl(
            key = doublePreferencesKey(key),
            scope = scope,
            prefs = prefs,
        )
    }

    protected fun stringDelegate(key: String): StoreItem<String> {
        return StoreItemImpl(
            key = stringPreferencesKey(key),
            scope = scope,
            prefs = prefs,
        )
    }

    protected fun booleanMapPrefsDelegate(keyBase: String): MapStoreItem<Boolean> {
        return MapStoreItemImpl(
            key = keyBase,
            createKey = { booleanPreferencesKey(it) },
            scope = scope,
            prefs = prefs,
        )
    }
}