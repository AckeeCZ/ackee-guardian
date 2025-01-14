/*
 * Copyright 2019 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * This file is based on the original MasterKeys from Jetpack Security Crypto library
 * https://developer.android.com/reference/kotlin/androidx/security/crypto/MasterKeys
 */
package io.github.ackeecz.guardian.core

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import androidx.annotation.VisibleForTesting
import io.github.ackeecz.guardian.core.MasterKey.Companion.createSafeDefaultSpecBuilder
import io.github.ackeecz.guardian.core.MasterKey.Companion.getOrCreate
import io.github.ackeecz.guardian.core.keystore.android.AndroidKeyStoreSemaphore
import io.github.ackeecz.guardian.core.keystore.android.SynchronizedAndroidKeyGenerator
import io.github.ackeecz.guardian.core.keystore.android.SynchronizedAndroidKeyStore
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.security.GeneralSecurityException
import java.security.KeyStore
import java.security.ProviderException

private val keyMutex = Mutex()

/**
 * Represents master keys that are used to encrypt data encryption keys for encrypting files, shared
 * preferences, etc. [MasterKey]s are stored securely in Android [KeyStore]. You can get [MasterKey]
 * by calling [getOrCreate] method and pass [KeyGenParameterSpec], which can be built manually or
 * by using [createSafeDefaultSpecBuilder] method for getting a [KeyGenParameterSpec.Builder]
 * configured with safe default parameters.
 *
 * Operations with Android [KeyStore] are synchronized using a provided [Semaphore]. More info about
 * this topic can be found in [AndroidKeyStoreSemaphore] documentation.
 */
public class MasterKey private constructor(public val alias: String) {

    public val keyStoreUri: String = "$KEYSTORE_URI_SCHEME$alias"

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as MasterKey

        return alias == other.alias
    }

    override fun hashCode(): Int {
        return alias.hashCode()
    }

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
         *
         * Android [KeyStore] operations are synchronized using [keyStoreSemaphore]. It is recommended
         * to use a default [AndroidKeyStoreSemaphore], if you really don't need to provide a custom
         * [Semaphore].
         */
        @JvmOverloads
        public suspend fun getOrCreate(
            keyGenParameterSpec: KeyGenParameterSpec = createSafeDefaultSpecBuilder().build(),
            keyStoreSemaphore: Semaphore = AndroidKeyStoreSemaphore,
        ): MasterKey {
            return provider.getOrCreate(keyGenParameterSpec, keyStoreSemaphore)
        }
    }

    @VisibleForTesting
    internal class Provider(private val defaultDispatcher: CoroutineDispatcher) {

        suspend fun getOrCreate(
            keyGenParameterSpec: KeyGenParameterSpec,
            keyStoreSemaphore: Semaphore,
        ): MasterKey {
            validate(keyGenParameterSpec)
            generateKeyIfNeeded(keyGenParameterSpec, keyStoreSemaphore)
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

        private suspend fun generateKeyIfNeeded(spec: KeyGenParameterSpec, keyStoreSemaphore: Semaphore) {
            val keyStore = SynchronizedAndroidKeyStore(keyStoreSemaphore)

            suspend fun keyDoesNotExist() = !keyStore.containsAlias(spec.keystoreAlias)

            if (keyDoesNotExist()) {
                keyMutex.withLock {
                    if (keyDoesNotExist()) {
                        generateKey(spec, keyStoreSemaphore)
                    }
                }
            }
        }

        private suspend fun generateKey(
            keyGenParameterSpec: KeyGenParameterSpec,
            keyStoreSemaphore: Semaphore,
        ) {
            withContext(defaultDispatcher) {
                try {
                    val keyGenerator = SynchronizedAndroidKeyGenerator.getInstance(
                        KeyProperties.KEY_ALGORITHM_AES,
                        keyStoreSemaphore,
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
    }
}
