package io.github.ackeecz.guardian.datastore.core.internal

import io.github.ackeecz.guardian.core.MasterKey
import io.github.ackeecz.guardian.datastore.core.DataStoreCryptoParams
import io.github.ackeecz.guardian.datastore.core.DataStoreEncryptionScheme

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
