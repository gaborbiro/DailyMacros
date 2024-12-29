package dev.gaborbiro.nutrition.preferences

import android.util.LruCache
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.google.gson.Gson
import dev.gaborbiro.nutrition.preferences.domain.MapStoreItem
import dev.gaborbiro.nutrition.preferences.domain.StoreItem


/**
 * Example usage:
 *
 * Create an interface and implementation. Something like this:
 * ```
 * interface AppPrefs {
 *     val someString: StoreItem<String>
 *     val someInt: StoreItem<Int>
 *     val someDouble: StoreItem<Double>
 *     val someObject: StoreItem<SampleDataItem>
 *     val someBooleanMap: MapStoreItem<Boolean>
 *     val someObjectMap: MapStoreItem<SomeDataItem>
 *
 *     data class SomeDataItem(val value: String)
 * }
 *
 * internal class AppPrefsImpl @Inject constructor(store: DataStore<Preferences>) :
 *     StoreBase(store), AppPrefs {
 *     override val someString: StoreItem<String> = stringDelegate("SOME_STRING")
 *     override val someInt: StoreItem<Int> = intDelegate("SOME_INT")
 *     override val someDouble: StoreItem<Double> = doubleDelegate("SOME_DOUBLE")
 *     override val someObject: StoreItem<SampleDataItem> = gsonSerializablePrefsDelegate<SampleDataItem>("SOME_OBJECT")
 *     override val someBooleanMap: MapStoreItem<Boolean> = booleanMapPrefsDelegate("SOME_BOOLEAN_MAP")
 *     override val someObjectMap: MapStoreItem<SomeDataItem> = gsonSerializableMapPrefsDelegate("SOME_OBJECT_MAP")
 * }
 * ```
 * Your DI module might look like this:
 * ```
 * private val Context.dataStore: DataStore<Preferences> by preferencesDataStore("appPrefs")
 *
 * @Module
 * @InstallIn(SingletonComponent::class)
 * internal abstract class AppPrefsModule {
 *
 *     @Binds
 *     @Singleton
 *     abstract fun provideContext(application: Application): Context
 *
 *     companion object {
 *
 *         @Provides
 *         @Singleton
 *         fun provideAppPrefs(context: Context): AppPrefs {
 *             return AppPrefsImpl(context.dataStore)
 *         }
 *     }
 * }
 * ```
 *
 * If you don't want to do DI, you can place the Context.dataStore right in the AppPrefsImpl file (
 * your AppPrefsImpl would have a dependency on Context).
 *
 * This is how you'd use your new store:
 *
 * ```
 * @HiltViewModel
 * class HomeViewModel @Inject constructor(
 *     application: Application,
 *     private val appPrefs: AppPrefs,
 * ) : ViewModel(application) {
 *     ...
 *     appPrefs.someString.set(flowOf("blah"))
 *     appPrefs.someString.set("blah2")
 *     appPrefs.someInt.set(flowOf(1))
 *     appPrefs.someDouble.set(flowOf(null))
 *     appPrefs.someObject.set(flowOf(SomeDataItem(value = "blah")))
 *     appPrefs.someBooleanMap.set("key", flowOf(false))
 *     appPrefs.someBooleanMap.set("key", true)
 *     appPrefs.someObjectMap.set("key", flowOf(SomeDataItem(value = "blah")))
 *     ...
 *     viewModelScope.launch {
 *         appPrefs.someString.get().collect { text: String? ->
 *
 *         }
 *     }
 *     ...
 * }
 * ```
 */
abstract class StoreBase(val dataStore: DataStore<Preferences>) {

    protected val gson = Gson()

    protected inline fun <reified T> gsonSerializablePrefsDelegate(
        key: String,
        size: Int = 1 * 1024 * 1024, // 1MB
    ): StoreItem<T> {
        return StoreItemImpl(
            key = stringPreferencesKey(key),
            dataStore = dataStore,
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
            prefs = dataStore,
            mapper = getPrefsDelegateMapper(size, T::class.java),
        )
    }

    protected fun <T> getPrefsDelegateMapper(size: Int, type: Class<T>): StoreMapper<T, String> =
        object : StoreMapper<T, String> {

            private val cache: LruCache<String, T> = LruCache(size)

            override fun toStoreType(value: T?): String? {
                return value?.let {
                    val serialised = gson.toJson(it)
                    cache.put(serialised, it)
                    serialised
                }
            }

            @Suppress("NAME_SHADOWING")
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
            dataStore = dataStore,
        )
    }

    protected fun doubleDelegate(key: String): StoreItem<Double> {
        return StoreItemImpl(
            key = doublePreferencesKey(key),
            dataStore = dataStore,
        )
    }

    protected fun stringDelegate(key: String): StoreItem<String> {
        return StoreItemImpl(
            key = stringPreferencesKey(key),
            dataStore = dataStore,
        )
    }

    protected fun booleanMapPrefsDelegate(keyBase: String): MapStoreItem<Boolean> {
        return MapStoreItemImpl(
            key = keyBase,
            createKey = { booleanPreferencesKey(it) },
            prefs = dataStore,
        )
    }

    protected fun stringMapPrefsDelegate(keyBase: String): MapStoreItem<String> {
        return MapStoreItemImpl(
            key = keyBase,
            createKey = { stringPreferencesKey(it) },
            prefs = dataStore,
        )
    }
}