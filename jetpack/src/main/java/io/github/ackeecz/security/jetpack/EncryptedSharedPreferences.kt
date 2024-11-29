/*
 * Copyright 2019 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.github.ackeecz.security.jetpack

import android.content.Context
import android.content.SharedPreferences
import androidx.annotation.VisibleForTesting
import androidx.collection.arraySetOf
import com.google.crypto.tink.Aead
import com.google.crypto.tink.DeterministicAead
import com.google.crypto.tink.KeyTemplate
import com.google.crypto.tink.aead.AeadConfig
import com.google.crypto.tink.aead.AesGcmKeyManager
import com.google.crypto.tink.daead.AesSivKeyManager
import com.google.crypto.tink.daead.DeterministicAeadConfig
import com.google.crypto.tink.integration.android.AndroidKeysetManager
import com.google.crypto.tink.subtle.Base64
import io.github.ackeecz.security.core.MasterKey
import io.github.ackeecz.security.core.internal.Base64Value
import io.github.ackeecz.security.core.internal.SynchronizedDataHolder
import io.github.ackeecz.security.core.internal.WeakReferenceFactory
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import java.lang.ClassCastException
import java.lang.ref.WeakReference
import java.nio.ByteBuffer
import java.security.GeneralSecurityException
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.CopyOnWriteArraySet
import java.util.concurrent.atomic.AtomicBoolean

private const val KEY_KEYSET_ALIAS = "__androidx_security_crypto_encrypted_prefs_key_keyset__"
private const val VALUE_KEYSET_ALIAS = "__androidx_security_crypto_encrypted_prefs_value_keyset__"

/**
 * An implementation of shared preferences that encrypts keys and values. Unlike
 * EncryptedSharedPreferences from Jetpack Security library, this interface does not extend
 * [SharedPreferences], because original implementation from Jetpack can block a thread a lot, even
 * in situations where you do not expect it with a regular [SharedPreferences] implementation. To
 * prevent this, Ackee [EncryptedSharedPreferences] makes necessary methods suspend to avoid thread
 * blockage. Extension of [SharedPreferences] is avoided mainly to discourage its usage when having
 * an instance of [EncryptedSharedPreferences] and to encourage usage of non-blocking methods. If
 * you really do need [SharedPreferences] interface and relied on it using Jetpack Security's
 * EncryptedSharedPreferences, you can convert Ackee [EncryptedSharedPreferences] to
 * [SharedPreferences] using [adaptToSharedPreferences] method.
 *
 * **WARNING**: The preference file should not be backed up with Auto Backup. When restoring the
 * file it is likely the key used to encrypt it will no longer be present. You should exclude all
 * [EncryptedSharedPreferences] from backup using
 * [backup rules](https://developer.android.com/guide/topics/data/autobackup#IncludingFiles).
 *
 * Basic use of the class:
 *```
 * val encryptedSharedPreferences = EncryptedSharedPreferences.create(
 *     fileName = "secret_shared_prefs",
 *     getMasterKey = { MasterKey.getOrCreate() },
 *     context = context,
 *     prefKeyEncryptionScheme = EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
 *     prefValueEncryptionScheme = EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
 * )
 * // Use EncryptedSharedPreferences and Editor as you would normally use SharedPreferences
 * encryptedSharedPreferences.edit {
 *     putString("secret_key", "secret_value")
 * }
 *```
 */
public interface EncryptedSharedPreferences {

    /**
     * Retrieve all values from the preferences.
     *
     * Note that you *must not* modify the collection returned by this method, or alter any of its
     * contents. The consistency of your stored data is not guaranteed if you do.
     *
     * @return Returns a map containing a list of pairs key/value representing the preferences.
     *
     * @throws NullPointerException
     */
    public suspend fun getAll(): Map<String?, *>

    /**
     * Retrieve a [String] value from the preferences.
     *
     * @param key The name of the preference to retrieve.
     * @param defValue Value to return if this preference does not exist.
     *
     * @return Returns the preference value if it exists, or [defValue].  Throws
     * [ClassCastException] if there is a preference with this name that is not
     * a [String].
     *
     * @throws ClassCastException
     */
    public suspend fun getString(key: String?, defValue: String?): String?

    /**
     * Retrieve a set of [String] values from the preferences.
     *
     * Note that you *must not* modify the set instance returned by this call. The consistency of
     * the stored data is not guaranteed if you do, nor is your ability to modify the instance at all.
     *
     * @param key The name of the preference to retrieve.
     * @param defValues Values to return if this preference does not exist.
     *
     * @return Returns the preference values if they exist, or [defValues].
     * Throws [ClassCastException] if there is a preference with this name that is not a [Set].
     *
     * @throws ClassCastException
     */
    public suspend fun getStringSet(key: String?, defValues: Set<String?>?): Set<String?>?

