package io.github.ackeecz.guardian.jetpack

/**
 * Factory function of [FileKeysetConfig], which creates it with default values of [FileKeysetConfig],
 * if possible. Other non-default values are provided by the function itself and can be overridden
 * by the caller if needed.
 */
internal fun createDefaultFileKeysetConfig(
    encryptionScheme: EncryptedFile.FileEncryptionScheme = EncryptedFile.FileEncryptionScheme.AES256_GCM_HKDF_4KB,
): FileKeysetConfig {
    return FileKeysetConfig(encryptionScheme = encryptionScheme)
}

internal fun createFileKeysetConfig(
    encryptionScheme: EncryptedFile.FileEncryptionScheme = EncryptedFile.FileEncryptionScheme.AES256_GCM_HKDF_4KB,
    prefsName: String = "prefsName",
    alias: String = "alias",
    cacheKeyset: Boolean = false,
): FileKeysetConfig {
    return FileKeysetConfig(
        encryptionScheme = encryptionScheme,
        prefsName = prefsName,
        alias = alias,
        cacheKeyset = cacheKeyset,
    )
}
