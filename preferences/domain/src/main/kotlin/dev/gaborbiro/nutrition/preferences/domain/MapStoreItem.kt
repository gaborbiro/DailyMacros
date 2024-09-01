package dev.gaborbiro.nutrition.preferences.domain

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf


interface MapStoreItem<T> {

    fun get(subKey: String): Flow<T?>

    /**
     * Cancels the reading of previous flows (if any)
     */
    suspend fun set(subKey: String, value: Flow<T?>)

    /**
     * Does not interfere with [set(String, Flow<T?>)]
     */
    suspend fun set(subKey: String, value: T?)

    companion object {
        fun <T> dummyImplementation(value: Map<String, T> = emptyMap()): MapStoreItem<T> {
            return object : MapStoreItem<T> {
                override fun get(subKey: String): Flow<T?> {
                    return flowOf(value[subKey])
                }

                override suspend fun set(subKey: String, value: Flow<T?>) {
                }

                override suspend fun set(subKey: String, value: T?) {
                }
            }
        }
    }
}
