package io.github.ackeecz.guardian.datastore.core

/**
 * The encryption scheme to encrypt DataStore files.
 */
public enum class DataStoreEncryptionScheme {

    /**
     * This encryption scheme is generally better suited for smaller files.
     * If you expect your file to be in a range of a few KBs up to 1 MB or a few MBs max, then
     * this is probably the best option.
     */
    AES256_GCM_HKDF_4KB,

    /**
     * This encryption scheme is generally better suited for bigger files.
     * If you expect your file to have a size of a few or a lot of MBs, then this is probably
     * the best option.
     */
    AES256_GCM_HKDF_1MB,
}
