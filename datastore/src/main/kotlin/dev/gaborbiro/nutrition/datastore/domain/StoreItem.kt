package dev.gaborbiro.nutrition.datastore.domain

import kotlinx.coroutines.flow.Flow


interface StoreItem<T> {
    fun get(): Flow<T?>

    /**
     * Cancels the reading of previous flows (if any)
     */
    fun set(value: Flow<T?>)

    /**
     * Does not interfere with [set(Flow<T?>)]
     */
    fun set(value: T?)
}
