package io.github.ackeecz.security.datastore.core

import io.github.ackeecz.security.core.MasterKey
import io.github.ackeecz.security.core.internal.ACKEE_SECURITY_KEYSET_PREFS_NAME

// TODO docs
public data class DataStoreCryptoParams(
    val encryptionScheme: DataStoreEncryptionScheme,
    val keysetPrefsName: String = ACKEE_SECURITY_KEYSET_PREFS_NAME,
    val keysetAlias: String = KEYSET_ALIAS,
    val getMasterKey: suspend () -> MasterKey,
) {

    private companion object {

        // TODO Check and adjust namespace when finalized for libraries
        /**
         * Default keyset alias used for encryption of DataStore files. It is used as a key in
         * SharedPreferences key-value pair, where value is the actual keyset data.
         */
        private const val KEYSET_ALIAS = "__io_github_ackeecz_security_datastore_keyset__"
    }
}
