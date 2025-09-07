package io.github.ackeecz.guardian.core.internal

import android.content.Context
import androidx.annotation.VisibleForTesting
import com.google.crypto.tink.Aead
import com.google.crypto.tink.DeterministicAead
import com.google.crypto.tink.KeyTemplate
import com.google.crypto.tink.KeysetHandle
import com.google.crypto.tink.RegistryConfiguration
import com.google.crypto.tink.StreamingAead
import com.google.crypto.tink.aead.AeadConfig
import com.google.crypto.tink.daead.DeterministicAeadConfig
import com.google.crypto.tink.streamingaead.StreamingAeadConfig
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withLock

/**
 * Singleton object providing Tink primitives. Caches keysets in memory if specified in [KeysetConfig].
 */
public object TinkPrimitiveProvider {

    private val cacheMutex = Mutex()
    private val keysetHandleCache = mutableMapOf<KeysetId, KeysetHandle>()
    private val appliedKeysetConfigs = mutableMapOf<KeysetId, KeysetConfig>()

    private val KeysetConfig.keysetId get() = KeysetId(prefsName = prefsName, alias = alias)

    public suspend fun getStreamingAead(params: Params): StreamingAead {
        StreamingAeadConfig.register()
        val keysetHandle = getOrCreateKeysetHandle(params)
        return keysetHandle.getPrimitive(RegistryConfiguration.get(), StreamingAead::class.java)
    }

    private suspend fun getOrCreateKeysetHandle(params: Params): KeysetHandle {
        validateKeysetConfig(params)
        val keysetConfig = params.keysetConfig
        return if (keysetConfig.cacheKeyset) {
            val keysetId = keysetConfig.keysetId
            keysetHandleCache[keysetId] ?: cacheMutex.withLock {
                if (keysetHandleCache[keysetId] == null) {
                    keysetHandleCache[keysetId] = createKeysetHandle(params)
                }
                checkNotNull(keysetHandleCache[keysetId])
            }
        } else {
            createKeysetHandle(params)
        }
    }

    private fun validateKeysetConfig(params: Params) {
        val keysetConfig = params.keysetConfig
        val keysetId = keysetConfig.keysetId
        val appliedConfig = appliedKeysetConfigs[keysetId]
        if (appliedConfig == null) {
            appliedKeysetConfigs[keysetId] = keysetConfig
        } else {
            if (appliedConfig.cacheKeyset != keysetConfig.cacheKeyset) {
                throw InvalidKeysetConfigException(
                    "All configurations of the same key must have the same caching configuration. " +
                        "You provided two different values for key identified by " +
                        "prefsName=${keysetConfig.prefsName} and alias=${keysetConfig.alias}."
                )
            }
        }
    }

    private suspend fun createKeysetHandle(params: Params): KeysetHandle {
        val keysetConfig = params.keysetConfig
        return AndroidKeysetManagerSynchronizedBuilder(params.keyStoreSemaphore)
            .withKeyTemplate(keysetConfig.keyTemplate)
            .withSharedPref(
                params.context,
                keysetConfig.alias,
                keysetConfig.prefsName,
            )
            .withMasterKeyUri(params.masterKeyUri)
            .build()
            .keysetHandle
    }

    public suspend fun getDeterministicAead(params: Params): DeterministicAead {
        DeterministicAeadConfig.register()
        val keysetHandle = getOrCreateKeysetHandle(params)
        return keysetHandle.getPrimitive(RegistryConfiguration.get(), DeterministicAead::class.java)
    }

    public suspend fun getAead(params: Params): Aead {
        AeadConfig.register()
        val keysetHandle = getOrCreateKeysetHandle(params)
        return keysetHandle.getPrimitive(RegistryConfiguration.get(), Aead::class.java)
    }

    @VisibleForTesting
    internal fun clear() {
        keysetHandleCache.clear()
        appliedKeysetConfigs.clear()
    }

    private data class KeysetId(val prefsName: String, val alias: String)

    public data class Params(
        val context: Context,
        val masterKeyUri: String,
        val keyStoreSemaphore: Semaphore,
        val keysetConfig: KeysetConfig,
    )

    public data class KeysetConfig(
        val keyTemplate: KeyTemplate,
        val prefsName: String,
        val alias: String,
        val cacheKeyset: Boolean,
    )
}

public class InvalidKeysetConfigException(message: String) : Exception(message)