    /**
     * Retrieve an [Int] value from the preferences.
     *
     * @param key The name of the preference to retrieve.
     * @param defValue Value to return if this preference does not exist.
     *
     * @return Returns the preference value if it exists, or [defValue]. Throws [ClassCastException]
     * if there is a preference with this name that is not an [Int].
     *
     * @throws ClassCastException
     */
    public suspend fun getInt(key: String?, defValue: Int): Int

    /**
     * Retrieve a [Long] value from the preferences.
     *
     * @param key The name of the preference to retrieve.
     * @param defValue Value to return if this preference does not exist.
     *
     * @return Returns the preference value if it exists, or [defValue]. Throws [ClassCastException]
     * if there is a preference with this name that is not a [Long].
     *
     * @throws ClassCastException
     */
    public suspend fun getLong(key: String?, defValue: Long): Long

    /**
     * Retrieve a float value from the preferences.
     *
     * @param key The name of the preference to retrieve.
     * @param defValue Value to return if this preference does not exist.
     *
     * @return Returns the preference value if it exists, or [defValue]. Throws [ClassCastException]
     * if there is a preference with this name that is not a [Float].
     *
     * @throws ClassCastException
     */
    public suspend fun getFloat(key: String?, defValue: Float): Float

    /**
     * Retrieve a [Boolean] value from the preferences.
     *
     * @param key The name of the preference to retrieve.
     * @param defValue Value to return if this preference does not exist.
     *
     * @return Returns the preference value if it exists, or [defValue]. Throws [ClassCastException]
     * if there is a preference with this name that is not a [Boolean].
     *
     * @throws ClassCastException
     */
    public suspend fun getBoolean(key: String?, defValue: Boolean): Boolean

    /**
     * Checks whether the preferences contains a preference.
     *
     * @param key The name of the preference to check.
     * @return Returns `true` if the preference exists in the preferences,
     * otherwise `false`.
     */
    public suspend fun contains(key: String?): Boolean

    /**
     * Create a new [Editor] for these preferences, through which you can make modifications to the
     * data in the preferences and atomically commit those changes back to the
     * [EncryptedSharedPreferences] object.
     *
     * Note that you *must* call [Editor.commit] or [Editor.apply] to have any changes you perform
     * in the [Editor] actually show up in the [EncryptedSharedPreferences].
     *
     * @return Returns a new instance of the [Editor] interface, allowing
     * you to modify the values in this [EncryptedSharedPreferences] object.
     */
    public fun edit(): Editor

    /**
     * Edits [EncryptedSharedPreferences] using provided [action]. [commit] parameter controls if
     * the transaction should be commited using [Editor.commit] or applied using [Editor.apply].
     * This has the same behaviour as [androidx.core.content.edit].
     */
    @Suppress("KDocUnresolvedReference")
    public suspend fun edit(
        commit: Boolean = false,
        action: suspend Editor.() -> Unit,
    )

    /**
     * Registers a callback to be invoked when a change happens to a preference.
     *
     * **Caution:**
     * The preference manager does not currently store a strong reference to the listener. You must
     * store a strong reference to the listener, or it will be susceptible to garbage collection.
     * We recommend you keep a reference to the listener in the instance data of an object that will
     * exist as long as you need the listener.
     *
     * @param listener The callback that will run.
     * @see [unregisterOnSharedPreferenceChangeListener]
     */
    public fun registerOnSharedPreferenceChangeListener(listener: OnSharedPreferenceChangeListener)

    /**
     * Unregisters a previous callback.
     *
     * @param listener The callback that should be unregistered.
     * @see [registerOnSharedPreferenceChangeListener]
     */
    public fun unregisterOnSharedPreferenceChangeListener(listener: OnSharedPreferenceChangeListener)

