package io.github.ackeecz.security.datastore.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.PreferencesSerializer
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.mutablePreferencesOf
import androidx.datastore.preferences.preferencesDataStoreFile
import io.github.ackeecz.security.datastore.core.DataStoreCryptoParams
import io.github.ackeecz.security.datastore.core.internal.DataStoreDelegateWrapper
import io.github.ackeecz.security.datastore.core.internal.EncryptedDataStoreTest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.test.runTest
import okio.buffer
import okio.source
import org.junit.Test
import java.io.File
import java.io.InputStream
import kotlin.random.Random

/**
 * Helper base class for preference data store tests that contains common tests setup
 */
internal abstract class EncryptedPreferenceDataStoreTest : EncryptedDataStoreTest<Preferences>() {

    override fun getDefaultDataStoreFile(fileName: String): File {
        return context.preferencesDataStoreFile(fileName)
    }

    override suspend fun updateData(currentData: Preferences): Preferences {
        return currentData.toMutablePreferences().apply {
            // We generate a new random long key and value to set to be on 99.99% sure, that we
            // actually update the current data with some new updated value.
            set(
                key = longPreferencesKey(Random.nextLong().toString()),
                value = Random.nextLong(),
            )
        }
    }

    override fun createExpectedData(): Preferences {
        return mutablePreferencesOf(longPreferencesKey("key") to Random.nextLong())
    }

    override suspend fun InputStream.decodeTestData(): Preferences {
        return PreferencesSerializer.readFrom(source().buffer())
    }
}

internal class EncryptedPreferenceDataStoreFactoryTest : EncryptedPreferenceDataStoreTest() {

    override fun createSut(
        cryptoParams: DataStoreCryptoParams,
        fileName: String,
        coroutineScope: CoroutineScope,
    ): DataStore<Preferences> {
        return PreferenceDataStoreFactory.createEncrypted(
            context = context,
            cryptoParams = cryptoParams,
            scope = coroutineScope,
            produceFile = { getDefaultDataStoreFile(fileName) },
        )
    }
}

internal class EncryptedPreferenceDataStoreDelegateTest : EncryptedPreferenceDataStoreTest() {

    override fun createSut(
        cryptoParams: DataStoreCryptoParams,
        fileName: String,
        coroutineScope: CoroutineScope,
    ): DataStore<Preferences> {
        return PreferenceDataStoreDelegateWrapper(
            context = context,
            cryptoParams = cryptoParams,
            fileName = fileName,
            coroutineScope = coroutineScope,
        ).getDataStore()
    }

    @Test
    fun `delegate returns singleton`() = runTest {
        testDataStoreDelegateSingleton { keyAliasIndex ->
            PreferenceDataStoreDelegateWrapper(
                context = context,
                cryptoParams = createDefaultCryptoParams(),
                fileName = "test_file_$keyAliasIndex",
                coroutineScope = createDataStoreCoroutineScope(),
            )
        }
    }
}

private class PreferenceDataStoreDelegateWrapper(
    private val context: Context,
    cryptoParams: DataStoreCryptoParams,
    fileName: String,
    coroutineScope: CoroutineScope,
) : DataStoreDelegateWrapper<Preferences> {

    val Context.dataStore by encryptedPreferencesDataStore(
        cryptoParams = cryptoParams,
        name = fileName,
        scope = coroutineScope,
    )

    override fun getDataStore() = context.dataStore
}
