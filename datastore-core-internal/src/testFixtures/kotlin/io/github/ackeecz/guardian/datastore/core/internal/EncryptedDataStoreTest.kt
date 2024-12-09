package io.github.ackeecz.guardian.datastore.core.internal

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.test.core.app.ApplicationProvider
import com.google.crypto.tink.KeysetHandle
import com.google.crypto.tink.StreamingAead
import io.github.ackeecz.guardian.core.MasterKey
import io.github.ackeecz.guardian.core.internal.AndroidTestWithKeyStore
import io.github.ackeecz.guardian.core.internal.assertAesGcmHkdfEncryptionScheme
import io.github.ackeecz.guardian.core.internal.getKeysetHandle
import io.github.ackeecz.guardian.core.internal.junit.rule.CoroutineRule
import io.github.ackeecz.guardian.datastore.core.DataStoreCryptoParams
import io.github.ackeecz.guardian.datastore.core.DataStoreEncryptionScheme
import io.kotest.inspectors.forAll
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeSameInstanceAs
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Rule
import org.junit.Test
import java.io.File
import java.io.InputStream
import kotlin.random.Random

private const val DEFAULT_KEYSET_PREFS_NAME = "__io_github_ackeecz_guardian_pref__"
private const val DEFAULT_KEYSET_ALIAS = "__io_github_ackeecz_guardian_datastore_keyset__"

abstract class EncryptedDataStoreTest<DataStoreData : Any> : AndroidTestWithKeyStore() {

    @get:Rule
    val coroutineRule: CoroutineRule = CoroutineRule()

    protected val context: Context = ApplicationProvider.getApplicationContext()

    @After
    fun tearDown() {
        deleteDefaultDataStoreFolder()
    }

    private fun deleteDefaultDataStoreFolder() {
        File(context.filesDir, "datastore").deleteRecursively()
    }

    protected abstract fun createSut(
        cryptoParams: DataStoreCryptoParams = createDefaultCryptoParams(),
        fileName: String = "test_file",
        coroutineScope: CoroutineScope = createDataStoreCoroutineScope(),
    ): DataStore<DataStoreData>

    /**
     * Returns [File] with [fileName] located in the default folder for DataStore files
     */
    protected abstract fun getDefaultDataStoreFile(fileName: String): File

    /**
     * Factory function of [DataStoreCryptoParams], which creates it with default values of [DataStoreCryptoParams],
     * if possible. Other non-default values are provided by the function itself and can be overridden
     * by the caller if needed.
     */
    protected fun createDefaultCryptoParams(
        encryptionScheme: DataStoreEncryptionScheme = DataStoreEncryptionScheme.AES256_GCM_HKDF_4KB,
        getMasterKey: suspend () -> MasterKey = { MasterKey.getOrCreate() },
    ): DataStoreCryptoParams {
        return DataStoreCryptoParams(
            encryptionScheme = encryptionScheme,
            getMasterKey = getMasterKey,
        )
    }

    protected fun createDataStoreCoroutineScope(): CoroutineScope {
        return CoroutineScope(coroutineRule.testDispatcher + SupervisorJob())
    }

    /**
     * Must update [currentData] and return it as a result. It is important to make any kind of
     * modification to the [currentData], because some tests can rely on the value to be changed.
     */
    protected abstract suspend fun updateData(currentData: DataStoreData): DataStoreData

    /**
     * Creates expected data that are used in various tests for assertions
     */
    protected abstract fun createExpectedData(): DataStoreData

    /**
     * Decodes [DataStoreData] from this [InputStream] that contains encoded data that are the
     * content of the [DataStore] file.
     */
    protected abstract suspend fun InputStream.decodeTestData(): DataStoreData

    @Test
    fun `generate data encryption key in keyset stored in shared preferences with default prefs name and keyset alias`() = runTest {
        val underTest = createSut(cryptoParams = createDefaultCryptoParams())

        underTest.forceEncryptionKeyGeneration()

        assertKeysetExists(prefsName = DEFAULT_KEYSET_PREFS_NAME, keysetAlias = DEFAULT_KEYSET_ALIAS)
    }

    private suspend fun DataStore<DataStoreData>.forceEncryptionKeyGeneration() {
        // Generates encryption key under the hood because they are generated lazily during first
        // read/write
        updateData { updateData(it) }
    }

    private fun assertKeysetExists(prefsName: String, keysetAlias: String) {
        context.getSharedPreferences(prefsName, Context.MODE_PRIVATE)
            .getString(keysetAlias, null)
            .shouldNotBeNull()
    }

    @Test
    fun `generate data encryption key in keyset stored in shared preferences with custom prefs name and keyset alias`() = runTest {
        val prefsName = "custom_prefs"
        val keysetAlias = "custom_alias"
        val cryptoParams = createDefaultCryptoParams().copy(
            keysetPrefsName = prefsName,
            keysetAlias = keysetAlias,
        )
        val underTest = createSut(cryptoParams = cryptoParams)

        underTest.forceEncryptionKeyGeneration()

        assertKeysetExists(prefsName = prefsName, keysetAlias = keysetAlias)
    }

    /**
     * Tests encryption, but also:
     * - encryption of data encryption key with master key
     * - correct generation of data encryption key
     * - correct used associated data
     */
    @Test
    fun `encrypt DataStore with data encryption key`() = runTest {
        val expectedData = createExpectedData()
        val masterKey = MasterKey.getOrCreate()
        val cryptoParams = createDefaultCryptoParams(getMasterKey = { masterKey })
        // Randomness more reliably verifies that file name is correctly used as associated data
        val fileName = "test_file-${Random.nextInt()}"
        val underTest = createSut(cryptoParams = cryptoParams, fileName = fileName)

        underTest.updateData { expectedData }

        val dataStoreFile = getDefaultDataStoreFile(fileName)
        val associatedData = dataStoreFile.name.toByteArray(Charsets.UTF_8)
        getDefaultKeysetHandle(masterKey)
            .getPrimitive(StreamingAead::class.java)
            .newDecryptingStream(dataStoreFile.inputStream(), associatedData)
            .decodeTestData()
            .shouldBe(expectedData)
    }

