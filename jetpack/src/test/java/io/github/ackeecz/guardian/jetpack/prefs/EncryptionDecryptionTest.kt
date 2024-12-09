package io.github.ackeecz.guardian.jetpack.prefs

import android.content.SharedPreferences
import com.google.crypto.tink.Aead
import com.google.crypto.tink.DeterministicAead
import com.google.crypto.tink.KeyTemplate
import com.google.crypto.tink.KeysetHandle
import com.google.crypto.tink.aead.AesGcmKeyManager
import com.google.crypto.tink.daead.AesSivKeyManager
import com.google.crypto.tink.proto.AesGcmKey
import com.google.crypto.tink.proto.AesSivKey
import com.google.crypto.tink.shaded.protobuf.ByteString
import com.google.crypto.tink.subtle.Base64
import io.github.ackeecz.guardian.core.MasterKey
import io.github.ackeecz.guardian.core.internal.getFirstKeyDataValue
import io.github.ackeecz.guardian.core.internal.remainingToByteArray
import io.github.ackeecz.guardian.core.internal.shouldHaveBitSize
import io.github.ackeecz.guardian.core.internal.shouldHaveTypeUrlFromTemplate
import io.github.ackeecz.guardian.core.internal.utf8ByteSize
import io.github.ackeecz.guardian.jetpack.EncryptedSharedPreferences
import io.github.ackeecz.guardian.jetpack.EncryptedSharedPreferences.PrefKeyEncryptionScheme
import io.github.ackeecz.guardian.jetpack.EncryptedSharedPreferences.PrefValueEncryptionScheme
import io.kotest.inspectors.forAll
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.test.runTest
import org.junit.Test
import java.nio.ByteBuffer

internal class EncryptionDecryptionTest : EncryptedSharedPreferencesTest() {

    @Test
    fun `generate data encryption key in keyset for shared prefs keys encryption`() = runTest {
        testDataEncryptionKeyGeneration(keysetAlias = KEY_KEYSET_ALIAS)
    }

    private suspend fun testDataEncryptionKeyGeneration(keysetAlias: String) {
        val prefsFileName = "shared_prefs"
        val underTest = createSut(prefsFileName = prefsFileName)

        underTest.forceEncryptionKeysGeneration()

        assertKeysetExists(prefsFileName = prefsFileName, keysetAlias = keysetAlias)
    }

    @Suppress("SameParameterValue")
    private fun assertKeysetExists(prefsFileName: String, keysetAlias: String) {
        getSharedPreferences(prefsFileName)
            .getString(keysetAlias, null)
            .shouldNotBeNull()
    }

    @Test
    fun `generate data encryption key in keyset for shared prefs values encryption`() = runTest {
        testDataEncryptionKeyGeneration(keysetAlias = VALUE_KEYSET_ALIAS)
    }

    /**
     * Tests encryption, but also:
     * - encryption of data encryption key with master key
     * - correct generation of data encryption key
     * - encoding encrypted prefs keys to base64
     */
    @Test
    fun `encrypt shared prefs non-null keys with data encryption key`() = runTest {
        val expectedPreferenceKey = "expected plain text key"
        testSharedPrefsKeysEncryption(
            preferenceKey = expectedPreferenceKey,
            expectedPreferenceKey = expectedPreferenceKey,
        )
    }

    private suspend fun testSharedPrefsKeysEncryption(
        preferenceKey: String?,
        expectedPreferenceKey: String,
    ) {
        val prefsFileName = "shared_prefs"
        val masterKey = MasterKey.getOrCreate()
        val underTest = createSut(
            prefsFileName = prefsFileName,
            getMasterKey = { masterKey },
        )

        getPutMethods().forAll { putMethod ->
            underTest.edit { putMethod(preferenceKey) }

            val prefsKeyAead = getPrefsKeyAead(masterKey, prefsFileName)
            val plainTextPrefsKeys = prefsKeyAead.decryptAllPrefsKeys(prefsFileName)
            plainTextPrefsKeys shouldContain expectedPreferenceKey
        }
    }

    private fun getPutMethods(): List<suspend EncryptedSharedPreferences.Editor.(String?) -> Unit> {
        return listOf(
            { putBoolean(it, true) },
            { putLong(it, 0) },
            { putFloat(it, 0f) },
            { putInt(it, 0) },
            { putString(it, "") },
            { putStringSet(it, setOf()) },
        )
    }

