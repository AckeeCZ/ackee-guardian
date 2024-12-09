package io.github.ackeecz.guardian.verification

import io.github.ackeecz.guardian.testutil.buildProject
import io.github.ackeecz.guardian.util.ExecuteCommand
import io.github.ackeecz.guardian.util.ExecuteCommandStub
import io.github.ackeecz.guardian.verification.GetTagTest.Companion.BOM_VERSION_TAG_PREFIX
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldEndWith
import io.kotest.matchers.string.shouldStartWith

private lateinit var executeCommand: ExecuteCommandStub
private lateinit var underTest: CheckArtifactUpdateStatus

internal class CheckArtifactUpdateStatusTest : FunSpec({

    beforeEach {
        executeCommand = ExecuteCommandStub()
        underTest = CheckArtifactUpdateStatusImpl(
            executeCommand = executeCommand,
        )
    }

    test("artifact is up-to-date when tag does not exist and there are no changes") {
        val firstCommitHash = "de5035f5a24621ea5361279d867ad75abc967ca3"
        val tagResult = TagResult.FirstCommitHash(firstCommitHash)
        executeCommand.resultStrategy = ExecuteCommandStub.ResultStrategy.OneRepeating(
            // no changes found
            ExecuteCommand.Result.Success(commandOutput = ""),
        )

        underTest(tagResult) shouldBe ArtifactUpdateStatus.UP_TO_DATE
        assertCorrectDiffCommand(diffFrom = firstCommitHash)
    }

    test("artifact needs update when tag does not exist and there are changes") {
        val tagResult = TagResult.FirstCommitHash("de5035f5a24621ea5361279d867ad75abc967ca3")
        executeCommand.resultStrategy = ExecuteCommandStub.ResultStrategy.OneRepeating(
            // changes found
            ExecuteCommand.Result.Success(commandOutput = "changes found"),
        )

        underTest(tagResult) shouldBe ArtifactUpdateStatus.UPDATE_NEEDED
    }

    test("artifact is up-to-date when tag exists and there are no changes") {
        val tag = "${BOM_VERSION_TAG_PREFIX}1.0.0"
        val tagResult = TagResult.Tag(tag)
        executeCommand.resultStrategy = ExecuteCommandStub.ResultStrategy.OneRepeating(
            // no changes found
            ExecuteCommand.Result.Success(commandOutput = ""),
        )

        underTest(tagResult) shouldBe ArtifactUpdateStatus.UP_TO_DATE
        assertCorrectDiffCommand(diffFrom = tag)
    }

    test("artifact needs update when tag exists and there are changes") {
        val tagResult = TagResult.Tag("${BOM_VERSION_TAG_PREFIX}1.0.0")
        executeCommand.resultStrategy = ExecuteCommandStub.ResultStrategy.OneRepeating(
            // changes found
            ExecuteCommand.Result.Success(commandOutput = "changes found"),
        )

        underTest(tagResult) shouldBe ArtifactUpdateStatus.UPDATE_NEEDED
    }

    test("throw if diff check fails") {
        val tagResult = TagResult.Tag("${BOM_VERSION_TAG_PREFIX}1.0.0")
        executeCommand.resultStrategy = ExecuteCommandStub.ResultStrategy.OneRepeating(
            // diff check fails
            ExecuteCommand.Result.Error(commandOutput = "", exitCode = 100),
        )

        shouldThrow<DiffCheckException> { underTest(tagResult) }
    }
})

private operator fun CheckArtifactUpdateStatus.invoke(tagResult: TagResult): ArtifactUpdateStatus {
    return invoke(buildProject(), tagResult)
}

private fun assertCorrectDiffCommand(diffFrom: String) {
    executeCommand.commands
        .getOrNull(0)
        .shouldStartWith("git diff $diffFrom -- ")
        .shouldEndWith("/src/main")
}
