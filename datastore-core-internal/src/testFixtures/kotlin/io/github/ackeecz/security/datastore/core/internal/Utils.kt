package io.github.ackeecz.security.datastore.core.internal

import io.github.ackeecz.security.core.MasterKey
import io.github.ackeecz.security.datastore.core.DataStoreCryptoParams
import io.github.ackeecz.security.datastore.core.DataStoreEncryptionScheme

fun DataStoreCryptoParams.copy(
    encryptionScheme: DataStoreEncryptionScheme = this.encryptionScheme,
    keysetPrefsName: String = this.keysetPrefsName,
    keysetAlias: String = this.keysetAlias,
    getMasterKey: suspend () -> MasterKey = this.getMasterKey,
): DataStoreCryptoParams {
    return DataStoreCryptoParams(
        encryptionScheme = encryptionScheme,
        keysetPrefsName = keysetPrefsName,
        keysetAlias = keysetAlias,
        getMasterKey = getMasterKey,
    )
}
