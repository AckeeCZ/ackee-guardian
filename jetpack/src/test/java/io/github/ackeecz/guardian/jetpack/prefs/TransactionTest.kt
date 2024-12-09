package io.github.ackeecz.guardian.jetpack.prefs

import android.os.Looper
import io.github.ackeecz.guardian.jetpack.EncryptedSharedPreferences
import io.kotest.inspectors.forAll
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldContainOnlyNulls
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.maps.shouldBeEmpty
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.withContext
import org.junit.Test

/**
 * Common abstract ancestor for transaction tests - testing [EncryptedSharedPreferences.Editor.commit]
 * and [EncryptedSharedPreferences.Editor.apply] methods
 */
internal abstract class TransactionTest(
    private val transactionType: TransactionType,
) : EncryptedSharedPreferencesTest() {

    @Test
    fun `remove preference`() = runTest {
        val underTest = createSut()
        val key = "key"
        underTest.edit(commit = transactionType.isCommit.not()) { putBoolean(key, true) }

        underTest.edit(commit = transactionType.isCommit) { remove(key) }

        underTest.getBoolean(key, false) shouldBe false
    }

    @Test
    fun `clear all previously put preferences`() = runTest {
        val underTest = createSut()
        underTest.edit(commit = transactionType.isCommit.not()) {
            putString("key1", "value1")
            putInt("key2", Int.MAX_VALUE)
        }

        underTest.edit(commit = transactionType.isCommit) { clear() }

        underTest.getAll().shouldBeEmpty()
    }

    @Test
    fun `do not clear preferences put in the same editor where clear is called`() = runTest {
        val underTest = createSut()
        underTest.edit(commit = transactionType.isCommit.not()) {
            putString("previous key", "previous value")
        }
        val keptKey1 = "kept key 1"
        val keptKey2 = "kept key 2"

        underTest.edit(commit = transactionType.isCommit) {
            putInt(keptKey1, Int.MAX_VALUE)
            clear()
            putString(keptKey2, "kept value 2")
        }

        underTest.getAll().keys
            .shouldHaveSize(2)
            .shouldContain(keptKey1)
            .shouldContain(keptKey2)
    }

    @Test
    fun `do not clear data encryption keys on clear`() = runTest {
        val prefsFileName = "prefs"
        val underTest = createSut(prefsFileName = prefsFileName)
        underTest.forceEncryptionKeysGeneration()

        underTest.edit(commit = transactionType.isCommit) { clear() }

        val allPrefsKeys = getSharedPreferences(prefsFileName = prefsFileName).all.keys
        RESERVED_KEYS.forAll { allPrefsKeys shouldContain it }
    }

    @Test
    fun `clear preference that was added in the previous commit of the same editor`() = runTest {
        val underTest = createSut()
        underTest.edit().apply {
            putInt("key", Int.MAX_VALUE)
            transactionType.executeTransactionOn(editor = this)

            clear()
            transactionType.executeTransactionOn(editor = this)
        }

        underTest.getAll().shouldBeEmpty()
    }

    @Test
    fun `register and notify listener on changes`() = runTest {
        // Arrange
        val underTest = createSut()

        val changedKey = "changed key"
        val addedKey = "added key"
        val removedKey = "removed key"
        underTest.edit(commit = transactionType.isCommit.not()) {
            putString(changedKey, "old value")
            putString(removedKey, "removed value")
        }

        val notifiedKeys = mutableListOf<String?>()
        onSharedPreferenceChangeListener.onChanged = { _, key -> notifiedKeys += key }
        underTest.registerOnSharedPreferenceChangeListener(onSharedPreferenceChangeListener)

        // Act
        underTest.edit(commit = transactionType.isCommit) {
            putString(changedKey, "new value") // Changed value
            putInt(addedKey, Int.MAX_VALUE) // Added value
            remove(removedKey) // Removed value
        }

        // Assert
        notifiedKeys.shouldHaveSize(3)
            .shouldContain(changedKey)
            .shouldContain(addedKey)
            .shouldContain(removedKey)
    }

    @Test
    fun `OnSharedPreferenceChanged is invoked on main thread`() = runTest {
        // StandardTestDispatcher mocks main dispatcher, but still allows to reliably check for main
        // thread which is not possible for UnconfinedTestDispatcher
        Dispatchers.setMain(StandardTestDispatcher())
        val underTest = createSut()
        var wasNotifiedOnMainThread: Boolean? = null
        onSharedPreferenceChangeListener.onChanged = { _, _ ->
            wasNotifiedOnMainThread = Looper.getMainLooper().isCurrentThread
        }
        underTest.registerOnSharedPreferenceChangeListener(onSharedPreferenceChangeListener)

        withContext(Dispatchers.Default) {
            underTest.edit(commit = transactionType.isCommit) {
                putString("key", "value")
            }
        }

        testScheduler.advanceUntilIdle()
        wasNotifiedOnMainThread shouldBe true
    }

    @Test
    fun `notify listener on null key changes`() = runTest {
        val underTest = createSut()
        val notifiedKeys = mutableListOf<String?>()
        onSharedPreferenceChangeListener.onChanged = { _, key -> notifiedKeys += key }
        underTest.registerOnSharedPreferenceChangeListener(onSharedPreferenceChangeListener)

        underTest.edit(commit = transactionType.isCommit) { putString(null, "old value") }
        underTest.edit(commit = transactionType.isCommit) { putString(null, "new value") }
        underTest.edit(commit = transactionType.isCommit) { remove(null) }

        notifiedKeys.shouldHaveSize(3).shouldContainOnlyNulls()
    }

    @Test
    fun `notify listener just once for multiple puts of same key in one editor`() = runTest {
        val underTest = createSut()
        val notifiedKeys = mutableListOf<String?>()
        onSharedPreferenceChangeListener.onChanged = { _, key -> notifiedKeys += key }
        underTest.registerOnSharedPreferenceChangeListener(onSharedPreferenceChangeListener)
        val key = "key"

        underTest.edit {
            putInt(key, 1)
            putInt(key, 2)
            putInt(key, 3)
        }

        notifiedKeys.shouldHaveSize(1).shouldContain(key)
    }

    @Test
    fun `do not notify listener when put and remove is executed for same key in same editor if key did not exist before`() = runTest {
        val underTest = createSut()
        val notifiedKeys = mutableListOf<String?>()
        onSharedPreferenceChangeListener.onChanged = { _, key -> notifiedKeys += key }
        underTest.registerOnSharedPreferenceChangeListener(onSharedPreferenceChangeListener)
        val key = "key"

        underTest.edit {
            putInt(key, 1)
            putInt(key, 2)
            remove(key)
        }

        notifiedKeys shouldHaveSize 0
    }

    @Test
    fun `notify listener just once when put and remove is executed for same key in same editor if key existed before`() = runTest {
        val underTest = createSut()
        val key = "key"
        underTest.edit { putInt(key, 0) }
        val notifiedKeys = mutableListOf<String?>()
        onSharedPreferenceChangeListener.onChanged = { _, key -> notifiedKeys += key }
        underTest.registerOnSharedPreferenceChangeListener(onSharedPreferenceChangeListener)

        underTest.edit {
            putInt(key, 1)
            remove(key)
        }

        notifiedKeys.shouldHaveSize(1).shouldContain(key)
    }

    internal enum class TransactionType {

        COMMIT {

            override suspend fun executeTransactionOn(editor: EncryptedSharedPreferences.Editor) {
                editor.commit()
            }
        },

        APPLY {

            override suspend fun executeTransactionOn(editor: EncryptedSharedPreferences.Editor) {
                editor.apply()
            }
        };

        val isCommit get() = this == COMMIT

        abstract suspend fun executeTransactionOn(editor: EncryptedSharedPreferences.Editor)
    }
}

/**
 * Tests [EncryptedSharedPreferences.Editor.commit] together with related methods that depend on it
 * like put methods, [EncryptedSharedPreferences.Editor.remove],
 * [EncryptedSharedPreferences.Editor.clear] or notifications.
 */
internal class CommitTest : TransactionTest(TransactionType.COMMIT)

/**
 * Tests [EncryptedSharedPreferences.Editor.apply] together with related methods that depend on it
 * like put methods, [EncryptedSharedPreferences.Editor.remove],
 * [EncryptedSharedPreferences.Editor.clear] or notifications.
 */
internal class ApplyTest : TransactionTest(TransactionType.APPLY)

internal class EditTest : EncryptedSharedPreferencesTest() {

    @Test
    fun `edit preferences using action`() = runTest {
        mapOf(
            false to "apply_key",
            true to "commit_key",
        ).forAll { (commit, key) ->
            val expectedValue = "expected value $key"
            val underTest = createSut()

            underTest.edit(commit = commit) {
                putString(key, expectedValue)
            }

            underTest.getString(key, null) shouldBe expectedValue
        }
    }
}
