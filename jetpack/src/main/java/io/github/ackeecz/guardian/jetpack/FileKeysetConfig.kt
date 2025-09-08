package io.github.ackeecz.guardian.jetpack

import io.github.ackeecz.guardian.core.KeysetConfig

/**
 * Configuration for managing keysets used by [EncryptedFile].
 */
public class FileKeysetConfig(
    public val encryptionScheme: EncryptedFile.FileEncryptionScheme,
    override val prefsName: String = "__androidx_security_crypto_encrypted_file_pref__",
    override val alias: String = "__androidx_security_crypto_encrypted_file_keyset__",
    override val cacheKeyset: Boolean = false,
) : KeysetConfig

internal fun FileKeysetConfig.copy(
    encryptionScheme: EncryptedFile.FileEncryptionScheme = this.encryptionScheme,
    prefsName: String = this.prefsName,
    alias: String = this.alias,
    cacheKeyset: Boolean = this.cacheKeyset,
): FileKeysetConfig = FileKeysetConfig(
    encryptionScheme = encryptionScheme,
    prefsName = prefsName,
    alias = alias,
    cacheKeyset = cacheKeyset,
)