    private fun getPrefsKeyAead(masterKey: MasterKey, prefsFileName: String): DeterministicAead {
        return getPrefsKeyKeysetHandle(
            masterKey = masterKey,
            prefsFileName = prefsFileName,
        ).getPrimitive(DeterministicAead::class.java)
    }

    private fun getPrefsKeyKeysetHandle(masterKey: MasterKey, prefsFileName: String): KeysetHandle {
        return getKeysetHandle(
            masterKey = masterKey,
            prefsFileName = prefsFileName,
            keysetAlias = KEY_KEYSET_ALIAS,
        )
    }

    private fun getKeysetHandle(
        masterKey: MasterKey,
        prefsFileName: String,
        keysetAlias: String,
    ): KeysetHandle {
        return io.github.ackeecz.guardian.core.internal.getKeysetHandle(
            context = context,
            masterKeyUri = masterKey.keyStoreUri,
            keysetPrefsName = prefsFileName,
            keysetAlias = keysetAlias,
        )
    }

    private fun DeterministicAead.decryptAllPrefsKeys(prefsFileName: String): List<String> {
        val encryptedPrefsKeys = getAllEncryptedPrefsKeys(prefsFileName)
        return decryptPreferenceKeys(encryptedPrefsKeys, prefsFileName)
    }

    /**
     * Returns all encrypted keys from shared preferences of [prefsFileName]
     */
    private fun getAllEncryptedPrefsKeys(prefsFileName: String): List<String> {
        return getSharedPreferences(prefsFileName).all.keys
            .filterNot { it == KEY_KEYSET_ALIAS }
            .filterNot { it == VALUE_KEYSET_ALIAS }
    }

    private fun DeterministicAead.decryptPreferenceKeys(
        encryptedPreferenceKeys: List<String>,
        prefsFileName: String,
    ): List<String> {
        return encryptedPreferenceKeys.map { decryptPreferenceKey(it, prefsFileName) }
    }

    private fun DeterministicAead.decryptPreferenceKey(
        encryptedPreferenceKey: String,
        prefsFileName: String,
    ): String {
        return decryptDeterministically(
            Base64.decode(encryptedPreferenceKey),
            prefsFileName.toByteArray(Charsets.UTF_8),
        ).toString(Charsets.UTF_8)
    }

    @Test
    fun `encrypt shared prefs null keys with data encryption key`() = runTest {
        testSharedPrefsKeysEncryption(preferenceKey = null, expectedPreferenceKey = NULL_VALUE)
    }

    @Test
    fun `decrypt shared prefs non-null keys with data encryption key`() = runTest {
        testSharedPrefsKeysDecryption(prefsKey = "expected plain text key")
    }

    private suspend fun testSharedPrefsKeysDecryption(prefsKey: String?) {
        val underTest = createSut()

        getPutMethods().forAll { putMethod ->
            underTest.edit { putMethod(prefsKey) }

            underTest.getAll().keys shouldContain prefsKey
        }
    }

    @Test
    fun `decrypt shared prefs null keys with data encryption key`() = runTest {
        testSharedPrefsKeysDecryption(prefsKey = null)
    }

    /**
     * Tests encryption, but also:
     * - encryption of data encryption key with master key
     * - correct generation of data encryption key
     * - encoding of encrypted prefs values
     */
    @Test
    fun `encrypt shared prefs values with data encryption key`() = runTest {
        ValueType.entries.flatMap { type ->
            when (type) {
                ValueType.STRING -> getStringValueEncryptionTestData(type)
                ValueType.STRING_SET -> getStringSetValueEncryptionTestData(type)
                ValueType.INT -> getIntValueEncryptionTestData(type)
                ValueType.LONG -> getLongValueEncryptionTestData(type)
                ValueType.FLOAT -> getFloatValueEncryptionTestData(type)
                ValueType.BOOLEAN -> getBooleanValueEncryptionTestData(type)
            }
        }.forAll { testSharedPrefsValuesEncryption(it) }
    }

    private fun getStringValueEncryptionTestData(type: ValueType): List<ValueEncryptionTestData> {
        val nullTestData = ValueEncryptionTestData(
            expectedValueType = type,
            put = { key -> putString(key, null) },
            assertValue = { byteBuffer -> byteBuffer.assertStringValue(NULL_VALUE) },
        )
        return listOf("", "value").map { expectedValue ->
            ValueEncryptionTestData(
                expectedValueType = type,
                put = { key -> putString(key, expectedValue) },
                assertValue = { byteBuffer -> byteBuffer.assertStringValue(expectedValue) },
            )
        } + listOf(nullTestData)
    }

