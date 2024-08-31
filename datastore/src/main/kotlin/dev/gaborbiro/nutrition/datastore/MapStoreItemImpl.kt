package dev.gaborbiro.nutrition.datastore

import androidx.collection.LruCache
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import dev.gaborbiro.nutrition.datastore.domain.MapStoreItem
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.cancellable
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

/**
 * A partial map implementation that knows how to get/set a DataStore of type Preferences.
 * This simplifies persistence by allowing the user to simply read/write a kotlin map object.
 *
 * The implementation allows for the underlying storage format to be different from the actual type
 * of the data, in order to allow storage (serialization) of complex data types.
 * Note, that the type of the key must always match the storage format in this case.
 *
 * If you don't need this, make [T] and [S] the same and set the [mapper] to null.
 */
class MapStoreItemImpl<T, S>(
    private val key: String,
    private val createKey: (key: String) -> Preferences.Key<S>,
    private val scope: CoroutineScope,
    private val prefs: DataStore<Preferences>,
    private val mapper: Mapper<T, S>? = null,
) : MapStoreItem<T> {

    private var job: Job? = null

    private val keyCache: LruCache<String, Preferences.Key<S>> =
        object : LruCache<String, Preferences.Key<S>>(100) {

            override fun create(key: String): Preferences.Key<S> {
                return createKey(key)
            }
        }

    override operator fun get(subKey: String): Flow<T?> {
        val key = keyCache["${key}_${subKey}"]!!
        return prefs.data.map { prefs ->
            val value: S? = prefs[key]
            mapper
                ?.fromStoreType(value)
                ?: value as T?
        }
    }

    /**
     * Cancels the reading of previous flows (if any)
     */
    override operator fun set(subKey: String, value: Flow<T?>) {
        val key = keyCache["${key}_${subKey}"]!!
        job?.cancel()
        job = scope.launch {
            value.cancellable().collect { latest ->
                prefs.edit { prefs: MutablePreferences ->
                    latest?.let {
                        val mapped = mapper
                            ?.toStoreType(latest)
                            ?: it as S
                        if (prefs[key] != mapped) {
                            prefs[key] = mapped
                        }
                    } ?: run {
                        prefs.remove(key)
                    }
                }
            }
        }
    }

    /**
     * Does not interfere with [set(String, Flow<T?>)]
     */
    override operator fun set(subKey: String, value: T?) {
        val key = keyCache["${key}_${subKey}"]!!
        job = scope.launch {
            prefs.edit { prefs: MutablePreferences ->
                value?.let {
                    val mapped = mapper
                        ?.toStoreType(value)
                        ?: it as S
                    if (prefs[key] != mapped) {
                        prefs[key] = mapped
                    }
                } ?: run {
                    prefs.remove(key)
                }
            }
        }
    }
}