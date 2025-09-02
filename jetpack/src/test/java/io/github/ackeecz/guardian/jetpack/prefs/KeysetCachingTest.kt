package io.github.ackeecz.guardian.jetpack.prefs

import io.github.ackeecz.guardian.core.MasterKey
import io.github.ackeecz.guardian.core.internal.InvalidKeysetConfigException
import io.github.ackeecz.guardian.core.internal.getKeysetGetCallCount
import io.github.ackeecz.guardian.jetpack.EncryptedSharedPreferences
import io.kotest.assertions.throwables.shouldNotThrow
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.test.runTest
import org.junit.Test

internal class KeysetCachingTest : EncryptedSharedPreferencesTest() {

    @Test
    fun `keysets are not cached by default`() = runTest {
        val fileName = "prefs_name"
        suspend fun getKeys() {
            EncryptedSharedPreferences.Builder(
                context = context,
                fileName = fileName,
                getMasterKey = { MasterKey.getOrCreate() },
                prefKeyEncryptionScheme = EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                prefValueEncryptionScheme = EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
            )
                .setCoroutineDispatcher(coroutineRule.testDispatcher)
                .build()
                .forceEncryptionKeysGeneration()
        }

        getKeys()
        getKeys()

        assertKeysetsCallCount(prefsName = fileName, expectedCalls = 2)
    }

    @Suppress("SameParameterValue")
    private fun assertKeysetsCallCount(prefsName: String, expectedCalls: Int) {
        val actualKeyKeysetCallCount = getKeysetGetCallCount(prefsName = prefsName, keysetAlias = KEY_KEYSET_ALIAS)
        actualKeyKeysetCallCount shouldBe expectedCalls
        val actualValueKeysetCallCount = getKeysetGetCallCount(prefsName = prefsName, keysetAlias = VALUE_KEYSET_ALIAS)
        actualValueKeysetCallCount shouldBe expectedCalls
    }

    private fun getKeysetGetCallCount(prefsName: String, keysetAlias: String): Int {
        return context.getKeysetGetCallCount(prefsName = prefsName, keysetAlias = keysetAlias)
    }

    @Test
    fun `keysets are not cached when cache disabled`() = runTest {
        testKeysetsCache(cached = false)
    }

    private suspend fun testKeysetsCache(cached: Boolean) {
        val fileName = "prefs_name"
        suspend fun getKeys() {
            createSut(prefsFileName = fileName, cacheKeysets = cached).forceEncryptionKeysGeneration()
        }

        getKeys()
        getKeys()

        assertKeysetsCallCount(prefsName = fileName, expectedCalls = if (cached) 1 else 2)
    }

    @Test
    fun `keysets are cached when cache enabled`() = runTest {
        testKeysetsCache(cached = true)
    }

    @Test
    fun `fail if caching not enabled for first instance but enabled for second instance of the same prefs`() = runTest {
        testInvalidCacheConfig(firstInstanceCache = false, secondInstanceCache = true)
    }

    private suspend fun testInvalidCacheConfig(firstInstanceCache: Boolean, secondInstanceCache: Boolean) {
        suspend fun getKeys(cacheKeysets: Boolean) {
            createSut(cacheKeysets = cacheKeysets).forceEncryptionKeysGeneration()
        }

        getKeys(firstInstanceCache)
        shouldThrow<InvalidKeysetConfigException> { getKeys(secondInstanceCache) }
    }

    @Test
    fun `fail if caching enabled for first instance but not enabled for second instance of the same prefs`() = runTest {
        testInvalidCacheConfig(firstInstanceCache = true, secondInstanceCache = false)
    }

    @Test
    fun `allow different caching values for different prefs`() = runTest {
        suspend fun getKeys(fileName: String, cacheKeysets: Boolean) {
            createSut(prefsFileName = fileName, cacheKeysets = cacheKeysets).forceEncryptionKeysGeneration()
        }

        getKeys(fileName = "prefs1", cacheKeysets = false)
        shouldNotThrow<InvalidKeysetConfigException> {
            getKeys(fileName = "prefs2", cacheKeysets = true)
        }
    }
}
