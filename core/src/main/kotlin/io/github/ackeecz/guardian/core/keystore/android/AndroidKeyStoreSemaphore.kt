package io.github.ackeecz.guardian.core.keystore.android

import android.app.Application
import io.github.ackeecz.guardian.core.keystore.android.AndroidKeyStoreSemaphore.setPermits
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.Semaphore
import java.security.KeyPairGenerator
import java.security.KeyStore
import java.security.Signature
import javax.crypto.Cipher
import javax.crypto.KeyGenerator

/**
 * [Semaphore] used to synchronize all Android KeyStore operations in Guardian. You can use this to
 * synchronize other Android KeyStore operations in your app, that you execute outside Guardian.
 *
 * Android KeyStore is not generally thread-safe and you can encounter errors if you execute
 * concurrent operations with Android KeyStore. Since KeyStore is implemented by device manufacturers,
 * its behaviour can (and does) vary a lot on different devices. Some can handle concurrency better
 * (e.g. Pixel) with faster KeyStores and possibly even allowing a concurrency to some extent, but
 * some (e.g. Motorola) can have very slow KeyStores and not allowing any concurrent operations.
 * Moreover, slow KeyStores naturally enables the errors caused by concurrency to happen more often.
 * Because of this, the only safe option is to synchronize all KeyStore operations in the whole app and
 * [AndroidKeyStoreSemaphore] does this in Guardian. It is public, so you can synchronize your other
 * custom Android KeyStore operations in your app with the operations happening in Guardian, so
 * everything is synchronized together.
 *
 * It is important to know that by Android KeyStore operations it is meant all operations that
 * execute the Android KeyStore service on the device. This involves operations with [KeyStore] class
 * backed by AndroidKeyStore JCA provider, but also all other classes from JCA backed by the same
 * provider, which internally communicates with Android KeyStore service like [KeyGenerator] and
 * [KeyPairGenerator] for generating keys in Android KeyStore, [Cipher] for encryption/decryption and
 * [Signature] for generating signatures using keys stored in the Android KeyStore, etc. To properly
 * handle Android KeyStore concurrency among various devices, you need to synchronize operations in
 * all those mentioned and similar JCA classes when backed by Android KeyStore. Guardian provides
 * already synchronized abstractions over some of those classes like [SynchronizedAndroidKeyStore],
 * [SynchronizedAndroidKeyGenerator] or [SynchronizedAndroidKeyPairGenerator] that use
 * [AndroidKeyStoreSemaphore] for synchronization by default. You can use those synchronized classes
 * or just implement your own synchronization using [AndroidKeyStoreSemaphore].
 *
 * It is also possible to alter the number of parallel operations that can run at the same time.
 * The default value is 1, so the behaviour is equivalent to [Mutex]. It is **HIGHLY** recommended
 * **NOT** to change this default, because this is the only safe option if you target a wide range of
 * various devices. However, if your app targets a specific tested devices, performs a lot of
 * Android KeyStore operations and you need to improve the performance, you might want to increase
 * the number of parallel operations. For example from our testing it looks like that Pixel devices
 * (at least tested Pixel 8 with Android 15) is probably capable of running at most 4 parallel
 * operations successfully (We did just a simple test, not covering all possible types of operations,
 * so this might not be actually true. It might also change in the future.). So if you are really sure,
 * know what you are doing and really do need this performance improvement, you can increase the
 * maximum number of parallel operations by using [setPermits]. Be sure to call this at the start of
 * the app, ideally in [Application.onCreate] before any other Guardian call or other usage of
 * [AndroidKeyStoreSemaphore].
 *
 * Guardian also allows you to use your own [Semaphore] implementation, if you need to. Every Guardian
 * API, working with Android KeyStore under the hood, allows you to pass your own instance of
 * [Semaphore], using [AndroidKeyStoreSemaphore] as a default value. However, it is then your responsibility
 * to ensure, that you really pass the same [Semaphore] instance everywhere and override a default
 * [AndroidKeyStoreSemaphore] value, avoiding an unwanted unsafe of multiple [Semaphore] instances,
 * thus allowing a concurrency errors to happen.
 */
public object AndroidKeyStoreSemaphore : Semaphore {

    private var delegate: Semaphore = Semaphore(permits = 1)

    override val availablePermits: Int get() = delegate.availablePermits

    override suspend fun acquire() {
        delegate.acquire()
    }

    override fun release() {
        delegate.release()
    }

    override fun tryAcquire(): Boolean = delegate.tryAcquire()

    /**
     * Changes the number of [permits] of this [Semaphore]. It is **HIGHLY** recommended **NOT** to
     * change a default value of permits, to prevent concurrency errors. More info in the documentation
     * of this class.
     */
    public fun setPermits(permits: Int) {
        delegate = Semaphore(permits = permits)
    }
}
