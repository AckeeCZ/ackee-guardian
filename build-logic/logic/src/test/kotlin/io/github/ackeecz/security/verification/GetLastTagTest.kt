package io.github.ackeecz.security.verification

import io.github.ackeecz.security.testutil.buildProject
import io.github.ackeecz.security.util.ExecuteCommand
import io.github.ackeecz.security.util.StubExecuteCommand
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

private const val NO_TAG_FOUND_EXIT_CODE = 128
private val noTagFoundError = ExecuteCommand.Result.Error(
    commandOutput = "fatal: No names found, cannot describe anything.",
    exitCode = NO_TAG_FOUND_EXIT_CODE,
)

private lateinit var executeCommand: StubExecuteCommand
private lateinit var underTest: GetLastTag

internal class GetLastTagTest : FunSpec({

    beforeEach {
        executeCommand = StubExecuteCommand()
        underTest = GetLastTagImpl(executeCommand)
    }

    test("call correct first command for getting latest BOM version tag") {
        underTest()

        executeCommand.commands
            .firstOrNull()
            .shouldBe("git describe --tags --match \"bom-*\" --abbrev=0")
    }

    test("get first commit hash when last tag does not exist") {
        val firstCommitHash = "de5035f5a24621ea5361279d867ad75abc967ca3"
        executeCommand.resultStrategy = StubExecuteCommand.ResultStrategy.MultipleExact(
            noTagFoundError,
            // get hash of the first commit
            ExecuteCommand.Result.Success(commandOutput = firstCommitHash),
        )

        underTest() shouldBe LastTagResult.FirstCommitHash(firstCommitHash)
        assertCorrectFirstCommitHashCommand()
    }

    test("get last tag when last tag exists") {
        val lastTag = "bom-1.0.0"
        executeCommand.resultStrategy = StubExecuteCommand.ResultStrategy.OneRepeating(
            ExecuteCommand.Result.Success(commandOutput = lastTag),
        )

        underTest() shouldBe LastTagResult.Tag(lastTag)
    }

    test("throw if getting first commit hash fails") {
        executeCommand.resultStrategy = StubExecuteCommand.ResultStrategy.MultipleExact(
            noTagFoundError,
            // getting first commit fails
            ExecuteCommand.Result.Error(commandOutput = "", exitCode = 100),
        )

        shouldThrow<FirstCommitHashException> { underTest() }
    }

    test("throw if getting last tag fails with other code than $NO_TAG_FOUND_EXIT_CODE") {
        executeCommand.resultStrategy = StubExecuteCommand.ResultStrategy.MultipleExact(
            ExecuteCommand.Result.Error(commandOutput = "", exitCode = NO_TAG_FOUND_EXIT_CODE + 1),
        )

        shouldThrow<LastTagException> { underTest() }
    }
})

private operator fun GetLastTag.invoke(): LastTagResult {
    return invoke(buildProject())
}

private fun assertCorrectFirstCommitHashCommand() {
    executeCommand.commands
        .getOrNull(1)
        .shouldBe("git rev-list --max-parents=0 HEAD")
}