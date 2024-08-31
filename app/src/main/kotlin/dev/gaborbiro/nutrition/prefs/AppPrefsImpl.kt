package dev.gaborbiro.nutrition.prefs

import android.content.Context
import dev.gaborbiro.nutrition.datastore.domain.StoreItem
import dev.gaborbiro.nutrition.datastore.StoreBase
import dev.gaborbiro.nutrition.datastore.domain.MapStoreItem
import dev.gaborbiro.nutrition.prefs.domain.AppPrefs
import dev.gaborbiro.nutrition.prefs.domain.SampleDataItem
import kotlinx.coroutines.CoroutineScope


internal open class AppPrefsImpl(context: Context, scope: CoroutineScope) : StoreBase(context, scope),
    AppPrefs {

    override val sampleString: StoreItem<String> = stringDelegate("SAMPLE_STRING")

    override val sampleInt: StoreItem<Int> = intDelegate("SAMPLE_INT")

    override val sampleDouble: StoreItem<Double> = doubleDelegate("SAMPLE_DOUBLE")

    override val sampleObject: StoreItem<SampleDataItem> =
        gsonSerializablePrefsDelegate<SampleDataItem>("SAMPLE_OBJECT")

    override val sampleBooleanMap: MapStoreItem<Boolean> =
        booleanMapPrefsDelegate("SAMPLE_BOOLEAN_MAP")

    override val sampleObjectMap: MapStoreItem<SampleDataItem> =
        gsonSerializableMapPrefsDelegate("SAMPLE_OBJECT_MAP")
}
