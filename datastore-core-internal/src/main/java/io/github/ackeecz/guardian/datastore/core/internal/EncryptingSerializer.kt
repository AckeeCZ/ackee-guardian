package io.github.ackeecz.guardian.datastore.core.internal

import android.content.Context
import androidx.datastore.core.Serializer
import com.google.crypto.tink.StreamingAead
import com.google.crypto.tink.streamingaead.StreamingAeadConfig
import io.github.ackeecz.guardian.core.internal.AndroidKeysetManagerSynchronizedBuilder
import io.github.ackeecz.guardian.core.internal.SynchronizedDataHolder
import io.github.ackeecz.guardian.datastore.core.DataStoreCryptoParams
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.withContext
import java.io.File
import java.io.InputStream
import java.io.OutputStream
import java.security.KeyStore

/**
 * [Serializer] that encrypts and decrypts [delegate]. Crypto objects are configured using
 * [DataStoreCryptoParams].
 *
 * @param keyStoreSemaphore is used to synchronize Android [KeyStore] operations.
 */
public class EncryptingSerializer<T>(
    context: Context,
    cryptoParams: DataStoreCryptoParams,
    private val delegate: Serializer<T>,
    scope: CoroutineScope,
    keyStoreSemaphore: Semaphore,
    dataStoreFile: File,
) : Serializer<T> {

    private val streamingAeadHolder = StreamingAeadHolder(context, cryptoParams, scope, keyStoreSemaphore)
    private val associatedData = dataStoreFile.name.toByteArray(Charsets.UTF_8)

    override val defaultValue: T = delegate.defaultValue

    override suspend fun writeTo(t: T, output: OutputStream) {
        val streamingAead = streamingAeadHolder.getOrCreate()
        streamingAead.newEncryptingStream(output, associatedData).use { encryptingStream ->
            delegate.writeTo(t, encryptingStream)
        }
    }

    override suspend fun readFrom(input: InputStream): T {
        val streamingAead = streamingAeadHolder.getOrCreate()
        return streamingAead.newDecryptingStream(input, associatedData).use { decryptingStream ->
            delegate.readFrom(decryptingStream)
        }
    }

    private class StreamingAeadHolder(
        context: Context,
        private val cryptoParams: DataStoreCryptoParams,
        private val scope: CoroutineScope,
        private val keyStoreSemaphore: Semaphore,
    ) : SynchronizedDataHolder<StreamingAead>() {

        private val context = context.applicationContext

        override suspend fun createSynchronizedData(): StreamingAead = withContext(scope.resolveDispatcher()) {
            StreamingAeadConfig.register()
            return@withContext AndroidKeysetManagerSynchronizedBuilder(keyStoreSemaphore)
                .withKeyTemplate(cryptoParams.encryptionScheme.keyTemplate)
                .withSharedPref(
                    context,
                    cryptoParams.keysetAlias,
                    cryptoParams.keysetPrefsName,
                )
                .withMasterKeyUri(cryptoParams.getMasterKey().keyStoreUri)
                .build()
                .keysetHandle
                .getPrimitive(StreamingAead::class.java)
        }

        private fun CoroutineScope.resolveDispatcher(): CoroutineDispatcher {
            return coroutineContext[CoroutineDispatcher.Key] ?: Dispatchers.IO
        }
    }
}