    private fun getDefaultKeysetHandle(masterKey: MasterKey): KeysetHandle {
        return getKeysetHandle(
            context = context,
            masterKeyUri = masterKey.keyStoreUri,
            keysetPrefsName = DEFAULT_KEYSET_PREFS_NAME,
            keysetAlias = DEFAULT_KEYSET_ALIAS,
        )
    }

    @Test
    fun `decrypt DataStore with data encryption key`() = runTest {
        val expectedData = createExpectedData()
        val cryptoParams = createDefaultCryptoParams()
        val firstDataStoreScope = createDataStoreCoroutineScope()
        createSut(
            cryptoParams = cryptoParams,
            coroutineScope = firstDataStoreScope,
        ).updateData { expectedData }
        // We need to use different DataStore instances to write and read to properly test decryption,
        // because with one instance if we save and get data, it gets the saved data from the
        // in-memory cache unencrypted and the test passes falsely. Because of this, we need to use
        // one instance to save data, cancel it by cancelling its CoroutineScope and then create a new
        // instance. Without cancelling the first instance (scope), creation of second instance fails
        // because there can be only one DataStore instance active per file.
        firstDataStoreScope.cancel()

        val actualData = createSut(cryptoParams = cryptoParams).data.first()

        actualData shouldBe expectedData
    }

    @Test
    fun `apply correct DataStore encryption scheme`() = runTest {
        DataStoreEncryptionScheme.entries.associateWith { scheme ->
            when (scheme) {
                DataStoreEncryptionScheme.AES256_GCM_HKDF_4KB -> 4096 // 4 KB
                DataStoreEncryptionScheme.AES256_GCM_HKDF_1MB -> 1_048_576 // 1 MB
            }
        }.forAll { (scheme, expectedCiphertextSegmentBitSize) ->
            val masterKey = MasterKey.getOrCreate()
            val keysetAlias = scheme.name
            val cryptoParams = createDefaultCryptoParams(
                encryptionScheme = scheme,
                getMasterKey = { masterKey },
            ).copy(keysetAlias = keysetAlias)
            val underTest = createSut(
                cryptoParams = cryptoParams,
                fileName = scheme.name,
            )

            underTest.forceEncryptionKeyGeneration()

            getKeysetHandle(
                context = context,
                masterKeyUri = masterKey.keyStoreUri,
                keysetPrefsName = DEFAULT_KEYSET_PREFS_NAME,
                keysetAlias = keysetAlias,
            ).assertAesGcmHkdfEncryptionScheme(
                expectedKeyBitSize = 256,
                expectedCiphertextSegmentBitSize = expectedCiphertextSegmentBitSize,
            )
        }
    }

    @Test
    fun `generate same data encryption key for multiple encrypted DataStores concurrently`() = runTest {
        val expectedData = createExpectedData()
        repeat(times = 50) { keyAliasIndex ->
            fun createDataStoresWithScopes(): Map<DataStore<DataStoreData>, CoroutineScope> {
                return (0 until 8).map { fileIndex ->
                    val cryptoParams = createDefaultCryptoParams().copy(keysetAlias = "alias_$keyAliasIndex")
                    val fileName = "encrypted_${keyAliasIndex}_$fileIndex"
                    // UnconfinedTestDispatcher internally allows to not confine execution to any
                    // particular thread, which means that if we call DataStore.updateData from
                    // Dispatchers.Default pool of threads, it actually uses threads from this pool
                    // and preserve this context, which is important for our case.
                    val coroutineScope = CoroutineScope(UnconfinedTestDispatcher() + SupervisorJob())
                    createSut(
                        cryptoParams = cryptoParams,
                        fileName = fileName,
                        coroutineScope = coroutineScope,
                    ) to coroutineScope
                }.toMap()
            }

            createDataStoresWithScopes()
                .map { dataStoreWithScope ->
                    // Concurrently write to DataStores, which generates data encryption key as well
                    async(Dispatchers.Default) {
                        dataStoreWithScope.also {
                            it.key.updateData { expectedData }
                        }
                    }
                }
                .awaitAll()
                // Deactivate all DataStore instances by cancelling their scopes
                .forEach { it.value.cancel() }

            // Create new DataStores to load stored encryption key and decrypt data to validate if all
            // DataStores were encrypted with the same single key.
            createDataStoresWithScopes().keys.forAll { dataStore ->
                dataStore.data.first() shouldBe expectedData
            }
        }
    }

    protected suspend fun <T : Any> TestScope.testDataStoreDelegateSingleton(
        createDelegateWrapper: (keyAliasIndex: Int) -> DataStoreDelegateWrapper<T>,
    ) {
        repeat(times = 50) { keyAliasIndex ->
            val dataStoreDelegateWrapper = createDelegateWrapper(keyAliasIndex)

            val dataStores = (0 until 8)
                .map {
                    async(Dispatchers.Default) {
                        dataStoreDelegateWrapper.getDataStore()
                    }
                }.awaitAll()

            // DataStore throws exception if there is more instances for a single file, so the above
            // code should fail in that case, but still better check the instances here, if it didn't
            // throw exception for some reason.
            dataStores.reduce { previous, current ->
                previous shouldBeSameInstanceAs current
                current
            }
        }
    }
}
