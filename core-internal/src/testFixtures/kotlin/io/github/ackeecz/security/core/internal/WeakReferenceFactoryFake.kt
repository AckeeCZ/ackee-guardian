package io.github.ackeecz.security.core.internal

import java.lang.ref.WeakReference

class WeakReferenceFactoryFake : WeakReferenceFactory {

    private val managedReferents = mutableListOf<WeakReference<*>>()

    override fun <T> create(referent: T): WeakReference<T> {
        return WeakReference(referent).also { managedReferents.add(it) }
    }

    fun <T> clear(referent: T) {
        managedReferents.filter { it.get() == referent }
            .forEach {
                it.clear()
                managedReferents -= it
            }
    }

    fun clearAll() {
        managedReferents.forEach { it.clear() }
        managedReferents.clear()
    }
}
