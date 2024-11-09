package io.github.ackeecz.security.datastore.core.internal

import androidx.datastore.core.DataStore

/**
 * Local extension properties are prohibited so we use a trick with a property delegate wrapper
 * to declare and access DataStore delegate in test methods
 */
interface DataStoreDelegateWrapper<T : Any> {

    // This is intentionally a function to always properly delegate to DataStore delegate property
    // and to not just access a stored DataStore from a property field! This would lead to access
    // the DataStore delegate just once, which might effect negatively some tests, e.g. testing
    // if the delegate returns a singleton instance.
    fun getDataStore(): DataStore<T>
}
