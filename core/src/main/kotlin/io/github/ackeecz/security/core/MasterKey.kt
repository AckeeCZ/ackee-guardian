package io.github.ackeecz.security.core

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import androidx.annotation.VisibleForTesting
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.security.GeneralSecurityException
import java.security.KeyStore
import java.security.ProviderException
import javax.crypto.KeyGenerator
import kotlin.also
import kotlin.collections.contentEquals
import kotlin.collections.contentToString

private val keyMutex = Mutex()

/**
 * Represents master keys that are used to encrypt data encryption keys for encrypting files, shared
 * preferences, etc. [MasterKey]s are stored securely in Android [KeyStore]. You can get [MasterKey]
 * by calling [getOrCreate] method and pass [KeyGenParameterSpec], which can be built manually or
 * by using [createSafeDefaultSpecBuilder] method for getting a [KeyGenParameterSpec.Builder]
 * configured with safe default parameters.
 */
public data class MasterKey private constructor(public val alias: String) {

    public val keyStoreUri: String = "$KEYSTORE_URI_SCHEME$alias"

    public companion object {

        private const val MASTER_KEY_ALIAS = "_androidx_security_master_key_"
        private const val KEY_SIZE = 256
        private const val KEYSTORE_URI_SCHEME: String = "android-keystore://"

        private val provider = Provider(defaultDispatcher = Dispatchers.Default)

        /**
         * Provides a safe default [KeyGenParameterSpec.Builder] with the settings:
         * Algorithm: AES
         * Block Mode: GCM
         * Padding: No Padding
         * Key Size: 256
         *
         * @param keyAlias The alias for the master key
         */
        public fun createSafeDefaultSpecBuilder(
            keyAlias: String = MASTER_KEY_ALIAS,
        ): KeyGenParameterSpec.Builder {
            return KeyGenParameterSpec.Builder(
                keyAlias,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(KEY_SIZE)
        }

        /**
         * Gets the [MasterKey]. If it does not exist yet, it is created with the provided
         * [keyGenParameterSpec].
         */
        public suspend fun getOrCreate(
            keyGenParameterSpec: KeyGenParameterSpec = createSafeDefaultSpecBuilder().build(),
        ): MasterKey {
            return provider.getOrCreate(keyGenParameterSpec)
        }
    }

    @VisibleForTesting
    internal class Provider(private val defaultDispatcher: CoroutineDispatcher) {

        private val keyStore = KeyStore.getInstance(ANDROID_KEY_STORE).also { it.load(null) }

        suspend fun getOrCreate(keyGenParameterSpec: KeyGenParameterSpec): MasterKey {
            validate(keyGenParameterSpec)
            generateKeyIfNeeded(keyGenParameterSpec)
            return MasterKey(keyGenParameterSpec.keystoreAlias)
        }

        private fun validate(spec: KeyGenParameterSpec) {
            require(spec.keySize == KEY_SIZE) {
                "Invalid key size. Required $KEY_SIZE bits, got ${spec.keySize} bits"
            }
            require(spec.blockModes.contentEquals(arrayOf(KeyProperties.BLOCK_MODE_GCM))) {
                "Invalid block mode. Required ${KeyProperties.BLOCK_MODE_GCM}, got ${spec.blockModes.contentToString()}"
            }
            require(spec.purposes == (KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)) {
                "Invalid key purposes. Required PURPOSE_ENCRYPT or PURPOSE_DECRYPT, got ${spec.purposes}"
            }
            require(spec.encryptionPaddings.contentEquals(arrayOf(KeyProperties.ENCRYPTION_PADDING_NONE))) {
                "Invalid encryption padding. Required ${KeyProperties.ENCRYPTION_PADDING_NONE}, got ${spec.encryptionPaddings.contentToString()}"
            }
            require(!(spec.isUserAuthenticationRequired && spec.userAuthenticationValidityDurationSeconds < 1)) {
                "Per-operation authentication is not supported (userAuthenticationValidityDurationSeconds must be > 0)"
            }
        }

        private suspend fun generateKeyIfNeeded(spec: KeyGenParameterSpec) {
            fun keyDoesNotExist() = !keyStore.containsAlias(spec.keystoreAlias)

            if (keyDoesNotExist()) {
                keyMutex.withLock {
                    if (keyDoesNotExist()) {
                        generateKey(spec)
                    }
                }
            }
        }

        private suspend fun generateKey(keyGenParameterSpec: KeyGenParameterSpec) {
            withContext(defaultDispatcher) {
                try {
                    val keyGenerator = KeyGenerator.getInstance(
                        KeyProperties.KEY_ALGORITHM_AES,
                        ANDROID_KEY_STORE,
                    )
                    keyGenerator.init(keyGenParameterSpec)
                    keyGenerator.generateKey()
                } catch (providerException: ProviderException) {
                    // Android 10 (API 29) throws a ProviderException under certain circumstances. Wrap
                    // that as a GeneralSecurityException so it's more consistent across API levels.
                    throw GeneralSecurityException(providerException.message, providerException)
                }
            }
        }

        companion object {

            private const val ANDROID_KEY_STORE = "AndroidKeyStore"
        }
    }
}
