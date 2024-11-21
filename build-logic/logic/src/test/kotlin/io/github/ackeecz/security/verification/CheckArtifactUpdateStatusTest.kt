package io.github.ackeecz.security.verification

import io.github.ackeecz.security.testutil.buildProject
import io.github.ackeecz.security.util.ExecuteCommand
import io.github.ackeecz.security.util.ExecuteCommandStub
import io.github.ackeecz.security.verification.GetLastTagTest.Companion.BOM_VERSION_TAG_PREFIX
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldEndWith
import io.kotest.matchers.string.shouldStartWith

private lateinit var getLastTag: GetLastTagStub
private lateinit var executeCommand: ExecuteCommandStub
private lateinit var underTest: CheckArtifactUpdateStatus

internal class CheckArtifactUpdateStatusTest : FunSpec({

    beforeEach {
        getLastTag = GetLastTagStub()
        executeCommand = ExecuteCommandStub()
        underTest = CheckArtifactUpdateStatusImpl(
            getLastTag = getLastTag,
            executeCommand = executeCommand,
        )
    }

    test("artifact is up-to-date when last tag does not exist and there are no changes") {
        val firstCommitHash = "de5035f5a24621ea5361279d867ad75abc967ca3"
        getLastTag.result = LastTagResult.FirstCommitHash(firstCommitHash)
        executeCommand.resultStrategy = ExecuteCommandStub.ResultStrategy.OneRepeating(
            // no changes found
            ExecuteCommand.Result.Success(commandOutput = ""),
        )

        underTest() shouldBe ArtifactUpdateStatus.UP_TO_DATE
        assertCorrectDiffCommand(diffFrom = firstCommitHash)
    }

    test("artifact needs update when last tag does not exist and there are changes") {
        getLastTag.result = LastTagResult.FirstCommitHash("de5035f5a24621ea5361279d867ad75abc967ca3")
        executeCommand.resultStrategy = ExecuteCommandStub.ResultStrategy.OneRepeating(
            // changes found
            ExecuteCommand.Result.Success(commandOutput = "changes found"),
        )

        underTest() shouldBe ArtifactUpdateStatus.UPDATE_NEEDED
    }

    test("artifact is up-to-date when last tag exists and there are no changes") {
        val lastTag = "${BOM_VERSION_TAG_PREFIX}1.0.0"
        getLastTag.result = LastTagResult.Tag(lastTag)
        executeCommand.resultStrategy = ExecuteCommandStub.ResultStrategy.OneRepeating(
            // no changes found
            ExecuteCommand.Result.Success(commandOutput = ""),
        )

        underTest() shouldBe ArtifactUpdateStatus.UP_TO_DATE
        assertCorrectDiffCommand(diffFrom = lastTag)
    }

    test("artifact needs update when last tag exists and there are changes") {
        getLastTag.result = LastTagResult.Tag("${BOM_VERSION_TAG_PREFIX}1.0.0")
        executeCommand.resultStrategy = ExecuteCommandStub.ResultStrategy.OneRepeating(
            // changes found
            ExecuteCommand.Result.Success(commandOutput = "changes found"),
        )

        underTest() shouldBe ArtifactUpdateStatus.UPDATE_NEEDED
    }

    test("throw if diff check fails") {
        getLastTag.result = LastTagResult.Tag("${BOM_VERSION_TAG_PREFIX}1.0.0")
        executeCommand.resultStrategy = ExecuteCommandStub.ResultStrategy.OneRepeating(
            // diff check fails
            ExecuteCommand.Result.Error(commandOutput = "", exitCode = 100),
        )

        shouldThrow<DiffCheckException> { underTest() }
    }
})

private operator fun CheckArtifactUpdateStatus.invoke(): ArtifactUpdateStatus {
    return invoke(buildProject())
}

private fun assertCorrectDiffCommand(diffFrom: String) {
    executeCommand.commands
        .getOrNull(0)
        .shouldStartWith("git diff $diffFrom -- ")
        .shouldEndWith("/src/main")
}