    private fun ByteBuffer.assertStringValue(expectedValue: String) {
        // Asserts String byte length
        getInt() shouldBe expectedValue.utf8ByteSize
        // Asserts String value
        remainingToByteArray()
            .toString(Charsets.UTF_8)
            .shouldBe(expectedValue)
    }

    private fun getStringSetValueEncryptionTestData(type: ValueType): List<ValueEncryptionTestData> {
        return listOf(
            ValueEncryptionTestData(
                expectedValueType = type,
                put = { key -> putStringSet(key, null) },
                assertValue = { byteBuffer -> byteBuffer.assertStringSetItemValue(NULL_VALUE) },
            ),
            ValueEncryptionTestData(
                expectedValueType = type,
                put = { key -> putStringSet(key, emptySet()) },
                assertValue = { byteBuffer -> byteBuffer.remaining() shouldBe 0 },
            ),
            setOf(null).let { expectedSet ->
                ValueEncryptionTestData(
                    expectedValueType = type,
                    put = { key -> putStringSet(key, expectedSet) },
                    assertValue = { byteBuffer -> byteBuffer.assertStringSetItemValue(NULL_ITEM_VALUE) },
                )
            },
            setOf("first", null, "third").let { expectedSet ->
                ValueEncryptionTestData(
                    expectedValueType = type,
                    put = { key -> putStringSet(key, expectedSet) },
                    assertValue = { byteBuffer ->
                        expectedSet.forAll { item ->
                            val expectedItem = if (item == null) NULL_ITEM_VALUE else item
                            byteBuffer.assertStringSetItemValue(expectedItem)
                        }
                    },
                )
            },
            setOf("first", "second", "third").let { expectedSet ->
                ValueEncryptionTestData(
                    expectedValueType = type,
                    put = { key -> putStringSet(key, expectedSet) },
                    assertValue = { byteBuffer ->
                        expectedSet.forAll { byteBuffer.assertStringSetItemValue(it) }
                    },
                )
            },
        )
    }

    private fun ByteBuffer.assertStringSetItemValue(expectedSetItemValue: String) {
        val itemLength = getInt()
        val itemSlice = slice().also { it.limit(itemLength) }
        position(position() + itemLength)
        itemSlice.remainingToByteArray()
            .toString(Charsets.UTF_8)
            .shouldBe(expectedSetItemValue)
    }

    private fun getIntValueEncryptionTestData(type: ValueType): List<ValueEncryptionTestData> {
        return listOf(Int.MIN_VALUE, 0, Int.MAX_VALUE).map { expected ->
            ValueEncryptionTestData(
                expectedValueType = type,
                put = { key -> putInt(key, expected) },
                assertValue = { byteBuffer -> byteBuffer.getInt() shouldBe expected },
            )
        }
    }

    private fun getLongValueEncryptionTestData(type: ValueType): List<ValueEncryptionTestData> {
        return listOf(Long.MIN_VALUE, 0, Long.MAX_VALUE).map { expected ->
            ValueEncryptionTestData(
                expectedValueType = type,
                put = { key -> putLong(key, expected) },
                assertValue = { byteBuffer -> byteBuffer.getLong() shouldBe expected },
            )
        }
    }

    private fun getFloatValueEncryptionTestData(type: ValueType): List<ValueEncryptionTestData> {
        return listOf(Float.MIN_VALUE, 0f, Float.MAX_VALUE).map { expected ->
            ValueEncryptionTestData(
                expectedValueType = type,
                put = { key -> putFloat(key, expected) },
                assertValue = { byteBuffer -> byteBuffer.getFloat() shouldBe expected },
            )
        }
    }

    private fun getBooleanValueEncryptionTestData(type: ValueType): List<ValueEncryptionTestData> {
        return listOf(
            ValueEncryptionTestData(
                expectedValueType = type,
                put = { key -> putBoolean(key, false) },
                assertValue = { byteBuffer -> byteBuffer.get() shouldBe 0.toByte() },
            ),
            ValueEncryptionTestData(
                expectedValueType = type,
                put = { key -> putBoolean(key, true) },
                assertValue = { byteBuffer -> byteBuffer.get() shouldBe 1.toByte() },
            ),
        )
    }

