package io.github.ackeecz.guardian.core.internal

import java.lang.ref.WeakReference

public class WeakReferenceFactoryFake : WeakReferenceFactory {

    private val managedReferents = mutableListOf<WeakReference<*>>()

    override fun <T> create(referent: T): WeakReference<T> {
        return WeakReference(referent).also { managedReferents.add(it) }
    }

    public fun <T> clear(referent: T) {
        managedReferents.filter { it.get() == referent }
            .forEach {
                it.clear()
                managedReferents -= it
            }
    }

    public fun clearAll() {
        managedReferents.forEach { it.clear() }
        managedReferents.clear()
    }
}
