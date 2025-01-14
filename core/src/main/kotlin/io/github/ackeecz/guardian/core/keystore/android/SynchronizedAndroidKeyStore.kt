package io.github.ackeecz.guardian.core.keystore.android

import io.github.ackeecz.guardian.core.internal.SynchronizedDataHolder
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import java.security.Key
import java.security.KeyStore
import java.security.cert.Certificate
import java.util.Date
import java.util.Enumeration

/**
 * Abstraction over Android [KeyStore] that synchronizes all operations using a provided [Semaphore].
 * More info about this topic can be found in [AndroidKeyStoreSemaphore] documentation.
 */
public interface SynchronizedAndroidKeyStore {

    /**
     * See [KeyStore.getType]
     */
    public suspend fun getType(): String

    /**
     * See [KeyStore.getKey]
     */
    public suspend fun getKey(alias: String, password: CharArray?): Key

    /**
     * See [KeyStore.getCertificateChain]
     */
    public suspend fun getCertificateChain(alias: String): Array<Certificate>

    /**
     * See [KeyStore.getCertificate]
     */
    public suspend fun getCertificate(alias: String): Certificate

    /**
     * See [KeyStore.getCreationDate]
     */
    public suspend fun getCreationDate(alias: String): Date

    /**
     * See [KeyStore.setKeyEntry]
     */
    public suspend fun setKeyEntry(
        alias: String,
        key: Key,
        password: CharArray?,
        chain: Array<Certificate>?,
    )

    /**
     * See [KeyStore.setKeyEntry]
     */
    public suspend fun setKeyEntry(alias: String, key: ByteArray, chain: Array<Certificate>?)

    /**
     * See [KeyStore.setCertificateEntry]
     */
    public suspend fun setCertificateEntry(alias: String, cert: Certificate)

    /**
     * See [KeyStore.deleteEntry]
     */
    public suspend fun deleteEntry(alias: String)

    /**
     * See [KeyStore.aliases]
     */
    public suspend fun aliases(): Enumeration<String>

    /**
     * See [KeyStore.containsAlias]
     */
    public suspend fun containsAlias(alias: String): Boolean

    /**
     * See [KeyStore.size]
     */
    public suspend fun size(): Int

    /**
     * See [KeyStore.isKeyEntry]
     */
    public suspend fun isKeyEntry(alias: String): Boolean

    /**
     * See [KeyStore.isCertificateEntry]
     */
    public suspend fun isCertificateEntry(alias: String): Boolean

    /**
     * See [KeyStore.getCertificateAlias]
     */
    public suspend fun getCertificateAlias(cert: Certificate): String

    /**
     * See [KeyStore.getEntry]
     */
    public suspend fun getEntry(
        alias: String,
        protectionParameter: KeyStore.ProtectionParameter?,
    ): KeyStore.Entry

    /**
     * See [KeyStore.setEntry]
     */
    public suspend fun setEntry(
        alias: String,
        entry: KeyStore.Entry,
        protectionParameter: KeyStore.ProtectionParameter?,
    )

    /**
     * See [KeyStore.entryInstanceOf]
     */
    public suspend fun entryInstanceOf(alias: String, entryClass: Class<out KeyStore.Entry>): Boolean

    public companion object {

        /**
         * Creates a new instance of [SynchronizedAndroidKeyStore].
         *
         * @param keyStoreSemaphore [Semaphore] used to synchronize Android [KeyStore] operations.
         * It is recommended to use a default [AndroidKeyStoreSemaphore], if you really don't need
         * to provide a custom [Semaphore].
         */
        public operator fun invoke(
            keyStoreSemaphore: Semaphore = AndroidKeyStoreSemaphore,
        ): SynchronizedAndroidKeyStore {
            return SynchronizedAndroidKeyStoreImpl(keyStoreSemaphore)
        }
    }
}

private class SynchronizedAndroidKeyStoreImpl(
    private val keyStoreSemaphore: Semaphore,
) : SynchronizedAndroidKeyStore {

    private val keyStoreHolder = KeyStoreHolder()

    override suspend fun getType(): String = withSynchronizedKeyStore { type }

    private suspend fun <T : Any> withSynchronizedKeyStore(
        action: suspend KeyStore.() -> T,
    ): T {
        val keyStore = keyStoreHolder.getOrCreate()
        return keyStoreSemaphore.withPermit { keyStore.action() }
    }

    override suspend fun getKey(alias: String, password: CharArray?): Key {
        return withSynchronizedKeyStore { getKey(alias, password) }
    }

    override suspend fun getCertificateChain(alias: String): Array<Certificate> {
        return withSynchronizedKeyStore { getCertificateChain(alias) }
    }

    override suspend fun getCertificate(alias: String): Certificate {
        return withSynchronizedKeyStore { getCertificate(alias) }
    }

    override suspend fun getCreationDate(alias: String): Date {
        return withSynchronizedKeyStore { getCreationDate(alias) }
    }

    override suspend fun setKeyEntry(
        alias: String,
        key: Key,
        password: CharArray?,
        chain: Array<Certificate>?,
    ) {
        withSynchronizedKeyStore { setKeyEntry(alias, key, password, chain) }
    }

    override suspend fun setKeyEntry(alias: String, key: ByteArray, chain: Array<Certificate>?) {
        withSynchronizedKeyStore { setKeyEntry(alias, key, chain) }
    }

    override suspend fun setCertificateEntry(alias: String, cert: Certificate) {
        withSynchronizedKeyStore { setCertificateEntry(alias, cert) }
    }

    override suspend fun deleteEntry(alias: String) {
        withSynchronizedKeyStore { deleteEntry(alias) }
    }

    override suspend fun aliases(): Enumeration<String> {
        return withSynchronizedKeyStore { aliases() }
    }

    override suspend fun containsAlias(alias: String): Boolean {
        return withSynchronizedKeyStore { containsAlias(alias) }
    }

    override suspend fun size(): Int = withSynchronizedKeyStore { size() }

    override suspend fun isKeyEntry(alias: String): Boolean {
        return withSynchronizedKeyStore { isKeyEntry(alias) }
    }

    override suspend fun isCertificateEntry(alias: String): Boolean {
        return withSynchronizedKeyStore { isCertificateEntry(alias) }
    }

    override suspend fun getCertificateAlias(cert: Certificate): String {
        return withSynchronizedKeyStore { getCertificateAlias(cert) }
    }

    override suspend fun getEntry(
        alias: String,
        protectionParameter: KeyStore.ProtectionParameter?,
    ): KeyStore.Entry {
        return withSynchronizedKeyStore { getEntry(alias, protectionParameter) }
    }

    override suspend fun setEntry(
        alias: String,
        entry: KeyStore.Entry,
        protectionParameter: KeyStore.ProtectionParameter?,
    ) {
        withSynchronizedKeyStore { setEntry(alias, entry, protectionParameter) }
    }

    override suspend fun entryInstanceOf(alias: String, entryClass: Class<out KeyStore.Entry>): Boolean {
        return withSynchronizedKeyStore { entryInstanceOf(alias, entryClass) }
    }

    private inner class KeyStoreHolder : SynchronizedDataHolder<KeyStore>() {

        override suspend fun createSynchronizedData(): KeyStore = keyStoreSemaphore.withPermit {
            KeyStore.getInstance(ANDROID_KEY_STORE_ID).also { it.load(null) }
        }
    }
}