    private suspend fun testSharedPrefsValuesEncryption(testData: ValueEncryptionTestData) {
        val prefsFileName = "shared_prefs"
        val preferenceKey = "key"
        val masterKey = MasterKey.getOrCreate()
        val underTest = createSut(
            prefsFileName = prefsFileName,
            getMasterKey = { masterKey },
        )

        underTest.edit { testData.put(this, preferenceKey) }

        val encryptedPreferenceKey = requireEncryptedPreferenceKey(
            masterKey = masterKey,
            prefsFileName = prefsFileName,
            preferenceKey = preferenceKey,
        )
        val decryptedPreferenceValue = getAndDecryptPreferenceValue(
            masterKey = masterKey,
            prefsFileName = prefsFileName,
            encryptedPreferenceKey = encryptedPreferenceKey,
        )
        with(ByteBuffer.wrap(decryptedPreferenceValue)) {
            position(0)
            getInt() shouldBe testData.expectedValueType.typeId
            testData.assertValue(this)
        }
    }

    /**
     * Returns encrypted preference key matching passed plain text [preferenceKey] from [SharedPreferences]
     * of [prefsFileName].
     */
    private fun requireEncryptedPreferenceKey(
        masterKey: MasterKey,
        prefsFileName: String,
        preferenceKey: String,
    ): String {
        val prefsKeyAead = getPrefsKeyAead(masterKey, prefsFileName)
        return getAllEncryptedPrefsKeys(prefsFileName).find { encryptedPreferenceKey ->
            val decryptedPreferenceKey = prefsKeyAead.decryptPreferenceKey(
                encryptedPreferenceKey = encryptedPreferenceKey,
                prefsFileName = prefsFileName,
            )
            decryptedPreferenceKey == preferenceKey
        }.let(::requireNotNull)
    }

    private fun getAndDecryptPreferenceValue(
        masterKey: MasterKey,
        prefsFileName: String,
        encryptedPreferenceKey: String,
    ): ByteArray {
        val prefsValueAead = getPrefsValueAead(masterKey, prefsFileName)
        val encryptedPreferenceValue = getSharedPreferences(prefsFileName)
            .getString(encryptedPreferenceKey, null)
        return prefsValueAead.decrypt(
            Base64.decode(encryptedPreferenceValue, Base64.DEFAULT),
            encryptedPreferenceKey.toByteArray(Charsets.UTF_8),
        )
    }

    private fun getPrefsValueAead(masterKey: MasterKey, prefsFileName: String): Aead {
        return getPrefsValueKeysetHandle(
            masterKey = masterKey,
            prefsFileName = prefsFileName,
        ).getPrimitive(Aead::class.java)
    }

    private fun getPrefsValueKeysetHandle(masterKey: MasterKey, prefsFileName: String): KeysetHandle {
        return getKeysetHandle(
            masterKey = masterKey,
            prefsFileName = prefsFileName,
            keysetAlias = VALUE_KEYSET_ALIAS,
        )
    }

    @Test
    fun `decrypt shared prefs values with data encryption key`() = runTest {
        ValueType.entries.flatMap { type ->
            when (type) {
                ValueType.STRING -> getStringValueDecryptionTestData()
                ValueType.STRING_SET -> getStringSetValueDecryptionTestData()
                ValueType.INT -> getIntValueDecryptionTestData()
                ValueType.LONG -> getLongValueDecryptionTestData()
                ValueType.FLOAT -> getFloatValueDecryptionTestData()
                ValueType.BOOLEAN -> getBooleanValueDecryptionTestData()
            }
        }.forAll { (put, assertValue) ->
            val preferenceKey = "key"
            val underTest = createSut()

            underTest.edit { put(preferenceKey) }

            underTest.assertValue(preferenceKey)
        }
    }

    private fun getStringValueDecryptionTestData(): List<ValueDecryptionTestData> {
        val expectedDefaultValue = "default"
        val nullTestData = ValueDecryptionTestData(
            put = { key -> putString(key, null) },
            assertValue = { key -> getString(key, expectedDefaultValue) shouldBe expectedDefaultValue },
        )
        return listOf("", "value").map { expectedValue ->
            ValueDecryptionTestData(
                put = { key -> putString(key, expectedValue) },
                assertValue = { key -> getString(key, null) shouldBe expectedValue },
            )
        } + listOf(nullTestData)
    }

    private fun getStringSetValueDecryptionTestData(): List<ValueDecryptionTestData> {
        val expectedDefaultValue = setOf("default")
        val nullTestData = ValueDecryptionTestData(
            put = { key -> putStringSet(key, null) },
            assertValue = { key -> getStringSet(key, expectedDefaultValue) shouldBe expectedDefaultValue },
        )
        return listOf(
            emptySet(),
            setOf(null),
            setOf("first", null, "third"),
            setOf("first", "second", "third"),
        ).map { expectedValue ->
            ValueDecryptionTestData(
                put = { key -> putStringSet(key, expectedValue) },
                assertValue = { key -> getStringSet(key, null) shouldBe expectedValue },
            )
        } + listOf(nullTestData)
    }

