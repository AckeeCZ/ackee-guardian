package io.github.ackeecz.guardian.core.keystore.android

import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import java.security.KeyStore
import java.security.SecureRandom
import java.security.spec.AlgorithmParameterSpec
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey

/**
 * Abstraction over [KeyGenerator] backed by AndroidKeyStore JCA provider, that generates keys in the
 * Android [KeyStore]. It synchronizes all operations involving Android [KeyStore] by using a
 * provided [Semaphore]. More info about this topic can be found in [AndroidKeyStoreSemaphore] documentation.
 */
public interface SynchronizedAndroidKeyGenerator {

    /**
     * See [KeyGenerator.getAlgorithm]
     */
    public val algorithm: String

    /**
     * See [KeyGenerator.init]
     */
    public fun init(random: SecureRandom)

    /**
     * See [KeyGenerator.init]
     */
    public fun init(params: AlgorithmParameterSpec)

    /**
     * See [KeyGenerator.init]
     */
    public fun init(params: AlgorithmParameterSpec, random: SecureRandom)

    /**
     * See [KeyGenerator.init]
     */
    public fun init(keySize: Int)

    /**
     * See [KeyGenerator.init]
     */
    public fun init(keySize: Int, random: SecureRandom)

    /**
     * See [KeyGenerator.generateKey]
     */
    public suspend fun generateKey(): SecretKey

    public companion object {

        /**
         * Gets the instance of [SynchronizedAndroidKeyGenerator].
         *
         * @param algorithm the standard name of the requested algorithm
         * @param keyStoreSemaphore [Semaphore] used to synchronize Android [KeyStore] operations.
         * It is recommended to use a default [AndroidKeyStoreSemaphore], if you really don't need
         * to provide a custom [Semaphore].
         */
        public fun getInstance(
            algorithm: String,
            keyStoreSemaphore: Semaphore = AndroidKeyStoreSemaphore,
        ): SynchronizedAndroidKeyGenerator {
            return SynchronizedAndroidKeyGeneratorImpl(algorithm, keyStoreSemaphore)
        }
    }
}

private class SynchronizedAndroidKeyGeneratorImpl(
    algorithm: String,
    private val keyStoreSemaphore: Semaphore,
) : SynchronizedAndroidKeyGenerator {

    private val delegate = KeyGenerator.getInstance(algorithm, ANDROID_KEY_STORE_ID)

    override val algorithm: String = delegate.algorithm

    override fun init(random: SecureRandom) {
        delegate.init(random)
    }

    override fun init(params: AlgorithmParameterSpec) {
        delegate.init(params)
    }

    override fun init(params: AlgorithmParameterSpec, random: SecureRandom) {
        delegate.init(params, random)
    }

    override fun init(keySize: Int) {
        delegate.init(keySize)
    }

    override fun init(keySize: Int, random: SecureRandom) {
        delegate.init(keySize, random)
    }

    override suspend fun generateKey(): SecretKey = invokeSynchronizedKeyStoreOperation {
        delegate.generateKey()
    }

    private suspend fun <T : Any> invokeSynchronizedKeyStoreOperation(
        operation: suspend () -> T,
    ): T = keyStoreSemaphore.withPermit { operation() }
}
