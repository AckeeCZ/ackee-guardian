package io.github.ackeecz.guardian.jetpack.prefs

import io.github.ackeecz.guardian.jetpack.EncryptedSharedPreferences
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.inspectors.forAll
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.maps.shouldBeEmpty
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.test.runTest
import org.junit.Test

internal class GetPreferencesTest : EncryptedSharedPreferencesTest() {

    @Test
    fun `get default value if preference is missing`() = runTest {
        ValueType.entries.forAll { type ->
            when (type) {
                ValueType.STRING -> {
                    val expected = "default"
                    createSut().getString("key", expected) shouldBe expected
                }
                ValueType.STRING_SET -> {
                    val expected = setOf("default")
                    createSut().getStringSet("key", expected) shouldBe expected
                }
                ValueType.INT -> {
                    val expected = Int.MIN_VALUE
                    createSut().getInt("key", expected) shouldBe expected
                }
                ValueType.LONG -> {
                    val expected = Long.MIN_VALUE
                    createSut().getLong("key", expected) shouldBe expected
                }
                ValueType.FLOAT -> {
                    val expected = Float.MIN_VALUE
                    createSut().getFloat("key", expected) shouldBe expected
                }
                ValueType.BOOLEAN -> {
                    val expected = true
                    createSut().getBoolean("key", expected) shouldBe expected
                }
            }
        }
    }

    @Test
    fun `get value using null key`() = runTest {
        suspend fun test(
            put: suspend EncryptedSharedPreferences.Editor.() -> Unit,
            assertValue: suspend EncryptedSharedPreferences.() -> Unit,
        ) {
            val underTest = createSut()

            underTest.edit { put() }

            underTest.assertValue()
        }

        ValueType.entries.forAll { type ->
            when (type) {
                ValueType.STRING -> "value".let { expected ->
                    test(
                        put = { putString(null, expected) },
                        assertValue = { getString(null, null) shouldBe expected },
                    )
                }
                ValueType.STRING_SET -> setOf("value").let { expected ->
                    test(
                        put = { putStringSet(null, expected) },
                        assertValue = { getStringSet(null, null) shouldBe expected },
                    )
                }
                ValueType.INT -> Int.MAX_VALUE.let { expected ->
                    test(
                        put = { putInt(null, expected) },
                        assertValue = { getInt(null, 0) shouldBe expected },
                    )
                }
                ValueType.LONG -> Long.MAX_VALUE.let { expected ->
                    test(
                        put = { putLong(null, expected) },
                        assertValue = { getLong(null, 0) shouldBe expected },
                    )
                }
                ValueType.FLOAT -> Float.MAX_VALUE.let { expected ->
                    test(
                        put = { putFloat(null, expected) },
                        assertValue = { getFloat(null, 0f) shouldBe expected },
                    )
                }
                ValueType.BOOLEAN -> true.let { expected ->
                    test(
                        put = { putBoolean(null, expected) },
                        assertValue = { getBoolean(null, false) shouldBe expected },
                    )
                }
            }
        }
    }

    @Test
    fun `throw SecurityException if trying to get value using reserved keys`() = runTest {
        suspend fun test(get: suspend EncryptedSharedPreferences.() -> Unit) {
            shouldThrow<SecurityException> { createSut().get() }
        }

        RESERVED_KEYS.forAll { reservedKey ->
            ValueType.entries.forAll { type ->
                when (type) {
                    ValueType.STRING -> {
                        test { getString(reservedKey, null) }
                    }
                    ValueType.STRING_SET -> {
                        test { getStringSet(reservedKey, null) }
                    }
                    ValueType.INT -> {
                        test { getInt(reservedKey, 0) }
                    }
                    ValueType.LONG -> {
                        test { getLong(reservedKey, 0) }
                    }
                    ValueType.FLOAT -> {
                        test { getFloat(reservedKey, 0f) }
                    }
                    ValueType.BOOLEAN -> {
                        test { getBoolean(reservedKey, false) }
                    }
                }
            }
        }
    }

