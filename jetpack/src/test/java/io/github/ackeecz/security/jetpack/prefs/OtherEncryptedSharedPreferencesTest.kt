package io.github.ackeecz.security.jetpack.prefs

import io.github.ackeecz.security.core.internal.FakeWeakReferenceFactory
import io.github.ackeecz.security.jetpack.EncryptedSharedPreferences
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.inspectors.forAll
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.test.runTest
import org.junit.Test

internal class OtherEncryptedSharedPreferencesTest : EncryptedSharedPreferencesTest() {

    @Test
    fun `throw exception if trying to put value using reserved keys`() = runTest {
        suspend fun test(put: suspend EncryptedSharedPreferences.Editor.() -> Unit) {
            shouldThrow<SecurityException> { createSut().edit().put() }
        }

        RESERVED_KEYS.forAll { reservedKey ->
            ValueType.entries.forAll { type ->
                when (type) {
                    ValueType.STRING -> {
                        test { putString(reservedKey, null) }
                    }
                    ValueType.STRING_SET -> {
                        test { putStringSet(reservedKey, null) }
                    }
                    ValueType.INT -> {
                        test { putInt(reservedKey, 0) }
                    }
                    ValueType.LONG -> {
                        test { putLong(reservedKey, 0) }
                    }
                    ValueType.FLOAT -> {
                        test { putFloat(reservedKey, 0f) }
                    }
                    ValueType.BOOLEAN -> {
                        test { putBoolean(reservedKey, false) }
                    }
                }
            }
        }
    }

    @Test
    fun `throw SecurityException if reserved key is passed to contains method`() = runTest {
        val underTest = createSut()

        RESERVED_KEYS.forAll { reservedKey ->
            shouldThrow<SecurityException> { underTest.contains(reservedKey) }
        }
    }

    @Test
    fun `return true if contains key`() = runTest {
        val underTest = createSut()
        val key = "key"
        underTest.edit { putInt(key, 0) }

        underTest.contains(key) shouldBe true
    }

    @Test
    fun `return false if does not contain key`() = runTest {
        createSut().contains("key") shouldBe false
    }

    @Test
    fun `throw SecurityException if reserved key is passed to remove method`() = runTest {
        val underTest = createSut()

        RESERVED_KEYS.forAll { reservedKey ->
            shouldThrow<SecurityException> {
                underTest.edit { remove(reservedKey) }
            }
        }
    }

    @Test
    fun `unregister prefs change listener`() = runTest {
        val underTest = createSut()
        var listenerCalled = false
        onSharedPreferenceChangeListener.onChanged = { _, _ -> listenerCalled = true }
        underTest.registerOnSharedPreferenceChangeListener(onSharedPreferenceChangeListener)

        underTest.unregisterOnSharedPreferenceChangeListener(onSharedPreferenceChangeListener)

        underTest.edit { putInt("key", 0) }
        listenerCalled shouldBe false
    }

    @Test
    fun `shared preferences does not store a strong reference to the registered listener`() = runTest {
        val weakReferenceFactory = FakeWeakReferenceFactory()
        val underTest = createSut(weakReferenceFactory = weakReferenceFactory)
        var listenerCalled = false

        underTest.registerOnSharedPreferenceChangeListener { _, _ ->
            listenerCalled = true
        }

        weakReferenceFactory.clearAll()
        underTest.edit { putInt("key", 0) }
        listenerCalled shouldBe false
    }
}
