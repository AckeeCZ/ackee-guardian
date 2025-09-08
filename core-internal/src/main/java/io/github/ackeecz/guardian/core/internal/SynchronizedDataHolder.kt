package io.github.ackeecz.guardian.core.internal

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Holder of [T], which creation is synchronized in the scope of the instance of
 * this class. Creation is not synchronized among multiple instances of this class, i.e. creation of
 * [T] can run concurrently in multiple threads, where each operates on different
 * instance of this class.
 */
public abstract class SynchronizedDataHolder<T : Any> {

    private val mutex = Mutex()

    @Volatile
    private var synchronizedData: T? = null

    /**
     * Gets [T] or creates it using [createSynchronizedData]
     */
    public suspend fun getOrCreate(): T {
        return synchronizedData ?: mutex.withLock {
            if (synchronizedData == null) {
                synchronizedData = createSynchronizedData()
            }
            checkNotNull(synchronizedData)
        }
    }

    /**
     * Creates [T]. This method is synchronized in the scope of this instance, i.e. for each
     * [SynchronizedDataHolder] instance this will be called just once.
     */
    protected abstract suspend fun createSynchronizedData(): T
}