    public companion object {

        /**
         * Creates an instance of [EncryptedSharedPreferences]
         *
         * @param context Context of the application.
         * @param fileName The name of the file to open. Can not contain path separators.
         * @param getMasterKey Returns the master key to use. Master key is used to encrypt/decrypt
         * generated data encryption key that is then used to encrypt/decrypt the preferences.
         * @param prefKeyEncryptionScheme The scheme to use for encrypting keys.
         * @param prefValueEncryptionScheme The scheme to use for encrypting values.
         *
         * @return The [EncryptedSharedPreferences] instance that encrypts all data.
         *
         * @throws GeneralSecurityException when a bad [MasterKey] or keyset has been attempted
         * @throws IOException when [fileName] can not be used
         */
        public fun create(
            fileName: String,
            getMasterKey: suspend () -> MasterKey,
            context: Context,
            prefKeyEncryptionScheme: PrefKeyEncryptionScheme,
            prefValueEncryptionScheme: PrefValueEncryptionScheme,
        ): EncryptedSharedPreferences {
            return create(
                fileName = fileName,
                getMasterKey = getMasterKey,
                context = context,
                prefKeyEncryptionScheme = prefKeyEncryptionScheme,
                prefValueEncryptionScheme = prefValueEncryptionScheme,
                weakReferenceFactory = WeakReferenceFactory(),
                defaultDispatcher = Dispatchers.Default,
            )
        }

        @Suppress("LongParameterList")
        @VisibleForTesting
        internal fun create(
            fileName: String,
            getMasterKey: suspend () -> MasterKey,
            context: Context,
            prefKeyEncryptionScheme: PrefKeyEncryptionScheme,
            prefValueEncryptionScheme: PrefValueEncryptionScheme,
            weakReferenceFactory: WeakReferenceFactory,
            defaultDispatcher: CoroutineDispatcher,
        ): EncryptedSharedPreferences {
            return EncryptedSharedPreferencesImpl(
                fileName = fileName,
                getMasterKey = getMasterKey,
                context = context,
                prefKeyEncryptionScheme = prefKeyEncryptionScheme,
                prefValueEncryptionScheme = prefValueEncryptionScheme,
                weakReferenceFactory = weakReferenceFactory,
                defaultDispatcher = defaultDispatcher,
            )
        }
    }

    /**
     * The encryption scheme to encrypt [EncryptedSharedPreferences] keys.
     */
    public enum class PrefKeyEncryptionScheme {

        /**
         * Pref keys are encrypted deterministically with AES256-SIV-CMAC (RFC 5297).
         */
        AES256_SIV {

            override val keyTemplate: KeyTemplate = AesSivKeyManager.aes256SivTemplate()
        };

        // Property declared outside of the primary constructor like this intentionally to keep
        // the property internal and not public
        internal abstract val keyTemplate: KeyTemplate
    }

    /**
     * The encryption scheme to encrypt [EncryptedSharedPreferences] values.
     */
    public enum class PrefValueEncryptionScheme {

        /**
         * Pref values are encrypted with AES256-GCM. The associated data is the encrypted pref key.
         */
        AES256_GCM {

            override val keyTemplate: KeyTemplate = AesGcmKeyManager.aes256GcmTemplate()
        };

        // Property declared outside of the primary constructor like this intentionally to keep
        // the property internal and not public
        internal abstract val keyTemplate: KeyTemplate
    }

    /**
     * Interface used for modifying values in a [EncryptedSharedPreferences] object. All changes
     * you make in an editor are batched, and not copied back to the original
     * [EncryptedSharedPreferences] until you call [commit] or [apply].
     */
    public interface Editor {

        /**
         * Set a [String] value in the preferences editor, to be written back once [commit] or
         * [apply] are called.
         *
         * @param key The name of the preference to modify.
         * @param value The new value for the preference.  Passing `null` for this argument is
         * equivalent to calling [remove] with this key.
         *
         * @return Returns a reference to the same [Editor] object, so you can chain put calls
         * together.
         */
        public suspend fun putString(key: String?, value: String?): Editor

        /**
         * Set a set of [String] values in the preferences editor, to be written back once [commit]
         * or [apply] is called.
         *
         * @param key The name of the preference to modify.
         * @param values The set of new values for the preference.  Passing `null` for this argument
         * is equivalent to calling [remove] with this key.
         *
         * @return Returns a reference to the same [Editor] object, so you can chain put calls
         * together.
         */
        public suspend fun putStringSet(key: String?, values: Set<String?>?): Editor

        /**
         * Set an [Int] value in the preferences editor, to be written back once [commit] or [apply]
         * are called.
         *
         * @param key The name of the preference to modify.
         * @param value The new value for the preference.
         *
         * @return Returns a reference to the same [Editor] object, so you can chain put calls
         * together.
         */
        public suspend fun putInt(key: String?, value: Int): Editor

        /**
         * Set a [Long] value in the preferences editor, to be written back once [commit] or [apply]
         * are called.
         *
         * @param key The name of the preference to modify.
         * @param value The new value for the preference.
         *
         * @return Returns a reference to the same [Editor] object, so you can chain put calls
         * together.
         */
        public suspend fun putLong(key: String?, value: Long): Editor

