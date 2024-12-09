package io.github.ackeecz.guardian.core.internal

import com.google.crypto.tink.shaded.protobuf.ByteString
import io.kotest.matchers.shouldBe
import java.nio.Buffer
import java.nio.ByteBuffer

val String.utf8ByteSize: Int
    get() = toByteArray(Charsets.UTF_8).size

/**
 * Returns [ByteBuffer.remaining] number of bytes in this [ByteBuffer] as [ByteArray]
 */
fun ByteBuffer.remainingToByteArray(): ByteArray {
    return ByteArray(remaining()).also { get(it) }
}

infix fun ByteString.shouldHaveBitSize(bitSize: Int) {
    toByteArray()
        .size
        .let { it * 8 }
        .shouldBe(bitSize)
}
