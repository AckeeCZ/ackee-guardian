package io.github.ackeecz.guardian.sample

import io.github.ackeecz.guardian.core.MasterKey
import kotlinx.coroutines.test.runTest
import org.junit.Test

internal class CoreArtifactTest : AndroidTestWithKeyStore() {

    @Test
    fun `generate master key`() = runTest {
        MasterKey.getOrCreate()
    }
}