        /**
         * Set a [Float] value in the preferences editor, to be written back once [commit] or
         * [apply] are called.
         *
         * @param key The name of the preference to modify.
         * @param value The new value for the preference.
         *
         * @return Returns a reference to the same [Editor] object, so you can chain put calls
         * together.
         */
        public suspend fun putFloat(key: String?, value: Float): Editor

        /**
         * Set a [Boolean] value in the preferences editor, to be written back once [commit] or
         * [apply] are called.
         *
         * @param key The name of the preference to modify.
         * @param value The new value for the preference.
         *
         * @return Returns a reference to the same [Editor] object, so you can chain put calls
         * together.
         */
        public suspend fun putBoolean(key: String?, value: Boolean): Editor

        /**
         * Mark in the editor that a preference value should be removed, which will be done in the
         * actual preferences once [commit] or [apply] are called.
         *
         * @param key The name of the preference to remove.
         *
         * @return Returns a reference to the same [Editor] object, so you can chain put calls
         * together.
         */
        public suspend fun remove(key: String?): Editor

        /**
         * Mark in the editor to remove *all* values from the preferences. Once commit is called,
         * the only remaining preferences will be any that you have defined in this editor.
         *
         * Note that when committing back to the preferences, the [clear] is done first, regardless
         * of whether you called [clear] before or after put methods on this editor.
         *
         * @return Returns a reference to the same [Editor] object, so you can chain put calls
         * together.
         */
        public fun clear(): Editor

        /**
         * Commit your preferences changes back from this [Editor] to the [EncryptedSharedPreferences]
         * object it is editing. This atomically performs the requested modifications, replacing
         * whatever is currently in the [EncryptedSharedPreferences].
         *
         * Note that when two editors are modifying preferences at the same time, the last one to
         * call commit wins.
         *
         * If you don't care about the return value consider using [apply] instead.
         *
         * Even though this mirrors the standard [SharedPreferences.Editor.commit] which blocks the
         * caller's thread and this method internally calls [SharedPreferences.Editor.commit] as well,
         * it does not block the caller's thread, because it is a suspend function, which can't block
         * caller's thread by a definition.
         *
         * @return Returns `true` if the new values were successfully written to persistent storage.
         */
        public suspend fun commit(): Boolean

        /**
         * Commit your preferences changes back from this [Editor] to the [EncryptedSharedPreferences]
         * object it is editing. This atomically performs the requested modifications, replacing
         * whatever is currently in the [EncryptedSharedPreferences].
         *
         * Note that when two editors are modifying preferences at the same time, the last one to
         * call apply wins.
         *
         * Unlike [commit], which writes its preferences out to persistent storage synchronously,
         * [apply] commits its changes to the in-memory delegated [SharedPreferences] immediately but
         * starts an asynchronous commit to disk and you won't be notified of any failures. If
         * another editor on this [EncryptedSharedPreferences] does a regular [commit] while an
         * [apply] is still outstanding, the [commit] will block until all async commits are
         * completed as well as the [commit] itself.
         *
         * As [SharedPreferences] instances are singletons within a process, it's safe to replace
         * any instance of [commit] with [apply] if you were already ignoring the return value.
         *
         * You don't need to worry about Android component lifecycles and their interaction with
         * [apply] writing to disk. The framework makes sure in-flight disk writes from [apply]
         * complete before switching states.
         */
        public suspend fun apply()
    }

    /**
     * Interface definition for a callback to be invoked when a shared preference is changed.
     */
    public fun interface OnSharedPreferenceChangeListener {

        /**
         * Called when a shared preference is changed, added, or removed. This may be called even
         * if a preference is set to its existing value.
         *
         * This callback will be run on your main thread.
         *
         * @param sharedPreferences The [EncryptedSharedPreferences] that received the change.
         * @param key The key of the preference that was changed, added, or removed.
         */
        public fun onSharedPreferenceChanged(sharedPreferences: EncryptedSharedPreferences, key: String?)
    }
}

