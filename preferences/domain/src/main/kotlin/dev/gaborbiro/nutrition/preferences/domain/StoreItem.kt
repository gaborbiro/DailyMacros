package dev.gaborbiro.nutrition.preferences.domain

import kotlinx.coroutines.flow.Flow


interface StoreItem<T> {
    fun get(): Flow<T?>

    /**
     * Cancels the reading of previous flows (if any)
     */
    suspend fun set(value: Flow<T?>)

    /**
     * Does not interfere with [set(Flow<T?>)]
     */
    suspend fun set(value: T?)
}
