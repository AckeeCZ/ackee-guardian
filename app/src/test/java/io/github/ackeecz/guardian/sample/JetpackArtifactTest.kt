package io.github.ackeecz.guardian.sample

import android.content.Context
import android.content.SharedPreferences
import androidx.test.core.app.ApplicationProvider
import io.github.ackeecz.guardian.core.MasterKey
import io.github.ackeecz.guardian.jetpack.EncryptedFile
import io.github.ackeecz.guardian.jetpack.EncryptedSharedPreferences
import io.github.ackeecz.guardian.jetpack.FileKeysetConfig
import io.github.ackeecz.guardian.jetpack.adaptToSharedPreferences
import io.github.ackeecz.guardian.sample.junit.rule.CoroutineRule
import io.kotest.matchers.maps.shouldHaveSize
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import java.io.File
import java.util.UUID
import kotlin.random.Random

internal class JetpackArtifactTest : AndroidTestWithKeyStore() {

    private val context: Context = ApplicationProvider.getApplicationContext()

    @get:Rule
    val coroutineRule = CoroutineRule(UnconfinedTestDispatcher())

    @Test
    fun `encrypt file`() = runTest {
        val underTest = EncryptedFile.Builder(
            file = context.createRandomFile(),
            context = context,
            keysetConfig = FileKeysetConfig(EncryptedFile.FileEncryptionScheme.AES256_GCM_HKDF_4KB),
            getMasterKey = { MasterKey.getOrCreate() },
        ).build()
        val expected = "expected file content"

        underTest.openFileOutput().bufferedWriter().use {
            it.write(expected)
        }

        underTest.openFileInput().bufferedReader().use {
            it.readText() shouldBe expected
        }
    }

    @Test
    fun `encrypt shared preferences`() = runTest {
        val underTest = EncryptedSharedPreferences.Builder(
            fileName = "test_prefs",
            getMasterKey = { MasterKey.getOrCreate() },
            context = context,
            prefKeyEncryptionScheme = EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            prefValueEncryptionScheme = EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
        ).build()
        val stringKey = "key1"
        val expectedString = "expected string"
        val intKey = "key2"
        val expectedInt = Random.nextInt()
        val booleanKey = "key3"
        val expectedBoolean = Random.nextBoolean()

        underTest.edit {
            putString(stringKey, expectedString)
            putInt(intKey, expectedInt)
            putBoolean(booleanKey, expectedBoolean)
        }

        underTest.getString(stringKey, null) shouldBe expectedString
        underTest.getInt(intKey, 0) shouldBe expectedInt
        underTest.getBoolean(booleanKey, false) shouldBe expectedBoolean
        underTest.getAll() shouldHaveSize 3
    }

    @Test
    @Suppress("DEPRECATION")
    fun `adapt encrypted shared preferences to shared preferences`() = runTest {
        val underTest: SharedPreferences = EncryptedSharedPreferences.Builder(
            fileName = "test_prefs",
            getMasterKey = { MasterKey.getOrCreate() },
            context = context,
            prefKeyEncryptionScheme = EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            prefValueEncryptionScheme = EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
        ).build().adaptToSharedPreferences()

        val stringKey = "key1"
        val expectedString = "expected string"
        val intKey = "key2"
        val expectedInt = Random.nextInt()
        val booleanKey = "key3"
        val expectedBoolean = Random.nextBoolean()

        underTest.edit()
            .putString(stringKey, expectedString)
            .putInt(intKey, expectedInt)
            .putBoolean(booleanKey, expectedBoolean)
            .commit()

        underTest.getString(stringKey, null) shouldBe expectedString
        underTest.getInt(intKey, 0) shouldBe expectedInt
        underTest.getBoolean(booleanKey, false) shouldBe expectedBoolean
        underTest.all shouldHaveSize 3
    }
}

private fun Context.createRandomFile() = File(filesDir, "test-${UUID.randomUUID()}")