@Suppress("LongParameterList")
private class EncryptedSharedPreferencesImpl(
    context: Context,
    private val fileName: String,
    private val getMasterKey: suspend () -> MasterKey,
    private val prefKeyEncryptionScheme: EncryptedSharedPreferences.PrefKeyEncryptionScheme,
    private val prefValueEncryptionScheme: EncryptedSharedPreferences.PrefValueEncryptionScheme,
    private val weakReferenceFactory: WeakReferenceFactory,
    private val defaultDispatcher: CoroutineDispatcher,
) : EncryptedSharedPreferences {

    private val applicationContext = context.applicationContext
    private val sharedPreferences = context.getSharedPreferences(fileName, Context.MODE_PRIVATE)
    private val cryptoObjectsHolder = CryptoObjectsHolder()
    private val listeners = CopyOnWriteArrayList<WeakReference<EncryptedSharedPreferences.OnSharedPreferenceChangeListener>>()

    // Switching context to minimize switching back and forth in the "child" suspend functions
    override suspend fun getAll(): Map<String?, *> = withContext(defaultDispatcher) {
        getAllEncryptedKeys().associate { encryptedKey ->
            decryptKey(encryptedKey)?.value to getDecryptedValue(encryptedKey)
        }
    }

    fun getAllEncryptedKeys(): List<PreferenceKey.Encrypted> {
        return sharedPreferences.all.keys
            .map { it.toEncryptedKey() }
            .filterNot { isReservedKey(it) }
    }

    private fun isReservedKey(key: PreferenceKey?): Boolean {
        val keyValue = key?.rawValue
        return KEY_KEYSET_ALIAS == keyValue || VALUE_KEYSET_ALIAS == keyValue
    }

    suspend fun decryptKey(encryptedKey: PreferenceKey.Encrypted): PreferenceKey.Decrypted? {
        return withContext(defaultDispatcher) {
            try {
                val keyDeterministicAead = cryptoObjectsHolder.getOrCreate().keyDeterministicAead
                val decryptedKeyBytes = keyDeterministicAead.decryptDeterministically(
                    Base64.decode(encryptedKey.value.value, Base64.DEFAULT),
                    fileName.toByteArray(),
                )
                String(decryptedKeyBytes, Charsets.UTF_8)
                    .takeUnless { it == NULL_VALUE }
                    ?.let { PreferenceKey.Decrypted(it) }
            } catch (ex: GeneralSecurityException) {
                throw SecurityException("Could not decrypt key. " + ex.message, ex)
            }
        }
    }

    private suspend fun getDecryptedValue(decryptedKey: PreferenceKey.Decrypted?): Any? {
        return getDecryptedValue(encryptKey(decryptedKey))
    }

    private suspend fun getDecryptedValue(encryptedKey: PreferenceKey.Encrypted): Any? {
        throwIfReservedKey(encryptedKey)
        try {
            val encryptedValue = sharedPreferences.getString(encryptedKey.value.value, null)
                ?.let(::Base64Value)
                ?: return null

            val decryptedValue = decryptValue(encryptedKey = encryptedKey, encryptedValue = encryptedValue)
            val buffer = ByteBuffer.wrap(decryptedValue).also { it.position(0) }
            return buffer.decodeValue()
        } catch (ex: GeneralSecurityException) {
            throw SecurityException("Could not decrypt value. ${ex.message}", ex)
        }
    }

    fun throwIfReservedKey(key: PreferenceKey?) {
        if (isReservedKey(key)) {
            throw SecurityException("${key?.rawValue} is a reserved key for the encryption keyset.")
        }
    }

    suspend fun encryptKey(key: PreferenceKey.Decrypted?): PreferenceKey.Encrypted = withContext(defaultDispatcher) {
        throwIfReservedKey(key)
        val resolvedKey = key?.value ?: NULL_VALUE
        try {
            val keyDeterministicAead = cryptoObjectsHolder.getOrCreate().keyDeterministicAead
            val encryptedKeyBytes = keyDeterministicAead.encryptDeterministically(
                resolvedKey.toByteArray(Charsets.UTF_8),
                fileName.toByteArray(),
            )
            val base64KeyValue = Base64Value(Base64.encode(encryptedKeyBytes))
            PreferenceKey.Encrypted(base64KeyValue)
        } catch (ex: GeneralSecurityException) {
            throw SecurityException("Could not encrypt key. ${ex.message}", ex)
        }
    }

    private suspend fun decryptValue(
        encryptedKey: PreferenceKey.Encrypted,
        encryptedValue: Base64Value,
    ): ByteArray = withContext(defaultDispatcher) {
        val cipherText = Base64.decode(encryptedValue.value, Base64.DEFAULT)
        val valueAead = cryptoObjectsHolder.getOrCreate().valueAead
        valueAead.decrypt(cipherText, getValueEncryptionAssociatedData(encryptedKey))
    }

    private fun getValueEncryptionAssociatedData(encryptedKey: PreferenceKey.Encrypted): ByteArray {
        return encryptedKey.value.value.toByteArray(Charsets.UTF_8)
    }

    private fun ByteBuffer.decodeValue(): Any? {
        return when (decodeEncryptedType()) {
            EncryptedType.STRING -> decodeStringValue()
            EncryptedType.INT -> getInt()
            EncryptedType.LONG -> getLong()
            EncryptedType.FLOAT -> getFloat()
            EncryptedType.BOOLEAN -> get() != 0.toByte()
            EncryptedType.STRING_SET -> decodeStringSet()
        }
    }

    private fun ByteBuffer.decodeEncryptedType(): EncryptedType {
        val typeId = getInt()
        return EncryptedType.fromId(typeId) ?: throw SecurityException("Unknown type ID for encrypted pref value: $typeId")
    }

    private fun ByteBuffer.decodeStringValue(): String? {
        val stringLength = getInt()
        val stringSlice = slice()
        limit(stringLength)
        return Charsets.UTF_8.decode(stringSlice)
            .toString()
            .takeUnless { it == NULL_VALUE }
    }

    private fun ByteBuffer.decodeStringSet(): Set<String?>? {
        val stringSet = arraySetOf<String?>()
        while (hasRemaining()) {
            val subStringLength = getInt()
            val subStringSlice = slice()
            subStringSlice.limit(subStringLength)
            position(position() + subStringLength)
            val decodedItem = Charsets.UTF_8.decode(subStringSlice)
                .toString()
                .takeUnless { it == NULL_ITEM_VALUE }
            stringSet.add(decodedItem)
        }
        return if (stringSet.firstOrNull() == NULL_VALUE) null else stringSet
    }

    override suspend fun getString(key: String?, defValue: String?): String? {
        return getValue(key, defValue)
    }

    private suspend inline fun <reified T : Any?> getValue(key: String?, defValue: T): T {
        return when (val value = getDecryptedValue(key.toDecryptedKey())) {
            null -> defValue
            is T -> value
            else -> throw ClassCastException("Expected ${T::class.simpleName} for key $key but was ${value::class.simpleName}")
        }
    }

    override suspend fun getStringSet(key: String?, defValues: Set<String?>?): Set<String?>? {
        return getValue(key, defValues)
    }

    override suspend fun getInt(key: String?, defValue: Int): Int {
        return getValue(key, defValue)
    }

    override suspend fun getLong(key: String?, defValue: Long): Long {
        return getValue(key, defValue)
    }

    override suspend fun getFloat(key: String?, defValue: Float): Float {
        return getValue(key, defValue)
    }

    override suspend fun getBoolean(key: String?, defValue: Boolean): Boolean {
        return getValue(key, defValue)
    }

    override suspend fun contains(key: String?): Boolean {
        val decryptedKey = key.toDecryptedKey()
        throwIfReservedKey(decryptedKey)
        return sharedPreferences.contains(encryptKey(decryptedKey).value.value)
    }

    override fun edit(): EncryptedSharedPreferences.Editor {
        return Editor(this, sharedPreferences.edit(), defaultDispatcher)
    }

    override suspend fun edit(
        commit: Boolean,
        action: suspend EncryptedSharedPreferences.Editor.() -> Unit,
    ): Unit = withContext(defaultDispatcher) { // Switching context to minimize switching back and forth in the "child" suspend functions
        with(edit()) {
            action()
            if (commit) {
                commit()
            } else {
                apply()
            }
        }
    }

    override fun registerOnSharedPreferenceChangeListener(listener: EncryptedSharedPreferences.OnSharedPreferenceChangeListener) {
        listeners.add(weakReferenceFactory.create(listener))
    }

    override fun unregisterOnSharedPreferenceChangeListener(listener: EncryptedSharedPreferences.OnSharedPreferenceChangeListener) {
        listeners.removeAll { it.get() == listener }
    }

    suspend fun encryptKeyValuePair(
        key: PreferenceKey.Decrypted?,
        value: ByteArray?,
    ): Pair<PreferenceKey.Encrypted, Base64Value> = withContext(defaultDispatcher) {
        val encryptedKey = encryptKey(key)
        val associatedData = getValueEncryptionAssociatedData(encryptedKey)
        val valueAead = cryptoObjectsHolder.getOrCreate().valueAead
        val encryptedValue = Base64Value(Base64.encode(valueAead.encrypt(value, associatedData)))
        encryptedKey to encryptedValue
    }

    private companion object {

        private const val NULL_VALUE = "__NULL__"
        private const val NULL_ITEM_VALUE = "__NULL_ITEM__"
    }

    /**
     * Internal enum to encode the type of encrypted data.
     */
    private enum class EncryptedType(val id: Int) {

        STRING(id = 0),
        STRING_SET(id = 1),
        INT(id = 2),
        LONG(id = 3),
        FLOAT(id = 4),
        BOOLEAN(id = 5);

        companion object {

            fun fromId(id: Int) = entries.find { it.id == id }
        }
    }

    private inner class CryptoObjectsHolder : SynchronizedDataHolder<CryptoObjects>(defaultDispatcher) {

        override suspend fun createSynchronizedData(): CryptoObjects {
            DeterministicAeadConfig.register()
            AeadConfig.register()

            val masterKey = getMasterKey()
            val daeadKeysetHandle = AndroidKeysetManager.Builder()
                .withKeyTemplate(prefKeyEncryptionScheme.keyTemplate)
                .withSharedPref(applicationContext, KEY_KEYSET_ALIAS, fileName)
                .withMasterKeyUri(masterKey.keyStoreUri)
                .build()
                .keysetHandle
            val aeadKeysetHandle = AndroidKeysetManager.Builder()
                .withKeyTemplate(prefValueEncryptionScheme.keyTemplate)
                .withSharedPref(applicationContext, VALUE_KEYSET_ALIAS, fileName)
                .withMasterKeyUri(masterKey.keyStoreUri)
                .build()
                .keysetHandle

            val keyDeterministicAead = daeadKeysetHandle.getPrimitive(DeterministicAead::class.java)
            val valueAead = aeadKeysetHandle.getPrimitive(Aead::class.java)

            return CryptoObjects(keyDeterministicAead, valueAead)
        }
    }

    private data class CryptoObjects(
        val keyDeterministicAead: DeterministicAead,
        val valueAead: Aead,
    )

    private class Editor(
        private val encryptedSharedPreferences: EncryptedSharedPreferencesImpl,
        private val editor: SharedPreferences.Editor,
        private val defaultDispatcher: CoroutineDispatcher,
    ) : EncryptedSharedPreferences.Editor {

        private val keysChanged = CopyOnWriteArraySet<PreferenceKey.Decrypted?>()
        private val clearRequested = AtomicBoolean(false)

        override suspend fun putString(key: String?, value: String?): EncryptedSharedPreferences.Editor {
            val stringBytes = (value ?: NULL_VALUE).toByteArray(Charsets.UTF_8)
            val stringByteLength = stringBytes.size
            val buffer = ByteBuffer.allocate(Integer.BYTES + Integer.BYTES + stringByteLength)
            buffer.putInt(EncryptedType.STRING.id)
            buffer.putInt(stringByteLength)
            buffer.put(stringBytes)
            putEncryptedObject(key.toDecryptedKey(), buffer.array())
            return this
        }

        private suspend fun putEncryptedObject(key: PreferenceKey.Decrypted?, value: ByteArray?) {
            encryptedSharedPreferences.throwIfReservedKey(key)
            try {
                val encryptedPair = encryptedSharedPreferences.encryptKeyValuePair(key, value)
                editor.putString(encryptedPair.first.value.value, encryptedPair.second.value)
                keysChanged.add(key)
            } catch (ex: GeneralSecurityException) {
                throw SecurityException("Could not encrypt data: ${ex.message}", ex)
            }
        }

        override suspend fun putStringSet(
            key: String?,
            values: Set<String?>?,
        ): EncryptedSharedPreferences.Editor {
            val resolvedValues = values ?: mutableSetOf<String>().also { it.add(NULL_VALUE) }
            val byteValues = ArrayList<ByteArray>(resolvedValues.size)
            var totalBytes = resolvedValues.size * Integer.BYTES // Size for each item size
            resolvedValues.forEach { value ->
                val byteValue = (value ?: NULL_ITEM_VALUE).toByteArray(Charsets.UTF_8)
                byteValues.add(byteValue)
                totalBytes += byteValue.size
            }
            totalBytes += Integer.BYTES // size for type
            val buffer = ByteBuffer.allocate(totalBytes)
            buffer.putInt(EncryptedType.STRING_SET.id)
            byteValues.forEach { bytes ->
                buffer.putInt(bytes.size)
                buffer.put(bytes)
            }
            putEncryptedObject(key.toDecryptedKey(), buffer.array())
            return this
        }

        override suspend fun putInt(key: String?, value: Int): EncryptedSharedPreferences.Editor {
            val buffer = ByteBuffer.allocate(Integer.BYTES + Integer.BYTES)
            buffer.putInt(EncryptedType.INT.id)
            buffer.putInt(value)
            putEncryptedObject(key.toDecryptedKey(), buffer.array())
            return this
        }

        override suspend fun putLong(key: String?, value: Long): EncryptedSharedPreferences.Editor {
            val buffer = ByteBuffer.allocate(Integer.BYTES + java.lang.Long.BYTES)
            buffer.putInt(EncryptedType.LONG.id)
            buffer.putLong(value)
            putEncryptedObject(key.toDecryptedKey(), buffer.array())
            return this
        }

        override suspend fun putFloat(key: String?, value: Float): EncryptedSharedPreferences.Editor {
            val buffer = ByteBuffer.allocate(Integer.BYTES + java.lang.Float.BYTES)
            buffer.putInt(EncryptedType.FLOAT.id)
            buffer.putFloat(value)
            putEncryptedObject(key.toDecryptedKey(), buffer.array())
            return this
        }

        override suspend fun putBoolean(key: String?, value: Boolean): EncryptedSharedPreferences.Editor {
            val buffer = ByteBuffer.allocate(Integer.BYTES + java.lang.Byte.BYTES)
            buffer.putInt(EncryptedType.BOOLEAN.id)
            buffer.put(if (value) 1.toByte() else 0.toByte())
            putEncryptedObject(key.toDecryptedKey(), buffer.array())
            return this
        }

        override suspend fun remove(key: String?): EncryptedSharedPreferences.Editor {
            val decryptedKey = key.toDecryptedKey()
            encryptedSharedPreferences.throwIfReservedKey(decryptedKey)
            val encryptedKey = encryptedSharedPreferences.encryptKey(decryptedKey)
            editor.remove(encryptedKey.value.value)
            updateChangedKeysForRemoveOperation(key, decryptedKey)
            return this
        }

        /**
         * Adds [decryptedKey] to [keysChanged] collection, if [encryptedSharedPreferences] already
         * contains the [key] and we need to notify listeners for key removal. Removes the [decryptedKey]
         * from [keysChanged], if [encryptedSharedPreferences] does not contain [key] and it means that
         * the client either put and immediately removed the same [key] in the same
         * [EncryptedSharedPreferences.Editor] or just removed non-existing [key] and in these cases
         * we should not notify listeners about these changes.
         */
        private suspend fun updateChangedKeysForRemoveOperation(
            key: String?,
            decryptedKey: PreferenceKey.Decrypted?,
        ) {
            if (encryptedSharedPreferences.contains(key)) {
                keysChanged.add(decryptedKey)
            } else {
                keysChanged.remove(decryptedKey)
            }
        }

        override fun clear(): EncryptedSharedPreferences.Editor {
            // Set the flag to clear on commit or apply, this operation happens first on commit or apply.
            // Cannot use underlying clear operation, because it will remove the keysets and break the editor.
            clearRequested.set(true)
            return this
        }

        override suspend fun commit(): Boolean = withContext(defaultDispatcher) {
            clearKeysIfNeeded()
            try {
                editor.commit()
            } finally {
                notifyListeners()
                keysChanged.clear()
            }
        }

        private suspend fun clearKeysIfNeeded() {
            // Call "clear" first as per the documentation, remove all keys that haven't
            // been modified in this editor.
            if (clearRequested.getAndSet(false)) {
                // Switching context to minimize switching back and forth in the "child" suspend functions
                withContext(defaultDispatcher) {
                    encryptedSharedPreferences.getAllEncryptedKeys().forEach { encryptedKey ->
                        val decryptedKey = encryptedSharedPreferences.decryptKey(encryptedKey)
                        if (!keysChanged.contains(decryptedKey)) {
                            editor.remove(encryptedKey.value.value)
                        }
                    }
                }
            }
        }

        private suspend fun notifyListeners() {
            // Switch context to Main to call listeners on Main thread
            withContext(Dispatchers.Main) {
                val garbageCollectedListeners = mutableListOf<WeakReference<EncryptedSharedPreferences.OnSharedPreferenceChangeListener>>()
                encryptedSharedPreferences.listeners.forEach { weakListener ->
                    val listener = weakListener.get()
                    if (listener == null) {
                        garbageCollectedListeners += weakListener
                    } else {
                        keysChanged.forEach { key ->
                            listener.onSharedPreferenceChanged(encryptedSharedPreferences, key?.value)
                        }
                    }
                }
                // Remove leftover weak references on already garbage collected listeners
                encryptedSharedPreferences.listeners.removeAll { garbageCollectedListeners.contains(it) }
            }
        }

        override suspend fun apply() {
            clearKeysIfNeeded()
            editor.apply()
            notifyListeners()
            keysChanged.clear()
        }
    }
}

private sealed interface PreferenceKey {

    val rawValue: String

    data class Encrypted(val value: Base64Value) : PreferenceKey {

        override val rawValue: String = value.value
    }

    data class Decrypted(val value: String) : PreferenceKey {

        override val rawValue: String = value
    }
}

private fun String?.toDecryptedKey(): PreferenceKey.Decrypted? {
    return this?.let { PreferenceKey.Decrypted(it) }
}

private fun String.toEncryptedKey(): PreferenceKey.Encrypted {
    return PreferenceKey.Encrypted(Base64Value(this))
}
