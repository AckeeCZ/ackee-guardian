package io.github.ackeecz.security.sample

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.preferencesOf
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStoreFile
import androidx.test.core.app.ApplicationProvider
import io.github.ackeecz.security.core.MasterKey
import io.github.ackeecz.security.datastore.core.DataStoreCryptoParams
import io.github.ackeecz.security.datastore.core.DataStoreEncryptionScheme
import io.github.ackeecz.security.datastore.preferences.createEncrypted
import io.github.ackeecz.security.datastore.preferences.encryptedPreferencesDataStore
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Test
import kotlin.random.Random

internal class DataStorePreferencesArtifactTest : AndroidTestWithKeyStore() {

    private val context: Context = ApplicationProvider.getApplicationContext()

    private val Context.encryptedPreferencesDataStore by encryptedPreferencesDataStore(
        cryptoParams = DataStoreCryptoParams(
            encryptionScheme = DataStoreEncryptionScheme.AES256_GCM_HKDF_4KB,
            getMasterKey = { MasterKey.getOrCreate() },
        ),
        name = "test_file",
    )

    @Test
    fun `create encrypted preferences DataStore using factory`() = runTest {
        val underTest = PreferenceDataStoreFactory.createEncrypted(
            context = context,
            cryptoParams = DataStoreCryptoParams(
                encryptionScheme = DataStoreEncryptionScheme.AES256_GCM_HKDF_4KB,
                getMasterKey = { MasterKey.getOrCreate() },
            ),
            produceFile = { context.preferencesDataStoreFile("test_file") },
        )
        testDataStore(underTest)
    }

    private suspend fun testDataStore(underTest: DataStore<Preferences>) {
        val expectedData = preferencesOf(
            intPreferencesKey("first") to Random.nextInt(),
            stringPreferencesKey("second") to "expected",
        )

        underTest.updateData { expectedData }

        underTest.data.first() shouldBe expectedData
    }

    @Test
    fun `create encrypted preferences DataStore using delegate`() = runTest {
        testDataStore(context.encryptedPreferencesDataStore)
    }
}
