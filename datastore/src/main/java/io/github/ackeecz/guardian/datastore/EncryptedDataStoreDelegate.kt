package io.github.ackeecz.guardian.datastore

import android.content.Context
import androidx.datastore.core.CorruptionException
import androidx.datastore.core.DataMigration
import androidx.datastore.core.DataStore
import androidx.datastore.core.DataStoreFactory
import androidx.datastore.core.Serializer
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.dataStoreFile
import io.github.ackeecz.guardian.core.keystore.android.AndroidKeyStoreSemaphore
import io.github.ackeecz.guardian.datastore.core.DataStoreCryptoParams
import io.github.ackeecz.guardian.datastore.core.internal.DataStoreDelegate
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.sync.Semaphore
import java.security.KeyStore
import kotlin.properties.ReadOnlyProperty

/**
 * Creates a property delegate for an encrypted single process [DataStore]. This should only be
 * called once in a file (at the top level), and all usages of the [DataStore] should use a reference
 * to the same instance. The receiver type for the property delegate must be an instance of [Context].
 *
 * All Android [KeyStore] operations are synchronized using a provided [keyStoreSemaphore].
 * More info about this topic can be found in [AndroidKeyStoreSemaphore] documentation.
 *
 * Example usage:
 * ```
 * val Context.myDataStore by encryptedDataStore(
 *     cryptoParams = DataStoreCryptoParams(
 *         encryptionScheme = DataStoreEncryptionScheme.AES256_GCM_HKDF_4KB,
 *         getMasterKey = { MasterKey.getOrCreate() },
 *     ),
 *     fileName = "filename",
 *     serializer = serializer,
 * )
 *
 * class SomeClass(val context: Context) {
 *    suspend fun update() = context.myDataStore.updateData {...}
 * }
 * ```
 *
 * @param cryptoParams Provides cryptographic parameters like used master key or encryption scheme
 * for the data encryption key used to encrypt/decrypt data.
 * @param fileName the filename relative to Context.applicationContext.filesDir that DataStore
 * acts on. The file is obtained from [dataStoreFile]. It is created in the "/datastore"
 * subdirectory.
 * @param serializer The serializer for `T`.
 * @param corruptionHandler The corruptionHandler is invoked if DataStore encounters a
 * [CorruptionException] when attempting to read data. CorruptionExceptions
 * are thrown by serializers when data can not be de-serialized.
 * @param produceMigrations produce the migrations. The ApplicationContext is passed in to these
 * callbacks as a parameter. DataMigrations are run before any access to data can occur. Each
 * producer and migration may be run more than once whether or not it already succeeded
 * (potentially because another migration failed or a write to disk failed.)
 * @param scope The scope in which IO operations and transform functions will execute.
 * @param keyStoreSemaphore [Semaphore] used to synchronize Android [KeyStore] operations.
 * It is recommended to use a default [AndroidKeyStoreSemaphore], if you really don't need
 * to provide a custom [Semaphore].
 *
 * @return a property delegate that manages an encrypted datastore as a singleton.
 */
@JvmOverloads
@Suppress("LongParameterList")
public fun <T> encryptedDataStore(
    cryptoParams: DataStoreCryptoParams,
    fileName: String,
    serializer: Serializer<T>,
    corruptionHandler: ReplaceFileCorruptionHandler<T>? = null,
    produceMigrations: (Context) -> List<DataMigration<T>> = { emptyList() },
    scope: CoroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob()),
    keyStoreSemaphore: Semaphore = AndroidKeyStoreSemaphore,
): ReadOnlyProperty<Context, DataStore<T>> {
    return EncryptedDataStoreDelegate(
        cryptoParams = cryptoParams,
        fileName = fileName,
        serializer = serializer,
        corruptionHandler = corruptionHandler,
        produceMigrations = produceMigrations,
        scope = scope,
        keyStoreSemaphore = keyStoreSemaphore,
    )
}

@Suppress("LongParameterList")
private class EncryptedDataStoreDelegate<T>(
    private val cryptoParams: DataStoreCryptoParams,
    private val fileName: String,
    private val serializer: Serializer<T>,
    private val corruptionHandler: ReplaceFileCorruptionHandler<T>?,
    private val produceMigrations: (Context) -> List<DataMigration<T>>,
    private val scope: CoroutineScope,
    private val keyStoreSemaphore: Semaphore,
) : DataStoreDelegate<T>() {

    override fun createDataStore(applicationContext: Context): DataStore<T> {
        return DataStoreFactory.createEncrypted(
            context = applicationContext,
            cryptoParams = cryptoParams,
            serializer = serializer,
            corruptionHandler = corruptionHandler,
            migrations = produceMigrations(applicationContext),
            scope = scope,
            keyStoreSemaphore = keyStoreSemaphore,
            produceFile = { applicationContext.dataStoreFile(fileName) },
        )
    }
}
