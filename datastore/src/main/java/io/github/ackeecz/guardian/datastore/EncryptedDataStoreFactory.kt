package io.github.ackeecz.guardian.datastore

import android.content.Context
import androidx.datastore.core.CorruptionException
import androidx.datastore.core.DataMigration
import androidx.datastore.core.DataStore
import androidx.datastore.core.DataStoreFactory
import androidx.datastore.core.Serializer
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.dataStoreFile
import io.github.ackeecz.guardian.datastore.core.DataStoreCryptoParams
import io.github.ackeecz.guardian.datastore.core.internal.EncryptingSerializer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import java.io.File

/**
 * Creates an instance of encrypted [DataStore]. Never create more than one instance of
 * DataStore for a given file. Doing so can break all DataStore functionality. You should
 * consider managing your DataStore instance as a singleton.
 *
 * @param context Context of the application. It is safe to pass an Activity context, but best
 * practice is to pass Application one to be sure to avoid potential memory leaks.
 * @param cryptoParams Provides cryptographic parameters like used master key or encryption scheme
 * for the data encryption key used to encrypt/decrypt data.
 * @param corruptionHandler The corruptionHandler is invoked if DataStore encounters a
 * [CorruptionException] when attempting to read data. CorruptionExceptions are thrown by
 * serializers when data cannot be de-serialized.
 * @param migrations are run before any access to data can occur. Each producer and migration
 * may be run more than once whether or not it already succeeded (potentially because another
 * migration failed or a write to disk failed.)
 * @param scope The scope in which IO operations and transform functions will execute.
 * @param produceFile Function which returns the file that the new DataStore will act on.
 * The function must return the same file every time. No two instances of DataStore
 * should act on the same file at the same time. File will be created if it doesn't exist. You can
 * use [Context.dataStoreFile] helper method to create a DataStore file in a standard location.
 *
 * @return a new encrypted DataStore instance with the provided configuration
 */
@Suppress("LongParameterList")
public fun <T> DataStoreFactory.createEncrypted(
    context: Context,
    cryptoParams: DataStoreCryptoParams,
    serializer: Serializer<T>,
    corruptionHandler: ReplaceFileCorruptionHandler<T>? = null,
    migrations: List<DataMigration<T>> = emptyList(),
    scope: CoroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob()),
    produceFile: () -> File,
): DataStore<T> {
    val dataStoreFile = produceFile()
    val encryptingSerializer = EncryptingSerializer(
        context = context,
        cryptoParams = cryptoParams,
        delegate = serializer,
        scope = scope,
        dataStoreFile = dataStoreFile,
    )
    return create(
        serializer = encryptingSerializer,
        corruptionHandler = corruptionHandler,
        migrations = migrations,
        scope = scope,
        produceFile = { dataStoreFile },
    )
}
