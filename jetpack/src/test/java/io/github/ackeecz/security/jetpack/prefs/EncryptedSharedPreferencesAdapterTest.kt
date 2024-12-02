package io.github.ackeecz.security.jetpack.prefs

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import androidx.test.core.app.ApplicationProvider
import io.github.ackeecz.security.core.MasterKey
import io.github.ackeecz.security.core.internal.AndroidTestWithKeyStore
import io.github.ackeecz.security.core.internal.FakeWeakReferenceFactory
import io.github.ackeecz.security.core.internal.junit.rule.CoroutineRule
import io.github.ackeecz.security.jetpack.EncryptedSharedPreferences
import io.github.ackeecz.security.jetpack.EncryptedSharedPreferencesAdapter
import io.kotest.common.runBlocking
import io.kotest.inspectors.forAll
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.maps.shouldContain
import io.kotest.matchers.maps.shouldHaveSize
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import org.junit.Before
import org.junit.Rule
import org.junit.Test

internal class EncryptedSharedPreferencesAdapterTest : AndroidTestWithKeyStore() {

    @get:Rule
    val coroutineRule: CoroutineRule = CoroutineRule(UnconfinedTestDispatcher())

    private val context: Context get() = ApplicationProvider.getApplicationContext()

    private val weakReferenceFactory = FakeWeakReferenceFactory()

    private lateinit var underTest: EncryptedSharedPreferencesAdapter

    @Before
    fun setUp() = runBlocking {
        val encryptedPrefs = EncryptedSharedPreferences.create(
            context = context,
            fileName = "prefs_name",
            getMasterKey = { MasterKey.getOrCreate() },
            prefKeyEncryptionScheme = EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            prefValueEncryptionScheme = EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
            weakReferenceFactory = weakReferenceFactory,
            defaultDispatcher = coroutineRule.testDispatcher,
        )
        underTest = EncryptedSharedPreferencesAdapter(encryptedPrefs, weakReferenceFactory)
    }

    @Test
    fun `put and get string`() {
        val key = "key"
        val expected = "expected"

        underTest.edit { putString(key, expected) }

        underTest.getString(key, null) shouldBe expected
    }

    @Test
    fun `get default value if string not found`() {
        val key = "key"
        val expected = "expected"

        underTest.getString(key, expected) shouldBe expected
    }

    @Test
    fun `put and get string set`() {
        val key = "key"
        val expected = setOf("first", "second", null)

        underTest.edit { putStringSet(key, expected) }

        underTest.getStringSet(key, null) shouldBe expected
    }

    @Test
    fun `get default value if string set not found`() {
        val key = "key"
        val expected = setOf("expected")

        underTest.getStringSet(key, expected) shouldBe expected
    }

    @Test
    fun `put and get int`() {
        val key = "key"
        val expected = 1

        underTest.edit { putInt(key, expected) }

        underTest.getInt(key, expected + 1) shouldBe expected
    }

    @Test
    fun `get default value if int not found`() {
        val key = "key"
        val expected = Int.MAX_VALUE

        underTest.getInt(key, expected) shouldBe expected
    }

    @Test
    fun `put and get long`() {
        val key = "key"
        val expected = 1L

        underTest.edit { putLong(key, expected) }

        underTest.getLong(key, expected + 1) shouldBe expected
    }

    @Test
    fun `get default value if long not found`() {
        val key = "key"
        val expected = Long.MAX_VALUE

        underTest.getLong(key, expected) shouldBe expected
    }

    @Test
    fun `put and get float`() {
        val key = "key"
        val expected = 1F

        underTest.edit { putFloat(key, expected) }

        underTest.getFloat(key, expected + 1) shouldBe expected
    }

    @Test
    fun `get default value if float not found`() {
        val key = "key"
        val expected = Float.MAX_VALUE

        underTest.getFloat(key, expected) shouldBe expected
    }

    @Test
    fun `put and get boolean`() {
        val key = "key"
        val expected = false

        underTest.edit { putBoolean(key, expected) }

        underTest.getBoolean(key, !expected) shouldBe expected
    }

    @Test
    fun `get default value if boolean not found`() {
        listOf(false, true).forAll { expected ->
            val key = "key"

            underTest.getBoolean(key, expected) shouldBe expected
        }
    }

    @Test
    fun `contains returns false if value is not present`() {
        underTest.contains("key") shouldBe false
    }

    @Test
    fun `contains returns true if value is present`() {
        val key = "key"
        underTest.edit { putString(key, "value") }

        underTest.contains(key) shouldBe true
    }

    @Test
    fun `get all entries`() {
        val stringKey = "string_key"
        val stringValue = "string value"
        val intKey = "int_key"
        val intValue = Int.MAX_VALUE
        underTest.edit {
            putString(stringKey, stringValue)
            putInt(intKey, intValue)
        }

        with(underTest.all) {
            shouldHaveSize(2)
            shouldContain(stringKey to stringValue)
            shouldContain(intKey to intValue)
        }
    }

    @Test
    fun `register listener`() {
        val notifiedKeys = mutableListOf<String?>()
        // Keeping listener in local variable to have a strong reference and avoid clearing from
        // memory before test finish. For more info see SharedPreferences docs.
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            notifiedKeys += key
        }

        underTest.registerOnSharedPreferenceChangeListener(listener)

        val expectedKey = "key"
        underTest.edit { putString(expectedKey, "") }
        notifiedKeys shouldContainExactly listOf(expectedKey)
    }

    @Test
    fun `unregister listener`() {
        val notifiedKeys1 = mutableListOf<String?>()
        val notifiedKeys2 = mutableListOf<String?>()
        // Keeping listeners in local variables to have a strong references and avoid clearing from
        // memory before test finish. For more info see SharedPreferences docs.
        val listener1 = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            notifiedKeys1 += key
        }
        val listener2 = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            notifiedKeys2 += key
        }
        underTest.registerOnSharedPreferenceChangeListener(listener1)
        underTest.registerOnSharedPreferenceChangeListener(listener2)

        underTest.unregisterOnSharedPreferenceChangeListener(listener1)

        val expectedKey = "key"
        underTest.edit { putString(expectedKey, "") }
        notifiedKeys1.shouldBeEmpty()
        notifiedKeys2 shouldContainExactly listOf(expectedKey)
    }

    @Test
    fun `registered listener is stored as weak reference`() {
        val notifiedKeys = mutableListOf<String?>()

        underTest.registerOnSharedPreferenceChangeListener { _, key ->
            notifiedKeys += key
        }

        weakReferenceFactory.clearAll()
        underTest.edit { putString("key", "") }
        notifiedKeys.shouldBeEmpty()
    }

    @Test
    fun `remove preference`() {
        val key = "key"
        underTest.edit { putString(key, "value") }

        underTest.edit { remove(key) }

        underTest.getString(key, null) shouldBe null
    }

    @Test
    fun clear() {
        val key = "key"
        underTest.edit { putString(key, "value") }

        underTest.edit { clear() }

        underTest.getString(key, null) shouldBe null
    }

    @Test
    fun apply() {
        testTransaction(commit = false)
    }

    private fun testTransaction(commit: Boolean) {
        val key = "key"
        val expected = "expected"

        underTest.edit(commit = commit) { putString(key, expected) }

        underTest.getString(key, null) shouldBe expected
    }

    @Test
    fun commit() {
        testTransaction(commit = true)
    }
}
