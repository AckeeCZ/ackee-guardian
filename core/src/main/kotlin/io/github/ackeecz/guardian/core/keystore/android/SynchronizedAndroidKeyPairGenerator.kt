package io.github.ackeecz.guardian.core.keystore.android

import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.KeyStore
import java.security.SecureRandom
import java.security.spec.AlgorithmParameterSpec

/**
 * Abstraction over [KeyPairGenerator] backed by AndroidKeyStore JCA provider, that generates key
 * pairs in the Android [KeyStore]. It synchronizes all operations involving Android [KeyStore] by
 * using a provided [Semaphore]. More info about this topic can be found in
 * [AndroidKeyStoreSemaphore] documentation.
 */
public interface SynchronizedAndroidKeyPairGenerator {

    /**
     * See [KeyPairGenerator.getAlgorithm]
     */
    public val algorithm: String

    /**
     * See [KeyPairGenerator.initialize]
     */
    public fun initialize(keySize: Int)

    /**
     * See [KeyPairGenerator.initialize]
     */
    public fun initialize(keySize: Int, random: SecureRandom)

    /**
     * See [KeyPairGenerator.initialize]
     */
    public fun initialize(params: AlgorithmParameterSpec)

    /**
     * See [KeyPairGenerator.initialize]
     */
    public fun initialize(params: AlgorithmParameterSpec, random: SecureRandom)

    /**
     * See [KeyPairGenerator.genKeyPair]
     */
    public suspend fun genKeyPair(): KeyPair

    /**
     * See [KeyPairGenerator.generateKeyPair]
     */
    public suspend fun generateKeyPair(): KeyPair

    public companion object {

        /**
         * Gets the instance of [SynchronizedAndroidKeyPairGenerator].
         *
         * @param algorithm the standard name of the requested algorithm
         * @param keyStoreSemaphore [Semaphore] used to synchronize Android [KeyStore] operations.
         * It is recommended to use a default [AndroidKeyStoreSemaphore], if you really don't need
         * to provide a custom [Semaphore].
         */
        public fun getInstance(
            algorithm: String,
            keyStoreSemaphore: Semaphore = AndroidKeyStoreSemaphore,
        ): SynchronizedAndroidKeyPairGenerator {
            return SynchronizedAndroidKeyPairGeneratorImpl(algorithm, keyStoreSemaphore)
        }
    }
}

private class SynchronizedAndroidKeyPairGeneratorImpl(
    algorithm: String,
    private val keyStoreSemaphore: Semaphore,
) : SynchronizedAndroidKeyPairGenerator {

    private val delegate = KeyPairGenerator.getInstance(algorithm, ANDROID_KEY_STORE_ID)

    override val algorithm: String = delegate.algorithm

    override fun initialize(keySize: Int) {
        delegate.initialize(keySize)
    }

    override fun initialize(keySize: Int, random: SecureRandom) {
        delegate.initialize(keySize, random)
    }

    override fun initialize(params: AlgorithmParameterSpec) {
        delegate.initialize(params)
    }

    override fun initialize(params: AlgorithmParameterSpec, random: SecureRandom) {
        delegate.initialize(params, random)
    }

    override suspend fun genKeyPair(): KeyPair {
        return invokeSynchronizedKeyStoreOperation { delegate.genKeyPair() }
    }

    private suspend fun <T : Any> invokeSynchronizedKeyStoreOperation(
        operation: suspend () -> T,
    ): T = keyStoreSemaphore.withPermit { operation() }

    override suspend fun generateKeyPair(): KeyPair {
        return invokeSynchronizedKeyStoreOperation { delegate.generateKeyPair() }
    }
}
