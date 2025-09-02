package io.github.ackeecz.guardian.jetpack.prefs

import android.content.Context
import android.content.SharedPreferences
import androidx.test.core.app.ApplicationProvider
import io.github.ackeecz.guardian.core.MasterKey
import io.github.ackeecz.guardian.core.internal.AndroidTestWithKeyStore
import io.github.ackeecz.guardian.core.internal.TinkPrimitiveProvider
import io.github.ackeecz.guardian.core.internal.WeakReferenceFactory
import io.github.ackeecz.guardian.core.internal.clearFixture
import io.github.ackeecz.guardian.core.internal.junit.rule.CoroutineRule
import io.github.ackeecz.guardian.jetpack.EncryptedSharedPreferences
import io.github.ackeecz.guardian.jetpack.EncryptedSharedPreferences.PrefKeyEncryptionScheme
import io.github.ackeecz.guardian.jetpack.EncryptedSharedPreferences.PrefValueEncryptionScheme
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import org.junit.After
import org.junit.Rule

internal abstract class EncryptedSharedPreferencesTest : AndroidTestWithKeyStore() {

    @get:Rule
    val coroutineRule: CoroutineRule = CoroutineRule(UnconfinedTestDispatcher())

    protected val context: Context get() = ApplicationProvider.getApplicationContext()

    /**
     * Helps keeping a strong reference to the listener for the whole execution of the test in the
     * suspend method. For more info see [OnSharedPreferenceChangeListener] docs.
     */
    protected val onSharedPreferenceChangeListener = OnSharedPreferenceChangeListener()

    @After
    fun tearDown() {
        TinkPrimitiveProvider.clearFixture()
    }

    @Suppress("LongParameterList")
    protected fun createSut(
        prefsFileName: String = "shared_prefs",
        getMasterKey: suspend () -> MasterKey = { MasterKey.getOrCreate() },
        prefKeyEncryptionScheme: PrefKeyEncryptionScheme = PrefKeyEncryptionScheme.AES256_SIV,
        prefValueEncryptionScheme: PrefValueEncryptionScheme = PrefValueEncryptionScheme.AES256_GCM,
        weakReferenceFactory: WeakReferenceFactory = WeakReferenceFactory(),
        cacheKeysets: Boolean = false,
    ): EncryptedSharedPreferences {
        return EncryptedSharedPreferences.Builder(
            fileName = prefsFileName,
            getMasterKey = getMasterKey,
            context = context,
            prefKeyEncryptionScheme = prefKeyEncryptionScheme,
            prefValueEncryptionScheme = prefValueEncryptionScheme,
        )
            .setCoroutineDispatcher(coroutineRule.testDispatcher)
            .setWeakReferenceFactory(weakReferenceFactory)
            .setCacheKeysets(cacheKeysets)
            .build()
    }

    protected fun getSharedPreferences(prefsFileName: String): SharedPreferences {
        return context.getSharedPreferences(prefsFileName, Context.MODE_PRIVATE)
    }

    protected suspend fun EncryptedSharedPreferences.forceEncryptionKeysGeneration() {
        // Generates encryption keys under the hood because they are generated lazily during first
        // get/put operation and not during instance initialization
        getString("key", null)
    }

    protected enum class ValueType(val typeId: Int) {

        STRING(typeId = 0),
        STRING_SET(typeId = 1),
        INT(typeId = 2),
        LONG(typeId = 3),
        FLOAT(typeId = 4),
        BOOLEAN(typeId = 5),
    }

    internal companion object {

        const val KEY_KEYSET_ALIAS = "__androidx_security_crypto_encrypted_prefs_key_keyset__"
        const val VALUE_KEYSET_ALIAS = "__androidx_security_crypto_encrypted_prefs_value_keyset__"
        val RESERVED_KEYS = listOf(KEY_KEYSET_ALIAS, VALUE_KEYSET_ALIAS)

        const val NULL_VALUE = "__NULL__"
        const val NULL_ITEM_VALUE = "__NULL_ITEM__"
    }

    /**
     * Helper class to test [EncryptedSharedPreferences.OnSharedPreferenceChangeListener] where keeping
     * a strong reference on the listener is needed during the whole test execution. There are issues
     * with using just a local method variable as a strong reference in suspend methods. Seems like
     * there are some optimizations during compilation going on that can mislead garbage collector,
     * which might think that there are actually no strong references on the listener and it can
     * remove the listener, clearing internal weak reference in the [EncryptedSharedPreferences] and
     * fail tests. Using a custom listener class and test instance property with strong reference to this
     * listener class fixes this issue.
     */
    protected class OnSharedPreferenceChangeListener : EncryptedSharedPreferences.OnSharedPreferenceChangeListener {

        lateinit var onChanged: (EncryptedSharedPreferences, String?) -> Unit

        override fun onSharedPreferenceChanged(
            sharedPreferences: EncryptedSharedPreferences,
            key: String?,
        ) {
            onChanged(sharedPreferences, key)
        }
    }
}
