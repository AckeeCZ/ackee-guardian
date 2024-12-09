package io.github.ackeecz.guardian.core.internal

import java.lang.ref.WeakReference

public interface WeakReferenceFactory {

    public fun <T> create(referent: T): WeakReference<T>

    public companion object {

        public operator fun invoke(): WeakReferenceFactory = object : WeakReferenceFactory {

            override fun <T> create(referent: T): WeakReference<T> {
                return WeakReference(referent)
            }
        }
    }
}
