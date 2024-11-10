package io.github.ackeecz.security.verification

import io.github.ackeecz.security.testutil.buildProject
import io.github.ackeecz.security.util.ExecuteCommand
import io.github.ackeecz.security.util.StubExecuteCommand
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldEndWith
import io.kotest.matchers.string.shouldStartWith
import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder

private lateinit var getLastTag: StubGetLastTag
private lateinit var executeCommand: StubExecuteCommand
private lateinit var underTest: CheckArtifactUpdateStatus

internal class CheckArtifactUpdateStatusTest : FunSpec({

    beforeEach {
        getLastTag = StubGetLastTag()
        executeCommand = StubExecuteCommand()
        underTest = CheckArtifactUpdateStatusImpl(
            getLastTag = getLastTag,
            executeCommand = executeCommand,
        )
    }

    test("artifact is up-to-date when last tag does not exist and there are no changes") {
        val firstCommitHash = "de5035f5a24621ea5361279d867ad75abc967ca3"
        getLastTag.result = LastTagResult.FirstCommitHash(firstCommitHash)
        executeCommand.resultStrategy = StubExecuteCommand.ResultStrategy.OneRepeating(
            // no changes found
            ExecuteCommand.Result.Success(commandOutput = ""),
        )

        underTest() shouldBe ArtifactUpdateStatus.UP_TO_DATE
        assertCorrectDiffCommand(diffFrom = firstCommitHash)
    }

    test("artifact needs update when last tag does not exist and there are changes") {
        getLastTag.result = LastTagResult.FirstCommitHash("de5035f5a24621ea5361279d867ad75abc967ca3")
        executeCommand.resultStrategy = StubExecuteCommand.ResultStrategy.OneRepeating(
            // changes found
            ExecuteCommand.Result.Success(commandOutput = "changes found"),
        )

        underTest() shouldBe ArtifactUpdateStatus.UPDATE_NEEDED
    }

    test("artifact is up-to-date when last tag exists and there are no changes") {
        val lastTag = "bom-1.0.0"
        getLastTag.result = LastTagResult.Tag(lastTag)
        executeCommand.resultStrategy = StubExecuteCommand.ResultStrategy.OneRepeating(
            // no changes found
            ExecuteCommand.Result.Success(commandOutput = ""),
        )

        underTest() shouldBe ArtifactUpdateStatus.UP_TO_DATE
        assertCorrectDiffCommand(diffFrom = lastTag)
    }

    test("artifact needs update when last tag exists and there are changes") {
        getLastTag.result = LastTagResult.Tag("bom-1.0.0")
        executeCommand.resultStrategy = StubExecuteCommand.ResultStrategy.OneRepeating(
            // changes found
            ExecuteCommand.Result.Success(commandOutput = "changes found"),
        )

        underTest() shouldBe ArtifactUpdateStatus.UPDATE_NEEDED
    }

    test("throw if diff check fails") {
        getLastTag.result = LastTagResult.Tag("bom-1.0.0")
        executeCommand.resultStrategy = StubExecuteCommand.ResultStrategy.OneRepeating(
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
