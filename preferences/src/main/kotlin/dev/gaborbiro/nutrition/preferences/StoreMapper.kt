package dev.gaborbiro.nutrition.preferences

interface StoreMapper<T, S> {
    fun toStoreType(value: T?): S?
    fun fromStoreType(serialised: S?): T?
}