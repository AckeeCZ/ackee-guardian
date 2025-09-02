package io.github.ackeecz.guardian.datastore.core

import io.github.ackeecz.guardian.core.MasterKey
import io.github.ackeecz.guardian.core.internal.ACKEE_GUARDIAN_KEYSET_PREFS_NAME

/**
 * Crypto params used for configuration of encrypted DataStores.
 *
 * @param keysetConfig Configuration for managing keysets used by DataStore.
 * @param getMasterKey Returns [MasterKey] used for encryption/decryption of the keyset specified by
 * [keysetConfig]. [MasterKey] must always be the same for a single keyset.
 */
public class DataStoreCryptoParams(
    public val keysetConfig: DataStoreKeysetConfig,
    public val getMasterKey: suspend () -> MasterKey,
) {

    @Deprecated(
        message = "Use keysetConfig.encryptionScheme instead",
        replaceWith = ReplaceWith("keysetConfig.encryptionScheme"),
        level = DeprecationLevel.WARNING,
    )
    public val encryptionScheme: DataStoreEncryptionScheme get() = keysetConfig.encryptionScheme

    @Deprecated(
        message = "Use keysetConfig.prefsName instead",
        replaceWith = ReplaceWith("keysetConfig.prefsName"),
        level = DeprecationLevel.WARNING,
    )
    public val keysetPrefsName: String get() = keysetConfig.prefsName

    @Deprecated(
        message = "Use keysetConfig.alias instead",
        replaceWith = ReplaceWith("keysetConfig.alias"),
        level = DeprecationLevel.WARNING,
    )
    public val keysetAlias: String get() = keysetConfig.alias

    /**
     * @param encryptionScheme Encryption scheme used to encrypt a DataStore file.
     * @param keysetPrefsName Name of the SharedPreferences file where a data encryption keyset is saved.
     * @param keysetAlias Alias of the data encryption keyset stored in the SharedPreferences file specified
     * by [keysetPrefsName]. Keyset is used to encrypt/decrypt the DataStore file. The value of this
     * [keysetAlias] must be the same for the single DataStore file to ensure successful encryption/decryption,
     * but might be unique per DataStore file to be able to have a unique keyset per DataStore file.
     * If you use a single value for all your DataStore files, they will be encrypted with a single key.
     * @param getMasterKey Returns [MasterKey] used for encryption/decryption of the keyset specified by
     * [keysetAlias]. [MasterKey] must always be the same for a single keyset.
     */
    @Deprecated(
        message = "Use constructor with keysetConfig instead",
        level = DeprecationLevel.WARNING,
    )
    public constructor(
        encryptionScheme: DataStoreEncryptionScheme,
        keysetPrefsName: String = ACKEE_GUARDIAN_KEYSET_PREFS_NAME,
        keysetAlias: String = DataStoreKeysetConfig.KEYSET_ALIAS,
        getMasterKey: suspend () -> MasterKey,
    ) : this(
        keysetConfig = DataStoreKeysetConfig(
            encryptionScheme = encryptionScheme,
            prefsName = keysetPrefsName,
            alias = keysetAlias,
        ),
        getMasterKey = getMasterKey,
    )
}