    @Test
    fun `throw ClassCastException if trying to get value of incorrect type`() = runTest {
        suspend fun test(
            put: suspend EncryptedSharedPreferences.Editor.(String) -> Unit,
            get: suspend EncryptedSharedPreferences.(String) -> Unit,
        ) {
            val key = "key"
            val underTest = createSut()
            underTest.edit { put(key) }

            shouldThrow<ClassCastException> { underTest.get(key) }
        }

        ValueType.entries.forAll { type ->
            when (type) {
                ValueType.STRING -> {
                    test(
                        put = { putInt(it, 0) },
                        get = { getString(it, null) },
                    )
                }
                ValueType.STRING_SET -> {
                    test(
                        put = { putInt(it, 0) },
                        get = { getStringSet(it, null) },
                    )
                }
                ValueType.INT -> {
                    test(
                        put = { putBoolean(it, false) },
                        get = { getInt(it, 0) },
                    )
                }
                ValueType.LONG -> {
                    test(
                        put = { putInt(it, 0) },
                        get = { getLong(it, 0) },
                    )
                }
                ValueType.FLOAT -> {
                    test(
                        put = { putInt(it, 0) },
                        get = { getFloat(it, 0f) },
                    )
                }
                ValueType.BOOLEAN -> {
                    test(
                        put = { putInt(it, 0) },
                        get = { getBoolean(it, false) },
                    )
                }
            }
        }
    }

    @Test
    @Suppress("LongMethod")
    fun `get all key value pairs for all value types`() = runTest {
        data class TestData(
            val put: suspend EncryptedSharedPreferences.Editor.() -> Unit,
            val getExpectedKeyValuePair: () -> Pair<String, Any>,
        )

        val testData = ValueType.entries.map { type ->
            when (type) {
                ValueType.STRING -> {
                    val expectedKey = "string key"
                    val expectedValue = "string value"
                    TestData(
                        put = { putString(expectedKey, expectedValue) },
                        getExpectedKeyValuePair = { expectedKey to expectedValue },
                    )
                }
                ValueType.STRING_SET -> {
                    val expectedKey = "string set key"
                    val expectedValue = setOf("first", "second")
                    TestData(
                        put = { putStringSet(expectedKey, expectedValue) },
                        getExpectedKeyValuePair = { expectedKey to expectedValue },
                    )
                }
                ValueType.INT -> {
                    val expectedKey = "int key"
                    val expectedValue = Int.MAX_VALUE
                    TestData(
                        put = { putInt(expectedKey, expectedValue) },
                        getExpectedKeyValuePair = { expectedKey to expectedValue },
                    )
                }
                ValueType.LONG -> {
                    val expectedKey = "long key"
                    val expectedValue = Long.MAX_VALUE
                    TestData(
                        put = { putLong(expectedKey, expectedValue) },
                        getExpectedKeyValuePair = { expectedKey to expectedValue },
                    )
                }
                ValueType.FLOAT -> {
                    val expectedKey = "float key"
                    val expectedValue = Float.MAX_VALUE
                    TestData(
                        put = { putFloat(expectedKey, expectedValue) },
                        getExpectedKeyValuePair = { expectedKey to expectedValue },
                    )
                }
                ValueType.BOOLEAN -> {
                    val expectedKey = "boolean key"
                    val expectedValue = true
                    TestData(
                        put = { putBoolean(expectedKey, expectedValue) },
                        getExpectedKeyValuePair = { expectedKey to expectedValue },
                    )
                }
            }
        }
        val underTest = createSut()
        underTest.edit {
            testData.forEach { it.put(this@edit) }
        }

        val allPairs = underTest.getAll()

        testData
            .map { it.getExpectedKeyValuePair() }
            .forAll { (expectedKey, expectedValue) ->
                allPairs[expectedKey] shouldBe expectedValue
            }
            .shouldHaveSize(ValueType.entries.size)
    }

    @Test
    fun `exclude reserved keys with values from all key value pairs`() = runTest {
        createSut().getAll().shouldBeEmpty()
    }
}
