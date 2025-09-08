package io.github.ackeecz.guardian.jetpack

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.google.crypto.tink.KeysetHandle
import com.google.crypto.tink.StreamingAead
import io.github.ackeecz.guardian.core.MasterKey
import io.github.ackeecz.guardian.core.internal.AndroidTestWithKeyStore
import io.github.ackeecz.guardian.core.internal.InvalidKeysetConfigException
import io.github.ackeecz.guardian.core.internal.TinkPrimitiveProvider
import io.github.ackeecz.guardian.core.internal.assertAesGcmHkdfEncryptionScheme
import io.github.ackeecz.guardian.core.internal.clearFixture
import io.github.ackeecz.guardian.core.internal.getKeysetGetCallCount
import io.github.ackeecz.guardian.core.internal.getKeysetHandle
import io.github.ackeecz.guardian.core.internal.junit.rule.CoroutineRule
import io.kotest.assertions.throwables.shouldNotThrow
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.inspectors.forAll
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Rule
import org.junit.Test
import java.io.File
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import kotlin.random.Random

private const val DEFAULT_KEYSET_PREFS_NAME = "__androidx_security_crypto_encrypted_file_pref__"
private const val DEFAULT_KEYSET_ALIAS = "__androidx_security_crypto_encrypted_file_keyset__"

internal class EncryptedFileTest : AndroidTestWithKeyStore() {

    @get:Rule
    val coroutineRule: CoroutineRule = CoroutineRule()

    private val context: Context get() = ApplicationProvider.getApplicationContext()

    @After
    fun tearDown() {
        TinkPrimitiveProvider.clearFixture()
    }

    @Suppress("DEPRECATION") // Testing deprecated API
    private fun createEncryptedFileBuilderOldApi(
        fileName: String = "encrypted",
        encryptionScheme: EncryptedFile.FileEncryptionScheme = EncryptedFile.FileEncryptionScheme.AES256_GCM_HKDF_4KB,
        getMasterKey: suspend () -> MasterKey = { MasterKey.getOrCreate() },
    ): EncryptedFile.Builder {
        return EncryptedFile.Builder(
            file = getFile(fileName = fileName),
            context = context,
            encryptionScheme = encryptionScheme,
            getMasterKey = getMasterKey,
        ).setBackgroundDispatcher(coroutineRule.testDispatcher)
    }

    private fun createEncryptedFileBuilder(
        file: File,
        fileKeysetConfig: FileKeysetConfig = createDefaultFileKeysetConfig(),
        getMasterKey: suspend () -> MasterKey = { MasterKey.getOrCreate() },
    ): EncryptedFile.Builder {
        return EncryptedFile.Builder(file, context, fileKeysetConfig, getMasterKey)
            .setBackgroundDispatcher(coroutineRule.testDispatcher)
    }

    private fun createEncryptedFileBuilder(
        fileName: String = "encrypted",
        fileKeysetConfig: FileKeysetConfig = createDefaultFileKeysetConfig(),
        getMasterKey: suspend () -> MasterKey = { MasterKey.getOrCreate() },
    ): EncryptedFile.Builder {
        return createEncryptedFileBuilder(
            file = getFile(fileName = fileName),
            fileKeysetConfig = fileKeysetConfig,
            getMasterKey = getMasterKey,
        )
    }

    private fun getFile(fileName: String) = File(context.filesDir, fileName)

    @Test
    fun `generate data encryption key in keyset stored in shared preferences with default prefs name and keyset alias using old API`() = runTest {
        val underTest = createEncryptedFileBuilderOldApi().build()

        underTest.forceEncryptionKeyGeneration()

        assertKeysetExists(prefsName = DEFAULT_KEYSET_PREFS_NAME, keysetAlias = DEFAULT_KEYSET_ALIAS)
    }

