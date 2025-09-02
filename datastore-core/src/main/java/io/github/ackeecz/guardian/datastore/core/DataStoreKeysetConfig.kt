package io.github.ackeecz.guardian.datastore.core

import io.github.ackeecz.guardian.core.KeysetConfig
import io.github.ackeecz.guardian.core.internal.ACKEE_GUARDIAN_KEYSET_PREFS_NAME

/**
 * Configuration for managing keysets used by DataStore.
 */
public class DataStoreKeysetConfig(
    public val encryptionScheme: DataStoreEncryptionScheme,
    override val prefsName: String = ACKEE_GUARDIAN_KEYSET_PREFS_NAME,
    override val alias: String = KEYSET_ALIAS,
    override val cacheKeyset: Boolean = false,
) : KeysetConfig {

    internal companion object {

        /**
         * Default keyset alias used for encryption of DataStore files. It is used as a key in
         * SharedPreferences key-value pair, where value is the actual keyset data.
         */
        internal const val KEYSET_ALIAS = "__io_github_ackeecz_guardian_datastore_keyset__"
    }
}
