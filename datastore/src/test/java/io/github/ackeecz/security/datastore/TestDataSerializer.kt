package io.github.ackeecz.security.datastore

import androidx.datastore.core.CorruptionException
import androidx.datastore.core.Serializer
import com.google.protobuf.InvalidProtocolBufferException
import java.io.InputStream
import java.io.OutputStream

internal object TestDataSerializer : Serializer<TestData> {

    override val defaultValue: TestData = TestData.getDefaultInstance()

    override suspend fun readFrom(input: InputStream): TestData {
        try {
            return TestData.parseFrom(input)
        } catch (exception: InvalidProtocolBufferException) {
            throw CorruptionException("Cannot read proto.", exception)
        }
    }

    override suspend fun writeTo(t: TestData, output: OutputStream) {
        t.writeTo(output)
    }
}