    @Test
    fun `generate data encryption key in keyset stored in shared preferences with default prefs name and keyset alias using new API`() = runTest {
        val underTest = createEncryptedFileBuilder(fileKeysetConfig = createDefaultFileKeysetConfig()).build()

        underTest.forceEncryptionKeyGeneration()

        assertKeysetExists(prefsName = DEFAULT_KEYSET_PREFS_NAME, keysetAlias = DEFAULT_KEYSET_ALIAS)
    }

    private suspend fun EncryptedFile.forceEncryptionKeyGeneration() {
        // Generates encryption key under the hood because they are generated lazily during first
        // opening of file output / file input
        openFileOutput().close()
    }

    private fun assertKeysetExists(prefsName: String, keysetAlias: String) {
        context.getSharedPreferences(prefsName, Context.MODE_PRIVATE)
            .getString(keysetAlias, null)
            .shouldNotBeNull()
    }

    @Test
    @Suppress("DEPRECATION") // Testing deprecated API
    fun `generate data encryption key in keyset stored in shared preferences with custom prefs name and keyset alias set using old API`() = runTest {
        val prefsName = "custom_prefs"
        val keysetAlias = "custom_alias"
        val underTest = createEncryptedFileBuilderOldApi()
            .setKeysetPrefName(prefsName)
            .setKeysetAlias(keysetAlias)
            .build()

        underTest.forceEncryptionKeyGeneration()

        assertKeysetExists(prefsName = prefsName, keysetAlias = keysetAlias)
    }

