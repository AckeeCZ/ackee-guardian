package io.github.ackeecz.guardian.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.core.DataStoreFactory
import androidx.datastore.dataStoreFile
import io.github.ackeecz.guardian.datastore.core.DataStoreCryptoParams
import io.github.ackeecz.guardian.datastore.core.internal.DataStoreDelegateWrapper
import io.github.ackeecz.guardian.datastore.core.internal.EncryptedDataStoreTest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.test.runTest
import org.junit.Test
import java.io.File
import java.io.InputStream
import kotlin.random.Random

/**
 * Helper base class for typed data store tests that contains common tests setup
 */
internal abstract class EncryptedTypedDataStoreTest : EncryptedDataStoreTest<TestData>() {

    override fun getDefaultDataStoreFile(fileName: String): File {
        return context.dataStoreFile(fileName)
    }

    override suspend fun updateData(currentData: TestData): TestData {
        return currentData.copy { first += 1 }
    }

    override fun createExpectedData(): TestData {
        return testData {
            first = Random.nextInt()
            second = "value"
        }
    }

    override suspend fun InputStream.decodeTestData(): TestData = TestDataSerializer.readFrom(this)
}

internal class EncryptedDataStoreFactoryTest : EncryptedTypedDataStoreTest() {

    override fun createSut(
        cryptoParams: DataStoreCryptoParams,
        fileName: String,
        coroutineScope: CoroutineScope,
    ): DataStore<TestData> {
        return DataStoreFactory.createEncrypted(
            context = context,
            cryptoParams = cryptoParams,
            serializer = TestDataSerializer,
            scope = coroutineScope,
            produceFile = { getDefaultDataStoreFile(fileName) },
        )
    }
}

internal class EncryptedDataStoreDelegateTest : EncryptedTypedDataStoreTest() {

    override fun createSut(
        cryptoParams: DataStoreCryptoParams,
        fileName: String,
        coroutineScope: CoroutineScope,
    ): DataStore<TestData> {
        return TypeDataStoreDelegateWrapper(
            context = context,
            cryptoParams = cryptoParams,
            fileName = fileName,
            coroutineScope = coroutineScope,
        ).getDataStore()
    }

    @Test
    fun `delegate returns singleton`() = runTest {
        testDataStoreDelegateSingleton { keyAliasIndex ->
            TypeDataStoreDelegateWrapper(
                context = context,
                cryptoParams = createDefaultCryptoParams(),
                fileName = "test_file_$keyAliasIndex",
                coroutineScope = createDataStoreCoroutineScope(),
            )
        }
    }
}

private class TypeDataStoreDelegateWrapper(
    private val context: Context,
    cryptoParams: DataStoreCryptoParams,
    fileName: String,
    coroutineScope: CoroutineScope,
) : DataStoreDelegateWrapper<TestData> {

    val Context.dataStore by encryptedDataStore(
        cryptoParams = cryptoParams,
        fileName = fileName,
        serializer = TestDataSerializer,
        scope = coroutineScope,
    )

    override fun getDataStore() = context.dataStore
}
