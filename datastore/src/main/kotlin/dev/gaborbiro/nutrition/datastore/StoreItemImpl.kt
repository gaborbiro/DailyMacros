package dev.gaborbiro.nutrition.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import dev.gaborbiro.nutrition.datastore.domain.StoreItem
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.cancellable
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

/**
 * A delegate that knows how to read/write a DataStore of type Preferences. This simplifies
 * persistence by allowing the user to simply read/write a kotlin variable.
 *
 * The implementation allows for the underlying storage format to be different from the actual type
 * of the data, in order to allow storage (serialization) of complex data types.
 * Note, that the type of the key must always match the storage format in this case.
 *
 * If you don't need this, make [T] and [S] the same and set the [mapper] to null.
 */
class StoreItemImpl<T, S>(
    private val key: Preferences.Key<S>,
    private val scope: CoroutineScope,
    private val prefs: DataStore<Preferences>,
    private val mapper: Mapper<T, S>? = null,
) : StoreItem<T> {

    private var job: Job? = null

    override fun get(): Flow<T?> {
        return prefs.data.map { prefs ->
            mapper
                ?.fromStoreType(prefs[key])
                ?: prefs[key] as T?
        }.distinctUntilChanged()
    }

    /**
     * Cancels the reading of previous flows (if any)
     */
    override fun set(value: Flow<T?>) {
        job?.cancel()
        job = scope.launch {
            value.cancellable().collect { latest ->
                prefs.edit { pref ->
                    if (latest != null) {
                        pref[key] = mapper
                            ?.let { it.toStoreType(latest)!! }
                            ?: latest as S
                    } else {
                        pref.remove(key)
                    }
                }
            }
        }
    }

    /**
     * Does not interfere with [set(Flow<T?>)]
     */
    override fun set(value: T?) {
        scope.launch {
            prefs.edit { pref ->
                if (value != null) {
                    pref[key] = mapper
                        ?.let { it.toStoreType(value)!! }
                        ?: value as S
                } else {
                    pref.remove(key)
                }
            }
        }
    }
}

