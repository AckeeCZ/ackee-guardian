package io.github.ackeecz.guardian.verification

import io.github.ackeecz.guardian.testutil.buildProject
import io.github.ackeecz.guardian.util.ExecuteCommand
import io.github.ackeecz.guardian.util.ExecuteCommandStub
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

private const val NO_TAG_FOUND_EXIT_CODE = 128
private val noTagFoundError = ExecuteCommand.Result.Error(
    commandOutput = "fatal: No names found, cannot describe anything.",
    exitCode = NO_TAG_FOUND_EXIT_CODE,
)

private lateinit var executeCommand: ExecuteCommandStub
private lateinit var underTest: GetTag

internal abstract class GetTagTest(
    private val createSut: () -> GetTag,
    body: FunSpec.() -> Unit,
) : FunSpec({

    beforeEach {
        executeCommand = ExecuteCommandStub()
        underTest = createSut()
    }

    body()

    test("get first commit hash when tag does not exist") {
        val firstCommitHash = "de5035f5a24621ea5361279d867ad75abc967ca3"
        executeCommand.resultStrategy = ExecuteCommandStub.ResultStrategy.MultipleExact(
            noTagFoundError,
            // get hash of the first commit
            ExecuteCommand.Result.Success(commandOutput = firstCommitHash),
        )

        underTest() shouldBe TagResult.FirstCommitHash(firstCommitHash)
        assertCorrectFirstCommitHashCommand()
    }

    test("get tag when it exists") {
        val tag = "${BOM_VERSION_TAG_PREFIX}1.0.0"
        executeCommand.resultStrategy = ExecuteCommandStub.ResultStrategy.OneRepeating(
            ExecuteCommand.Result.Success(commandOutput = tag),
        )

        underTest() shouldBe TagResult.Tag(tag)
    }

    test("throw if getting first commit hash fails") {
        executeCommand.resultStrategy = ExecuteCommandStub.ResultStrategy.MultipleExact(
            noTagFoundError,
            // getting first commit fails
            ExecuteCommand.Result.Error(commandOutput = "", exitCode = 100),
        )

        shouldThrow<FirstCommitHashException> { underTest() }
    }

    test("throw if getting tag fails with other code than $NO_TAG_FOUND_EXIT_CODE") {
        executeCommand.resultStrategy = ExecuteCommandStub.ResultStrategy.MultipleExact(
            ExecuteCommand.Result.Error(commandOutput = "", exitCode = NO_TAG_FOUND_EXIT_CODE + 1),
        )

        shouldThrow<TagException> { underTest() }
    }
}) {

    companion object {

        const val BOM_VERSION_TAG_PREFIX = "bom-"
    }
}

private operator fun GetTag.invoke(): TagResult {
    return invoke(buildProject())
}

private fun assertCorrectFirstCommitHashCommand() {
    executeCommand.commands
        .getOrNull(1)
        .shouldBe("git rev-list --max-parents=0 HEAD")
}

internal class GetCurrentTagTest : GetTagTest(createSut = { GetCurrentTagImpl(executeCommand) }, {

    test("call correct first command for getting current BOM version tag") {
        underTest()

        executeCommand.commands
            .firstOrNull()
            .shouldBe("git describe --tags --match \"${BOM_VERSION_TAG_PREFIX}*\" --abbrev=0")
    }
})

internal class GetPreviousTagTest : GetTagTest(createSut = { GetPreviousTagImpl(executeCommand) }, {

    test("call correct first command for getting previous BOM version tag") {
        val expectedCommand = "git describe --tags --match \"${BOM_VERSION_TAG_PREFIX}*\" --abbrev=0 " +
            "\$(git rev-list --tags=\"${BOM_VERSION_TAG_PREFIX}*\" --skip=1 --max-count=1 HEAD)"

        underTest()

        executeCommand.commands
            .firstOrNull()
            .shouldBe(expectedCommand)
    }
})
