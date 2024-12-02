package io.github.ackeecz.security.datastore.preferences

import android.content.Context
import androidx.datastore.core.CorruptionException
import androidx.datastore.core.DataMigration
import androidx.datastore.core.DataStore
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStoreFile
import io.github.ackeecz.security.datastore.core.DataStoreCryptoParams
import io.github.ackeecz.security.datastore.core.internal.DataStoreDelegate
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlin.properties.ReadOnlyProperty

// TODO Validate "example usage" in docs before library release when public interface is finalized
/**
 * Creates a property delegate for an encrypted single process preferences [DataStore]. This should
 * only be called once in a file (at the top level), and all usages of the [DataStore] should use a
 * reference to the same instance. The receiver type for the property delegate must be an instance
 * of [Context].
 *
 * Example usage:
 * ```
 * val Context.myDataStore by encryptedPreferencesDataStore(
 *     cryptoParams = DataStoreCryptoParams(
 *         encryptionScheme = DataStoreEncryptionScheme.AES256_GCM_HKDF_4KB,
 *         getMasterKey = { MasterKey.getOrCreate() },
 *     ),
 *     name = "preferences_name",
 * )
 *
 * class SomeClass(val context: Context) {
 *     suspend fun update() = context.myDataStore.edit {...}
 * }
 * ```
 *
 * @param cryptoParams Provides cryptographic parameters like used master key or encryption scheme
 * for the data encryption key used to encrypt/decrypt data.
 * @param name The name of the preferences. The preferences will be stored in a file in the
 * "datastore/" subdirectory in the application context's files directory and is generated using
 * [preferencesDataStoreFile].
 * @param corruptionHandler The corruptionHandler is invoked if DataStore encounters a
 * [CorruptionException] when attempting to read data. CorruptionExceptions
 * are thrown by serializers when data can not be de-serialized.
 * @param produceMigrations produce the migrations. The ApplicationContext is passed in to these
 * callbacks as a parameter. DataMigrations are run before any access to data can occur. Each
 * producer and migration may be run more than once whether or not it already succeeded
 * (potentially because another migration failed or a write to disk failed.)
 * @param scope The scope in which IO operations and transform functions will execute.
 *
 * @return a property delegate that manages an encrypted preferences datastore as a singleton.
 */
public fun encryptedPreferencesDataStore(
    cryptoParams: DataStoreCryptoParams,
    name: String,
    corruptionHandler: ReplaceFileCorruptionHandler<Preferences>? = null,
    produceMigrations: (Context) -> List<DataMigration<Preferences>> = { emptyList() },
    scope: CoroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob()),
): ReadOnlyProperty<Context, DataStore<Preferences>> {
    return EncryptedPreferenceDataStoreDelegate(
        cryptoParams = cryptoParams,
        fileName = name,
        corruptionHandler = corruptionHandler,
        produceMigrations = produceMigrations,
        scope = scope,
    )
}

private class EncryptedPreferenceDataStoreDelegate(
    private val cryptoParams: DataStoreCryptoParams,
    private val fileName: String,
    private val corruptionHandler: ReplaceFileCorruptionHandler<Preferences>?,
    private val produceMigrations: (Context) -> List<DataMigration<Preferences>>,
    private val scope: CoroutineScope,
) : DataStoreDelegate<Preferences>() {

    override fun createDataStore(applicationContext: Context): DataStore<Preferences> {
        return PreferenceDataStoreFactory.createEncrypted(
            context = applicationContext,
            cryptoParams = cryptoParams,
            corruptionHandler = corruptionHandler,
            migrations = produceMigrations(applicationContext),
            scope = scope
        ) {
            applicationContext.preferencesDataStoreFile(fileName)
        }
    }
}
