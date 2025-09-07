package io.github.ackeecz.guardian.core.internal

import android.content.Context
import com.google.crypto.tink.KeyTemplate
import com.google.crypto.tink.KeysetHandle
import com.google.crypto.tink.LegacyKeysetSerialization
import com.google.crypto.tink.integration.android.AndroidKeystoreKmsClient
import com.google.crypto.tink.integration.android.SharedPrefKeysetReader
import com.google.crypto.tink.proto.AesGcmHkdfStreamingKey
import com.google.crypto.tink.proto.Keyset
import com.google.crypto.tink.shaded.protobuf.ByteString
import com.google.crypto.tink.streamingaead.AesGcmHkdfStreamingKeyManager
import io.kotest.matchers.shouldBe

public fun KeysetHandle.assertAesGcmHkdfEncryptionScheme(
    expectedKeyBitSize: Int,
    expectedCiphertextSegmentBitSize: Int,
) {
    // Asserts AES alg, GCM mode, HKDF. Remaining scheme config is asserted by next parameters.
    this shouldHaveTypeUrlFromTemplate AesGcmHkdfStreamingKeyManager.aes256GcmHkdf4KBTemplate()

    val key = AesGcmHkdfStreamingKey.parseFrom(getFirstKeyDataValue())
    key.keyValue shouldHaveBitSize expectedKeyBitSize
    key shouldUseCiphertextSegmentBitSize expectedCiphertextSegmentBitSize
}

private infix fun AesGcmHkdfStreamingKey.shouldUseCiphertextSegmentBitSize(bitSize: Int) {
    params.ciphertextSegmentSize shouldBe bitSize
}

@Suppress("DEPRECATION")
public infix fun KeysetHandle.shouldHaveTypeUrlFromTemplate(template: KeyTemplate) {
    keysetInfo
        .getKeyInfo(0)
        .typeUrl
        .shouldBe(template.typeUrl)
}

public fun KeysetHandle.getFirstKeyDataValue(): ByteString {
    return getKeyset().getKey(0).keyData.value
}

public fun KeysetHandle.getKeyset(): Keyset {
    return KeysetHandle::class.java
        .declaredMethods
        .find { it.name == "getKeyset" }!!
        .let { getKeysetMethod ->
            getKeysetMethod.isAccessible = true
            getKeysetMethod.invoke(this) as Keyset
        }
}

public fun getKeysetHandle(
    context: Context,
    masterKeyUri: String,
    keysetPrefsName: String,
    keysetAlias: String,
): KeysetHandle {
    val keysetReader = SharedPrefKeysetReader(context, keysetAlias, keysetPrefsName)
    val masterAead = AndroidKeystoreKmsClient().getAead(masterKeyUri)
    return LegacyKeysetSerialization.parseEncryptedKeyset(keysetReader, masterAead, byteArrayOf())
}

public fun TinkPrimitiveProvider.clearFixture() {
    clear()
}
