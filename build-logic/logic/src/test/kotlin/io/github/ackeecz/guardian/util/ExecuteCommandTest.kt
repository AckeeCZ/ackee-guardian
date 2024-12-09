package io.github.ackeecz.guardian.util

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldBeEmpty
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.types.shouldBeInstanceOf
import org.gradle.testfixtures.ProjectBuilder

private lateinit var underTest: ExecuteCommand

internal class ExecuteCommandTest : FunSpec({

    beforeEach {
        underTest = ExecuteCommand()
    }

    test("return success on successful command execution") {
        val expectedOutput = "expected command output"
        val command = "echo $expectedOutput"

        val result = underTest(command)

        result.shouldBeInstanceOf<ExecuteCommand.Result.Success>()
            .commandOutput
            .shouldBe(expectedOutput)
    }

    test("return error on failed command execution") {
        val command = "cat non_existent_file.non_existing"

        val result = underTest(command)

        with(result.shouldBeInstanceOf<ExecuteCommand.Result.Error>()) {
            commandOutput shouldContain "No such file or directory"
            exitCode shouldBe 1
        }
    }

    test("return error on failed command execution with pipe operator") {
        // Fails with exit code 1 on grep not finding goodbye in hello
        val command = """echo "hello" | grep "goodbye""""

        val result = underTest(command)

        with(result.shouldBeInstanceOf<ExecuteCommand.Result.Error>()) {
            commandOutput.shouldBeEmpty()
            exitCode shouldBe 1
        }
    }
})

private operator fun ExecuteCommand.invoke(command: String): ExecuteCommand.Result {
    return invoke(command, ProjectBuilder.builder().build())
}
