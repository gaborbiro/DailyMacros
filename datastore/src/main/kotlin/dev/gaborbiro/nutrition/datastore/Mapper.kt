package dev.gaborbiro.nutrition.datastore

interface Mapper<T, S> {
    fun toStoreType(value: T?): S?
    fun fromStoreType(value: S?): T?
}