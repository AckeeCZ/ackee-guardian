package io.github.ackeecz.guardian.datastore.core

import io.github.ackeecz.guardian.core.MasterKey
import io.github.ackeecz.guardian.core.internal.ACKEE_GUARDIAN_KEYSET_PREFS_NAME

/**
 * Crypto params used for configuration of encrypted DataStores.
 *
 * @param encryptionScheme Encryption scheme used to encrypt a DataStore file.
 * @param keysetPrefsName Name of the SharedPreferences file where a data encryption keyset is saved.
 * @param keysetAlias Alias of the data encryption keyset stored in the SharedPreferences file specified
 * by [keysetPrefsName]. Keyset is used to encrypt/decrypt the DataStore file. The value of this
 * [keysetAlias] must be the same for the single DataStore file to ensure successful encryption/decryption,
 * but might be unique per DataStore file to be able to have a unique keyset per DataStore file.
 * If you use a single value for all your DataStore files, they will be encrypted with a single key.
 * @param getMasterKey Returns [MasterKey] used for encryption/decryption of the keyset specified by
 * [keysetAlias]. [MasterKey] must always be the same for a single DataStore file.
 */
public class DataStoreCryptoParams(
    public val encryptionScheme: DataStoreEncryptionScheme,
    public val keysetPrefsName: String = ACKEE_GUARDIAN_KEYSET_PREFS_NAME,
    public val keysetAlias: String = KEYSET_ALIAS,
    public val getMasterKey: suspend () -> MasterKey,
) {

    private companion object {

        /**
         * Default keyset alias used for encryption of DataStore files. It is used as a key in
         * SharedPreferences key-value pair, where value is the actual keyset data.
         */
        private const val KEYSET_ALIAS = "__io_github_ackeecz_guardian_datastore_keyset__"
    }
}
