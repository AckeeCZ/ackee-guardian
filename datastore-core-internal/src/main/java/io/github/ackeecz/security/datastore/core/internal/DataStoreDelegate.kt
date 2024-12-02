package io.github.ackeecz.security.datastore.core.internal

import android.content.Context
import androidx.datastore.core.DataStore
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

public abstract class DataStoreDelegate<T> : ReadOnlyProperty<Context, DataStore<T>> {

    private val lock = Any()

    @Volatile
    private var dataStoreSingleton: DataStore<T>? = null

    override fun getValue(thisRef: Context, property: KProperty<*>): DataStore<T> {
        return dataStoreSingleton ?: synchronized(lock) {
            if (dataStoreSingleton == null) {
                val applicationContext = thisRef.applicationContext
                dataStoreSingleton = createDataStore(applicationContext)
            }
            requireNotNull(dataStoreSingleton)
        }
    }

    protected abstract fun createDataStore(applicationContext: Context): DataStore<T>
}
