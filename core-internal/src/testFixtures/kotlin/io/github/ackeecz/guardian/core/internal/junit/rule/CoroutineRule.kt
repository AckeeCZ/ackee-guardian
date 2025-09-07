package io.github.ackeecz.guardian.core.internal.junit.rule

import io.github.ackeecz.guardian.core.internal.kotest.extension.CoroutineExtension
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import org.junit.rules.TestWatcher
import org.junit.runner.Description

public class CoroutineRule(
    public val testDispatcher: TestDispatcher = StandardTestDispatcher(),
) : TestWatcher() {

    private val coroutineExtension = CoroutineExtension(testDispatcher)

    override fun starting(description: Description?) {
        coroutineExtension.beforeEach()
    }

    override fun finished(description: Description?) {
        coroutineExtension.afterEach()
    }
}
