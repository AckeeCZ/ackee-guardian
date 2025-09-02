package io.github.ackeecz.guardian.jetpack

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.google.crypto.tink.KeysetHandle
import com.google.crypto.tink.StreamingAead
import io.github.ackeecz.guardian.core.MasterKey
import io.github.ackeecz.guardian.core.internal.AndroidTestWithKeyStore
import io.github.ackeecz.guardian.core.internal.assertAesGcmHkdfEncryptionScheme
import io.github.ackeecz.guardian.core.internal.getKeysetHandle
import io.github.ackeecz.guardian.core.internal.junit.rule.CoroutineRule
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.inspectors.forAll
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
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

    private fun createEncryptedFileBuilder(
        file: File = File(context.filesDir, "encrypted"),
        encryptionScheme: EncryptedFile.FileEncryptionScheme = EncryptedFile.FileEncryptionScheme.AES256_GCM_HKDF_4KB,
        getMasterKey: suspend () -> MasterKey = { MasterKey.getOrCreate() },
    ): EncryptedFile.Builder {
        return EncryptedFile.Builder(file, context, encryptionScheme, getMasterKey)
            .setBackgroundDispatcher(coroutineRule.testDispatcher)
    }

    @Test
    fun `generate data encryption key in keyset stored in shared preferences with default prefs name and keyset alias`() = runTest {
        val underTest = createEncryptedFileBuilder().build()

        underTest.forceEncryptionKeyGeneration()

        assertKeysetExists(prefsName = DEFAULT_KEYSET_PREFS_NAME, keysetAlias = DEFAULT_KEYSET_ALIAS)
    }

    private suspend fun EncryptedFile.forceEncryptionKeyGeneration() {
        // Generates encryption key under the hood because they are generated lazily during first
        // opening of file output / file input
        openFileOutput()
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
        val underTest = createEncryptedFileBuilder()
            .setKeysetPrefName(prefsName)
            .setKeysetAlias(keysetAlias)
            .build()

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
    fun `apply correct file encryption scheme`() = runTest {
        EncryptedFile.FileEncryptionScheme.entries.associateWith { scheme ->
            when (scheme) {
                EncryptedFile.FileEncryptionScheme.AES256_GCM_HKDF_4KB -> 4096 // 4 KB
                EncryptedFile.FileEncryptionScheme.AES256_GCM_HKDF_1MB -> 1_048_576 // 1 MB
            }
        }.forAll { (scheme, expectedCiphertextSegmentBitSize) ->
            val masterKey = MasterKey.getOrCreate()
            val keysetAlias = scheme.name
            val underTest = createEncryptedFileBuilder(
                file = File(context.filesDir, scheme.name),
                encryptionScheme = scheme,
                getMasterKey = { masterKey },
            ).setKeysetAlias(keysetAlias).build()

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
    fun `throw exception on opening file output if the file already exists`() = runTest {
        val file = File(context.filesDir, "encrypted").also(File::createNewFile)
        val underTest = createEncryptedFileBuilder(file = file).build()

        shouldThrow<IOException> { underTest.openFileOutput() }
    }

    @Test
    fun `throw exception on opening file input if the file does not exist`() = runTest {
        val file = File(context.filesDir, "encrypted")
        val underTest = createEncryptedFileBuilder(file = file).build()

        shouldThrow<FileNotFoundException> { underTest.openFileInput() }
    }

    @Test
    fun `throw exception if trying to get channel from encrypted file output`() = runTest {
        val file = File(context.filesDir, "encrypted")
        val underTest = createEncryptedFileBuilder(file = file).build()

        shouldThrow<UnsupportedOperationException> { underTest.openFileOutput().channel }
    }

    @Test
    fun `throw exception if trying to get channel from encrypted file input`() = runTest {
        val file = File(context.filesDir, "encrypted").also(File::createNewFile)
        val underTest = createEncryptedFileBuilder(file = file).build()

        shouldThrow<UnsupportedOperationException> { underTest.openFileInput().channel }
    }

    @Test
    fun `generate same data encryption key for multiple encrypted files concurrently`() = runTest {
        val expectedText = "expected text"
        repeat(50) { keyAliasIndex ->
            fun createEncryptedFiles(): List<EncryptedFile> {
                return (0 until 8).map { fileIndex ->
                    val file = File(context.filesDir, "encrypted_${keyAliasIndex}_$fileIndex")
                    createEncryptedFileBuilder(file = file)
                        .setKeysetAlias("alias_$keyAliasIndex")
                        // UnconfinedTestDispatcher internally allows to not confine execution to any
                        // particular thread, which means that if we call Encrypted.openFileOutput from
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
}
