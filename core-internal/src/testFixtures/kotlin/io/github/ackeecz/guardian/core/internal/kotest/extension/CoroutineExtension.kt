package io.github.ackeecz.guardian.core.internal.kotest.extension

import io.kotest.core.listeners.AfterEachListener
import io.kotest.core.listeners.BeforeEachListener
import io.kotest.core.test.TestCase
import io.kotest.core.test.TestResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain

class CoroutineExtension(
    private val testDispatcher: TestDispatcher = StandardTestDispatcher(),
) : BeforeEachListener, AfterEachListener {

    override suspend fun beforeEach(testCase: TestCase) {
        beforeEach()
    }

    fun beforeEach() {
        Dispatchers.setMain(testDispatcher)
    }

    override suspend fun afterEach(testCase: TestCase, result: TestResult) {
        afterEach()
    }

    fun afterEach() {
        Dispatchers.resetMain()
    }
}
