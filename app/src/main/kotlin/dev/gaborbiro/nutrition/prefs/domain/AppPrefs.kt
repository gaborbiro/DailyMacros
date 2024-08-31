package dev.gaborbiro.nutrition.prefs.domain

import dev.gaborbiro.nutrition.datastore.domain.MapStoreItem
import dev.gaborbiro.nutrition.datastore.domain.StoreItem


interface AppPrefs {
    val sampleString: StoreItem<String>
    val sampleInt: StoreItem<Int>
    val sampleDouble: StoreItem<Double>
    val sampleObject: StoreItem<SampleDataItem>
    val sampleBooleanMap: MapStoreItem<Boolean>
    val sampleObjectMap: MapStoreItem<SampleDataItem>
}

data class SampleDataItem(
    val sampleValue: String,
)