    private fun getIntValueDecryptionTestData(): List<ValueDecryptionTestData> {
        return listOf(Int.MIN_VALUE, 0, Int.MAX_VALUE).map { expectedValue ->
            ValueDecryptionTestData(
                put = { key -> putInt(key, expectedValue) },
                assertValue = { key -> getInt(key, -1) shouldBe expectedValue },
            )
        }
    }

    private fun getLongValueDecryptionTestData(): List<ValueDecryptionTestData> {
        return listOf(Long.MIN_VALUE, 0, Long.MAX_VALUE).map { expectedValue ->
            ValueDecryptionTestData(
                put = { key -> putLong(key, expectedValue) },
                assertValue = { key -> getLong(key, -1) shouldBe expectedValue },
            )
        }
    }

    private fun getFloatValueDecryptionTestData(): List<ValueDecryptionTestData> {
        return listOf(Float.MIN_VALUE, 0f, Float.MAX_VALUE).map { expectedValue ->
            ValueDecryptionTestData(
                put = { key -> putFloat(key, expectedValue) },
                assertValue = { key -> getFloat(key, -1f) shouldBe expectedValue },
            )
        }
    }

    private fun getBooleanValueDecryptionTestData(): List<ValueDecryptionTestData> {
        return listOf(false, true).map { expectedValue ->
            ValueDecryptionTestData(
                put = { key -> putBoolean(key, expectedValue) },
                assertValue = { key -> getBoolean(key, !expectedValue) shouldBe expectedValue },
            )
        }
    }

    @Test
    fun `apply AES256_SIV pref key encryption scheme`() = runTest {
        val prefsFileName = "prefs"
        val masterKey = MasterKey.getOrCreate()
        val underTest = createSut(
            prefsFileName = prefsFileName,
            getMasterKey = { masterKey },
            prefKeyEncryptionScheme = PrefKeyEncryptionScheme.AES256_SIV,
        )

        underTest.forceEncryptionKeysGeneration()

        getPrefsKeyKeysetHandle(masterKey, prefsFileName).assertEncryptionScheme(
            // Asserts AES alg and SIV mode
            expectedKeyTemplate = AesSivKeyManager.aes256SivTemplate(),
            // 512 is actually correct, because the key is split to two 256-bit keys (for encryption
            // and authentication) and AES-256 refers to the size of the encryption key only
            expectedKeyBitSize = 512,
            getKeyValue = { AesSivKey.parseFrom(it).keyValue },
        )
    }

    private fun KeysetHandle.assertEncryptionScheme(
        expectedKeyTemplate: KeyTemplate,
        expectedKeyBitSize: Int,
        getKeyValue: (ByteString) -> ByteString,
    ) {
        this shouldHaveTypeUrlFromTemplate expectedKeyTemplate
        getKeyValue(getFirstKeyDataValue()) shouldHaveBitSize expectedKeyBitSize
    }

    @Test
    fun `apply AES256_GCM pref value encryption scheme`() = runTest {
        val prefsFileName = "prefs"
        val masterKey = MasterKey.getOrCreate()
        val underTest = createSut(
            prefsFileName = prefsFileName,
            getMasterKey = { masterKey },
            prefValueEncryptionScheme = PrefValueEncryptionScheme.AES256_GCM,
        )

        underTest.forceEncryptionKeysGeneration()

        getPrefsValueKeysetHandle(masterKey, prefsFileName).assertEncryptionScheme(
            // Asserts AES alg and GCM mode
            expectedKeyTemplate = AesGcmKeyManager.aes256GcmTemplate(),
            expectedKeyBitSize = 256,
            getKeyValue = { AesGcmKey.parseFrom(it).keyValue },
        )
    }

    private data class ValueEncryptionTestData(
        val expectedValueType: ValueType,
        val put: suspend EncryptedSharedPreferences.Editor.(String) -> Unit,
        val assertValue: (ByteBuffer) -> Unit,
    )

    private data class ValueDecryptionTestData(
        val put: suspend EncryptedSharedPreferences.Editor.(String) -> Unit,
        val assertValue: suspend EncryptedSharedPreferences.(String) -> Unit,
    )
}
