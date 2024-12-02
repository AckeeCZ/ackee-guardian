package io.github.ackeecz.security.sample

import android.content.Context
import androidx.datastore.core.CorruptionException
import androidx.datastore.core.DataStore
import androidx.datastore.core.DataStoreFactory
import androidx.datastore.core.Serializer
import androidx.datastore.dataStoreFile
import androidx.test.core.app.ApplicationProvider
import com.google.protobuf.InvalidProtocolBufferException
import io.github.ackeecz.security.core.MasterKey
import io.github.ackeecz.security.datastore.core.DataStoreCryptoParams
import io.github.ackeecz.security.datastore.core.DataStoreEncryptionScheme
import io.github.ackeecz.security.datastore.createEncrypted
import io.github.ackeecz.security.datastore.encryptedDataStore
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Test
import java.io.InputStream
import java.io.OutputStream
import kotlin.random.Random

internal class DataStoreArtifactTest : AndroidTestWithKeyStore() {

    private val context: Context = ApplicationProvider.getApplicationContext()

    private val Context.encryptedDataStore by encryptedDataStore(
        cryptoParams = DataStoreCryptoParams(
            encryptionScheme = DataStoreEncryptionScheme.AES256_GCM_HKDF_4KB,
            getMasterKey = { MasterKey.getOrCreate() },
        ),
        fileName = "test_file",
        serializer = TestDataSerializer,
    )

    @Test
    fun `create encrypted DataStore using factory`() = runTest {
        val underTest = DataStoreFactory.createEncrypted(
            context = context,
            cryptoParams = DataStoreCryptoParams(
                encryptionScheme = DataStoreEncryptionScheme.AES256_GCM_HKDF_4KB,
                getMasterKey = { MasterKey.getOrCreate() },
            ),
            serializer = TestDataSerializer,
            produceFile = { context.dataStoreFile("test_file") },
        )
        testDataStore(underTest)
    }

    private suspend fun testDataStore(underTest: DataStore<TestData>) {
        val expectedData = testData {
            first = Random.nextInt()
            second = "expected data"
        }

        underTest.updateData { expectedData }

        underTest.data.first() shouldBe expectedData
    }

    @Test
    fun `create encrypted DataStore using delegate`() = runTest {
        testDataStore(context.encryptedDataStore)
    }
}

private object TestDataSerializer : Serializer<TestData> {

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
