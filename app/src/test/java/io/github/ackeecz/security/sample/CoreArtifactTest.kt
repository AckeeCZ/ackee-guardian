package io.github.ackeecz.security.sample

import io.github.ackeecz.security.core.MasterKey
import kotlinx.coroutines.test.runTest
import org.junit.Test

internal class CoreArtifactTest : AndroidTestWithKeyStore() {

    @Test
    fun `generate master key`() = runTest {
        MasterKey.getOrCreate()
    }
}
