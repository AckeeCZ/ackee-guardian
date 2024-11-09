package io.github.ackeecz.security.datastore.preferences

import android.content.Context
import androidx.datastore.core.CorruptionException
import androidx.datastore.core.DataMigration
import androidx.datastore.core.DataStore
import androidx.datastore.core.FileStorage
import androidx.datastore.core.Serializer
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.PreferencesSerializer
import androidx.datastore.preferences.preferencesDataStoreFile
import io.github.ackeecz.security.datastore.core.DataStoreCryptoParams
import io.github.ackeecz.security.datastore.core.internal.EncryptingSerializer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import okio.buffer
import okio.sink
import okio.source
import java.io.File
import java.io.InputStream
import java.io.OutputStream

private const val REQUIRED_PREFERENCES_FILE_EXTENSION = "preferences_pb"

/**
 * Creates an instance of encrypted preferences [DataStore]. Never create more than one instance of
 * DataStore for a given file; doing so can break all DataStore functionality. You should
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
 * should act on the same file at the same time. The file must have the extension
 * preferences_pb and you can use [preferencesDataStoreFile] function to create it. File will be
 * created if it doesn't exist.
 *
 * @return a new encrypted preferences DataStore instance with the provided configuration
 */
@Suppress("LongParameterList")
public fun PreferenceDataStoreFactory.createEncrypted(
    context: Context,
    cryptoParams: DataStoreCryptoParams,
    corruptionHandler: ReplaceFileCorruptionHandler<Preferences>? = null,
    migrations: List<DataMigration<Preferences>> = emptyList(),
    scope: CoroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob()),
    produceFile: () -> File,
): DataStore<Preferences> {
    val dataStoreFile = produceFileWithCheck(produceFile)
    val encryptingSerializer = EncryptingSerializer(
        context = context,
        cryptoParams = cryptoParams,
        delegate = OkioPreferencesSerializerAdapter,
        scope = scope,
        dataStoreFile = dataStoreFile,
    )
    return create(
        storage = FileStorage(encryptingSerializer) { dataStoreFile },
        corruptionHandler = corruptionHandler,
        migrations = migrations,
        scope = scope,
    )
}

private fun produceFileWithCheck(produceFile: () -> File): File = produceFile().also { file ->
    check(file.extension == REQUIRED_PREFERENCES_FILE_EXTENSION) {
        "File extension for file: $file does not match required extension for" +
            " Preferences file: $REQUIRED_PREFERENCES_FILE_EXTENSION"
    }
}

private object OkioPreferencesSerializerAdapter : Serializer<Preferences> {

    private val adaptee = PreferencesSerializer

    override val defaultValue: Preferences = adaptee.defaultValue

    override suspend fun writeTo(t: Preferences, output: OutputStream) {
        output.sink().buffer().use { adaptee.writeTo(t, it) }
    }

    override suspend fun readFrom(input: InputStream): Preferences {
        return input.source().buffer().use { adaptee.readFrom(it) }
    }
}
