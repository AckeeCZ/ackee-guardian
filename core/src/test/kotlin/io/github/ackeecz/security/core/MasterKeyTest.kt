package io.github.ackeecz.security.core

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import io.github.ackeecz.security.core.internal.AndroidTestWithKeyStore
import io.github.ackeecz.security.core.internal.junit.rule.CoroutineRule
import io.kotest.inspectors.forAll
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.security.KeyStore
import javax.crypto.SecretKey
import kotlin.reflect.KClass

private const val DEFAULT_MASTER_KEY_ALIAS = "_androidx_security_master_key_"
private const val MASTER_KEY_SIZE = 256

internal class MasterKeyTest : AndroidTestWithKeyStore() {

    @get:Rule
    val coroutineRule: CoroutineRule = CoroutineRule()

    private lateinit var masterKeyProvider: MasterKey.Provider

    @Before
    fun setUp() {
        masterKeyProvider = MasterKey.Provider(defaultDispatcher = coroutineRule.testDispatcher)
    }

    @Test
    fun `create safe default KenGenParameterSpecBuilder for master key with custom alias`() {
        val expectedKeyAlias = "key alias"

        val builder = MasterKey.createSafeDefaultSpecBuilder(expectedKeyAlias)

        builder.assertSafeDefaultSpecBuilder(expectedKeyAlias = expectedKeyAlias)
    }

