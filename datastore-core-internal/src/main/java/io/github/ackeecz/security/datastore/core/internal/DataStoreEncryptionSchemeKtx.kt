package io.github.ackeecz.security.datastore.core.internal

import com.google.crypto.tink.KeyTemplate
import com.google.crypto.tink.streamingaead.AesGcmHkdfStreamingKeyManager
import io.github.ackeecz.security.datastore.core.DataStoreEncryptionScheme

internal val DataStoreEncryptionScheme.keyTemplate: KeyTemplate
    get() = when (this) {
        DataStoreEncryptionScheme.AES256_GCM_HKDF_4KB -> AesGcmHkdfStreamingKeyManager.aes256GcmHkdf4KBTemplate()
        DataStoreEncryptionScheme.AES256_GCM_HKDF_1MB -> AesGcmHkdfStreamingKeyManager.aes256GcmHkdf1MBTemplate()
    }
