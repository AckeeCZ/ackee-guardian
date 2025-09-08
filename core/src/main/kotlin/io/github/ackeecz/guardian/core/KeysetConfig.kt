package io.github.ackeecz.guardian.core

/**
 * Configuration for managing keysets that is common to most of the encrypted storages.
 *
 * # Keyset caching
 * If the cache is disabled, the keyset is always read from SharedPreferences and decrypted with the
 * [MasterKey] each time you create a new instance of the encrypted object (e.g. EncryptedFile) and is
 * kept in memory only as long as the instance of the encrypted object lives.
 *
 * Caching the keyset is more performant, especially if you back your AndroidKeyStore [MasterKey] by HW StrongBox
 * implementation. Working with key in StrongBox can be significantly slower on some particular
 * devices, so caching the already decrypted keyset can significantly improve performance in such
 * cases. On the other hand, caching the keyset in memory can increase the attack surface of your app,
 * because the key will be kept in the memory for the whole lifetime of the app process. This should
 * not be generally an issue on Android, because the OS takes care of memory isolation between apps,
 * but keeping sensitive data in memory longer than needed can slightly increase the security risk,
 * if the device is compromised. Therefore if you are working with especially sensitive data and
 * don't need the performance optimization, you might want to disable the keyset caching.
 *
 * [KeysetConfig] is tied to a particular keyset identified by a unique combination of [prefsName]
 * and [alias]. For each unique keyset, you can choose whether to cache it in memory or not, controlled
 * by [cacheKeyset], but your choice needs to be consistent for the specific keyset. For example,
 * if you create an EncryptedFile with a specific keyset and enable caching, you need to also enable
 * caching when you create another EncryptedFile with the same keyset, otherwise exception is thrown.
 * For this reason it is recommended to share a single instance of [KeysetConfig] per particular keyset.
 *
 * @property prefsName Name of the SharedPreferences file where a data encryption keyset is saved.
 * @property alias Alias of the data encryption keyset stored in the SharedPreferences file specified
 * by [prefsName]. Keyset is used to encrypt/decrypt the data. The value of this
 * [alias] must be the same for the single piece of data to ensure successful encryption/decryption,
 * but might be unique for different data to be able to have a unique keyset per different data.
 * If you use a single value for all your data, they will be encrypted with a single key.
 * @property cacheKeyset Whether to cache the keyset in memory for faster access. Default is `false`.
 * For more details see *Keyset caching* section in [KeysetConfig].
 */
public interface KeysetConfig {

    public val prefsName: String
    public val alias: String
    public val cacheKeyset: Boolean
}