    private fun KeyGenParameterSpec.Builder.assertSafeDefaultSpecBuilder(expectedKeyAlias: String) {
        with(build()) {
            keystoreAlias shouldBe expectedKeyAlias
            purposes shouldBe (KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
            blockModes.shouldHaveSize(1).first() shouldBe KeyProperties.BLOCK_MODE_GCM
            encryptionPaddings.shouldHaveSize(1).first() shouldBe KeyProperties.ENCRYPTION_PADDING_NONE
            keySize shouldBe MASTER_KEY_SIZE
        }
    }

    @Test
    fun `create safe default KenGenParameterSpecBuilder for master key with default alias`() {
        val builder = MasterKey.createSafeDefaultSpecBuilder()

        builder.assertSafeDefaultSpecBuilder(expectedKeyAlias = DEFAULT_MASTER_KEY_ALIAS)
    }

    @Test
    fun `validate key size`() = runTest {
        listOf(
            128 to IllegalArgumentException::class,
            192 to IllegalArgumentException::class,
            256 to null,
        ).forAll { (keySize, expectedExceptionClass) ->
            val spec = MasterKey.createSafeDefaultSpecBuilder().setKeySize(keySize).build()

            validateAndAssert(spec, expectedExceptionClass)
        }
    }

    private suspend fun validateAndAssert(
        spec: KeyGenParameterSpec,
        expectedExceptionClass: KClass<IllegalArgumentException>?,
    ) {
        val result = runCatching { getOrCreateMasterKey(spec) }

        result.exceptionOrNull()?.javaClass?.kotlin shouldBe expectedExceptionClass
    }

    private suspend fun getOrCreateMasterKey(
        spec: KeyGenParameterSpec = MasterKey.createSafeDefaultSpecBuilder().build(),
    ): MasterKey {
        return masterKeyProvider.getOrCreate(spec)
    }

    @Test
    fun `validate block modes`() = runTest {
        listOf(
            arrayOf(KeyProperties.BLOCK_MODE_CBC) to IllegalArgumentException::class,
            arrayOf(KeyProperties.BLOCK_MODE_CTR) to IllegalArgumentException::class,
            arrayOf(KeyProperties.BLOCK_MODE_ECB) to IllegalArgumentException::class,
            arrayOf(KeyProperties.BLOCK_MODE_GCM, KeyProperties.BLOCK_MODE_CBC) to IllegalArgumentException::class,
            arrayOf(KeyProperties.BLOCK_MODE_GCM) to null,
        ).forAll { (blockModes, expectedExceptionClass) ->
            val spec = MasterKey.createSafeDefaultSpecBuilder().setBlockModes(*blockModes).build()

            validateAndAssert(spec, expectedExceptionClass)
        }
    }

    @Test
    fun `validate key purposes`() = runTest {
        listOf(
            KeyProperties.PURPOSE_AGREE_KEY to IllegalArgumentException::class,
            KeyProperties.PURPOSE_WRAP_KEY to IllegalArgumentException::class,
            KeyProperties.PURPOSE_ATTEST_KEY to IllegalArgumentException::class,
            KeyProperties.PURPOSE_VERIFY to IllegalArgumentException::class,
            KeyProperties.PURPOSE_SIGN to IllegalArgumentException::class,
            KeyProperties.PURPOSE_ENCRYPT to IllegalArgumentException::class,
            KeyProperties.PURPOSE_DECRYPT to IllegalArgumentException::class,
            (KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT or KeyProperties.PURPOSE_AGREE_KEY) to IllegalArgumentException::class,
            (KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT) to null,
        ).forAll { (keyPurposes, expectedExceptionClass) ->
            val spec = KeyGenParameterSpec.Builder("key alias", keyPurposes)
                .applySafeDefaultSpec()
                .build()

            val result = runCatching { getOrCreateMasterKey(spec) }

            val actualException = result.exceptionOrNull()
            actualException?.javaClass?.kotlin shouldBe expectedExceptionClass
            // This ensures that validation fails correctly on purposes and not something else, because
            // we can't just get safe default spec builder and modify it as for other validation tests,
            // because key purposes need to be passed in the constructor of the builder.
            if (actualException?.message != null) {
                actualException.message
                    .shouldNotBeNull()
                    .shouldContain("PURPOSE_ENCRYPT or PURPOSE_DECRYPT")
            }
        }
    }

    private fun KeyGenParameterSpec.Builder.applySafeDefaultSpec(): KeyGenParameterSpec.Builder {
        val defaultSpec = MasterKey.createSafeDefaultSpecBuilder().build()
        return setBlockModes(*defaultSpec.blockModes)
            .setEncryptionPaddings(*defaultSpec.encryptionPaddings)
            .setKeySize(defaultSpec.keySize)
    }

    @Test
    fun `validate encryption paddings`() = runTest {
        listOf(
            arrayOf(KeyProperties.ENCRYPTION_PADDING_PKCS7) to IllegalArgumentException::class,
            arrayOf(KeyProperties.ENCRYPTION_PADDING_RSA_OAEP) to IllegalArgumentException::class,
            arrayOf(KeyProperties.ENCRYPTION_PADDING_RSA_PKCS1) to IllegalArgumentException::class,
            arrayOf(KeyProperties.ENCRYPTION_PADDING_PKCS7, KeyProperties.ENCRYPTION_PADDING_RSA_OAEP) to IllegalArgumentException::class,
            arrayOf(KeyProperties.ENCRYPTION_PADDING_NONE) to null,
        ).forAll { (encryptionPaddings, expectedExceptionClass) ->
            val spec = MasterKey.createSafeDefaultSpecBuilder().setEncryptionPaddings(*encryptionPaddings).build()

            validateAndAssert(spec, expectedExceptionClass)
        }
    }

    @Test
    fun `validate user authentication`() = runTest {
        data class Params(val userAuthRequired: Boolean, val validityDurationSeconds: Int)

        listOf(
            Params(userAuthRequired = false, validityDurationSeconds = 1) to null,
            Params(userAuthRequired = false, validityDurationSeconds = 0) to null,
            Params(userAuthRequired = true, validityDurationSeconds = 1) to null,
            Params(userAuthRequired = true, validityDurationSeconds = 0) to IllegalArgumentException::class,
        ).forAll { (params, expectedExceptionClass) ->
            val spec = MasterKey.createSafeDefaultSpecBuilder()
                .setUserAuthenticationRequired(params.userAuthRequired)
                .setUserAuthenticationParameters(params.validityDurationSeconds, KeyProperties.AUTH_BIOMETRIC_STRONG)
                .build()

            validateAndAssert(spec, expectedExceptionClass)
        }
    }

    @Test
    fun `generate correct key`() = runTest {
        val expectedKeyAlias = "key alias"

        getOrCreateMasterKey(MasterKey.createSafeDefaultSpecBuilder(expectedKeyAlias).build())

        loadAndroidKeyStore().getMasterKey(expectedKeyAlias).algorithm shouldBe "AES"
    }

    private fun loadAndroidKeyStore(): KeyStore {
        return KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
    }

    private fun KeyStore.getMasterKey(alias: String): SecretKey {
        return getEntry(alias, null)
            .shouldBeInstanceOf<KeyStore.SecretKeyEntry>()
            .secretKey
    }

    @Test
    fun `do not generate new key if key already exist`() = runTest {
        val keyAlias = DEFAULT_MASTER_KEY_ALIAS
        val keyStore = loadAndroidKeyStore()
        getOrCreateMasterKey()
        val firstKey = keyStore.getMasterKey(keyAlias)

        getOrCreateMasterKey()

        keyStore.getMasterKey(keyAlias) shouldBe firstKey
    }

    @Test
    fun `synchronize key generation`() = runTest {
        repeat(100) { keyAliasIndex ->
            val keyAlias = "key_alias_$keyAliasIndex"
            val spec = MasterKey.createSafeDefaultSpecBuilder(keyAlias).build()
            (1..8).map {
                launch(Dispatchers.Default) { getOrCreateMasterKey(spec) }
            }.joinAll()
            fakeKeyStoreRule.getKeyHistory(keyAlias) shouldHaveSize 1
        }
    }

    @Test
    fun `get correct master key`() = runTest {
        val expectedKeyAlias = "key alias"

        val actual = getOrCreateMasterKey(MasterKey.createSafeDefaultSpecBuilder(expectedKeyAlias).build())

        actual.alias shouldBe expectedKeyAlias
    }
}