    @Test
    fun `generate data encryption key in keyset stored in shared preferences with custom prefs name and keyset alias set using new API`() = runTest {
        val prefsName = "custom_prefs"
        val keysetAlias = "custom_alias"
        val config = createFileKeysetConfig(prefsName = prefsName, alias = keysetAlias)
        val underTest = createEncryptedFileBuilder(fileKeysetConfig = config).build()

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
    fun `encrypt file with data encryption key`() = runTest {
        // Randomness more reliably verifies that file name is correctly used as associated data
        val file = File(context.filesDir, "encrypted-${Random.nextInt()}")
        val expectedPlainText = "expected plain text"
        val masterKey = MasterKey.getOrCreate()
        val underTest = createEncryptedFileBuilder(file = file, getMasterKey = { masterKey }).build()

        underTest.openFileOutput().write(expectedPlainText)

        val actualPlainText = getDefaultKeysetHandle(masterKey)
            .getPrimitive(StreamingAead::class.java)
            .newDecryptingStream(file.inputStream(), file.name.toByteArray(Charsets.UTF_8))
            .readText()
        actualPlainText shouldBe expectedPlainText
    }

    private fun FileOutputStream.write(text: String) {
        bufferedWriter().use { it.write(text) }
    }

    private fun InputStream.readText(): String = bufferedReader().readText()

    private fun getDefaultKeysetHandle(masterKey: MasterKey): KeysetHandle {
        return getKeysetHandle(
            context = context,
            masterKeyUri = masterKey.keyStoreUri,
            keysetPrefsName = DEFAULT_KEYSET_PREFS_NAME,
            keysetAlias = DEFAULT_KEYSET_ALIAS,
        )
    }

    @Test
    fun `decrypt file with data encryption key`() = runTest {
        val expectedPlainText = "expected plain text"
        val underTest = createEncryptedFileBuilder().build()
        underTest.openFileOutput().write(expectedPlainText)

        val actualPlainText = underTest.openFileInput().readText()

        actualPlainText shouldBe expectedPlainText
    }

    @Test
    @Suppress("DEPRECATION") // Testing deprecated API
    fun `apply correct file encryption scheme using old API`() = runTest {
        testEncryptionScheme { masterKey, scheme, prefsName, keysetAlias ->
            createEncryptedFileBuilderOldApi(
                fileName = scheme.name,
                encryptionScheme = scheme,
                getMasterKey = { masterKey },
            )
                .setKeysetPrefName(prefsName)
                .setKeysetAlias(keysetAlias)
        }
    }

    private suspend fun testEncryptionScheme(
        getUnderTest: (MasterKey, EncryptedFile.FileEncryptionScheme, String, String) -> EncryptedFile.Builder,
    ) {
        EncryptedFile.FileEncryptionScheme.entries.associateWith { scheme ->
            when (scheme) {
                EncryptedFile.FileEncryptionScheme.AES256_GCM_HKDF_4KB -> 4096 // 4 KB
                EncryptedFile.FileEncryptionScheme.AES256_GCM_HKDF_1MB -> 1_048_576 // 1 MB
            }
        }.forAll { (scheme, expectedCiphertextSegmentBitSize) ->
            val masterKey = MasterKey.getOrCreate()
            val prefsName = DEFAULT_KEYSET_PREFS_NAME
            val keysetAlias = scheme.name
            val underTest = getUnderTest(masterKey, scheme, prefsName, keysetAlias).build()

            underTest.forceEncryptionKeyGeneration()

            getKeysetHandle(
                context = context,
                masterKeyUri = masterKey.keyStoreUri,
                keysetPrefsName = prefsName,
                keysetAlias = keysetAlias,
            ).assertAesGcmHkdfEncryptionScheme(
                expectedKeyBitSize = 256,
                expectedCiphertextSegmentBitSize = expectedCiphertextSegmentBitSize,
            )
        }
    }

    @Test
    fun `apply correct file encryption scheme using new API`() = runTest {
        testEncryptionScheme { masterKey, scheme, prefsName, keysetAlias ->
            createEncryptedFileBuilder(
                fileName = scheme.name,
                fileKeysetConfig = createFileKeysetConfig(
                    encryptionScheme = scheme,
                    prefsName = prefsName,
                    alias = keysetAlias,
                ),
                getMasterKey = { masterKey },
            )
        }
    }

    @Test
    fun `throw exception on opening file output if the file already exists`() = runTest {
        val file = File(context.filesDir, "encrypted").also(File::createNewFile)
        val underTest = createEncryptedFileBuilder(file = file).build()

        shouldThrow<IOException> { underTest.openFileOutput() }
    }

    @Test
    fun `throw exception on opening file input if the file does not exist`() = runTest {
        val underTest = createEncryptedFileBuilder().build()

        shouldThrow<FileNotFoundException> { underTest.openFileInput() }
    }

    @Test
    fun `throw exception if trying to get channel from encrypted file output`() = runTest {
        val underTest = createEncryptedFileBuilder().build()

        shouldThrow<UnsupportedOperationException> { underTest.openFileOutput().channel }
    }

    @Test
    fun `throw exception if trying to get channel from encrypted file input`() = runTest {
        val file = File(context.filesDir, "encrypted").also(File::createNewFile)
        val underTest = createEncryptedFileBuilder(file = file).build()

        shouldThrow<UnsupportedOperationException> { underTest.openFileInput().channel }
    }

    @Test
    @Suppress("DEPRECATION") // Testing deprecated API
    fun `generate same data encryption key for multiple encrypted files concurrently`() = runTest {
        val expectedText = "expected text"
        repeat(50) { keyAliasIndex ->
            fun createEncryptedFiles(): List<EncryptedFile> {
                return (0 until 8).map { fileIndex ->
                    createEncryptedFileBuilder(fileName = "encrypted_${keyAliasIndex}_$fileIndex")
                        .setKeysetAlias("alias_$keyAliasIndex")
                        // UnconfinedTestDispatcher internally allows to not confine execution to any
                        // particular thread, which means that if we call EncryptedFile.openFileOutput from
                        // Dispatchers.Default pool of threads, it actually uses threads from this pool
                        // and preserve this context, which is important for our case.
                        .setBackgroundDispatcher(UnconfinedTestDispatcher())
                        .build()
                }
            }

            createEncryptedFiles().map { encryptedFile ->
                // Concurrently write to files, which generates data encryption key as well
                async(Dispatchers.Default) {
                    encryptedFile.openFileOutput().write(expectedText)
                }
            }.awaitAll()

            // Create new files to load stored encryption key and decrypt data to validate if all
            // files were encrypted with the same single key.
            createEncryptedFiles().forAll { encryptedFile ->
                encryptedFile.openFileInput().readText() shouldBe expectedText
            }
        }
    }

    @Test
    fun `do not cache data encryption key by default using old API`() = runTest {
        testDefaultCaching { fileName -> createEncryptedFileBuilderOldApi(fileName = fileName) }
    }

    private suspend fun testDefaultCaching(getUnderTest: (String) -> EncryptedFile.Builder) {
        suspend fun getKey(fileName: String) {
            getUnderTest(fileName).build().forceEncryptionKeyGeneration()
        }

        getKey(fileName = "first")
        getKey(fileName = "second")

        val keysetGetCallCount = getKeysetGetCallCount(prefsName = DEFAULT_KEYSET_PREFS_NAME, keysetAlias = DEFAULT_KEYSET_ALIAS)
        keysetGetCallCount shouldBe 2
    }

    private fun getKeysetGetCallCount(prefsName: String, keysetAlias: String): Int {
        return context.getKeysetGetCallCount(prefsName = prefsName, keysetAlias = keysetAlias)
    }

    @Test
    fun `do not cache data encryption key by default using new API`() = runTest {
        testDefaultCaching { fileName -> createEncryptedFileBuilder(fileName = fileName) }
    }

    @Test
    fun `do not cache data encryption key if disabled`() = runTest {
        testKeyCaching(cacheKeyset = false)
    }

    private fun testKeyCaching(cacheKeyset: Boolean) = runTest {
        val config = createFileKeysetConfig(cacheKeyset = cacheKeyset)
        suspend fun getKey(fileName: String) {
            createEncryptedFileBuilder(fileName = fileName, fileKeysetConfig = config)
                .build()
                .forceEncryptionKeyGeneration()
        }

        getKey(fileName = "first")
        getKey(fileName = "second")
        getKey(fileName = "third")

        val keysetGetCallCount = getKeysetGetCallCount(prefsName = config.prefsName, keysetAlias = config.alias)
        keysetGetCallCount shouldBe if (cacheKeyset) 1 else 3
    }

    @Test
    fun `cache data encryption key if enabled`() = runTest {
        testKeyCaching(cacheKeyset = true)
    }

    @Test
    fun `fail if caching not enabled in first config but enabled in second for the same key`() = runTest {
        testInvalidCacheConfig(firstCacheKeyset = false, secondCacheKeyset = true)
    }

    private suspend fun testInvalidCacheConfig(firstCacheKeyset: Boolean, secondCacheKeyset: Boolean) {
        suspend fun getKey(fileName: String, cacheKeyset: Boolean) {
            val config = createFileKeysetConfig(cacheKeyset = cacheKeyset)
            createEncryptedFileBuilder(fileName = fileName, fileKeysetConfig = config)
                .build()
                .forceEncryptionKeyGeneration()
        }

        getKey("first", firstCacheKeyset)
        shouldThrow<InvalidKeysetConfigException> { getKey("second", secondCacheKeyset) }
    }

    @Test
    fun `fail if caching enabled in first config but not enabled in second for the same key`() = runTest {
        testInvalidCacheConfig(firstCacheKeyset = true, secondCacheKeyset = false)
    }

    @Test
    fun `allow different caching values for different keys`() = runTest {
        suspend fun getKey(fileName: String, keysetAlias: String, cacheKeyset: Boolean) {
            val config = createFileKeysetConfig(alias = keysetAlias, cacheKeyset = cacheKeyset)
            createEncryptedFileBuilder(fileName = fileName, fileKeysetConfig = config)
                .build()
                .forceEncryptionKeyGeneration()
        }

        getKey("file1", "key1", false)
        shouldNotThrow<InvalidKeysetConfigException> {
            getKey("file2", "key2", true)
        }
    }
}
