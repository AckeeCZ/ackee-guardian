package io.github.ackeecz.guardian.util

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.types.shouldBeInstanceOf
import org.gradle.api.Action
import org.gradle.process.BaseExecSpec
import org.gradle.process.CommandLineArgumentProvider
import org.gradle.process.ExecOperations
import org.gradle.process.ExecResult
import org.gradle.process.ExecSpec
import org.gradle.process.JavaExecSpec
import org.gradle.process.ProcessForkOptions
import java.io.File
import java.io.InputStream
import java.io.OutputStream

private lateinit var execOperations: ExecOperationsMock
private lateinit var underTest: ExecuteCommand

internal class ExecuteCommandTest : FunSpec({

    beforeEach {
        execOperations = ExecOperationsMock()
        underTest = ExecuteCommand(execOperations)
    }

    test("return success on successful command execution") {
        val expectedOutput = "expected command output"
        val command = "echo $expectedOutput"
        execOperations.exitValue = 0 // Simulate successful execution
        execOperations.standardOutputContent = expectedOutput // Simulate command output

        val result = underTest(command)

        with(execOperations.execSpec) {
            commandLine shouldBe listOf("sh", "-c", command)
            isIgnoreExitValue.shouldBeTrue()
        }
        result.shouldBeInstanceOf<ExecuteCommand.Result.Success>()
            .commandOutput
            .shouldBe(expectedOutput)
    }

    test("return error on failed command execution") {
        val expectedExitCode = 1
        val expectedOutput = "No such file or directory"
        val command = "cat non_existent_file.non_existing"
        execOperations.exitValue = expectedExitCode // Simulate error execution
        execOperations.errorOutputContent = expectedOutput // Simulate command output

        val result = underTest(command)

        with(result.shouldBeInstanceOf<ExecuteCommand.Result.Error>()) {
            commandOutput shouldContain expectedOutput
            exitCode shouldBe expectedExitCode
        }
    }
})

private class ExecOperationsMock : ExecOperations {

    val execSpec: ExecSpec = ExecSpecMock()
    var exitValue: Int = 0
    var standardOutputContent: String = ""
    var errorOutputContent: String = ""

    override fun exec(action: Action<in ExecSpec>): ExecResult {
        action.execute(execSpec)
        execSpec.standardOutput.use { it.write(standardOutputContent.toByteArray(Charsets.UTF_8)) }
        execSpec.errorOutput.use { it.write(errorOutputContent.toByteArray(Charsets.UTF_8)) }
        return object : ExecResult {
            override fun getExitValue(): Int = this@ExecOperationsMock.exitValue
            override fun assertNormalExitValue(): ExecResult = throw NotImplementedError()
            override fun rethrowFailure(): ExecResult = throw NotImplementedError()
        }
    }

    override fun javaexec(action: Action<in JavaExecSpec>?): ExecResult {
        throw NotImplementedError("JavaExecSpec is not implemented in this mock")
    }
}

private class ExecSpecMock : ExecSpec {

    private var isIgnoreExitValue: Boolean? = null
    private var standardOutput: OutputStream? = null
    private var errorOutput: OutputStream? = null
    private var commandLine: List<String> = emptyList()

    override fun getExecutable(): String = throw NotImplementedError()

    override fun setExecutable(executable: String?) {
        throw NotImplementedError()
    }

    override fun setExecutable(executable: Any?) {
        throw NotImplementedError()
    }

    override fun executable(executable: Any?): ProcessForkOptions = throw NotImplementedError()

    override fun getWorkingDir(): File = throw NotImplementedError()

    override fun setWorkingDir(dir: File?) {
        throw NotImplementedError()
    }

    override fun setWorkingDir(dir: Any?) {
        throw NotImplementedError()
    }

    override fun workingDir(dir: Any?): ProcessForkOptions = throw NotImplementedError()

    override fun getEnvironment(): MutableMap<String, Any> = throw NotImplementedError()

    override fun setEnvironment(environmentVariables: MutableMap<String, *>?) {
        throw NotImplementedError()
    }

    override fun environment(environmentVariables: MutableMap<String, *>?): ProcessForkOptions = throw NotImplementedError()

    override fun environment(name: String?, value: Any?): ProcessForkOptions = throw NotImplementedError()

    override fun copyTo(options: ProcessForkOptions?): ProcessForkOptions = throw NotImplementedError()

    override fun setIgnoreExitValue(ignoreExitValue: Boolean): BaseExecSpec {
        isIgnoreExitValue = ignoreExitValue
        return this
    }

    override fun isIgnoreExitValue(): Boolean = requireNotNull(isIgnoreExitValue)

    override fun setStandardInput(inputStream: InputStream?): BaseExecSpec {
        throw NotImplementedError()
    }

    override fun getStandardInput(): InputStream = throw NotImplementedError()

    override fun setStandardOutput(outputStream: OutputStream?): BaseExecSpec {
        standardOutput = outputStream
        return this
    }

    override fun getStandardOutput(): OutputStream? = standardOutput

    override fun setErrorOutput(outputStream: OutputStream?): BaseExecSpec {
        errorOutput = outputStream
        return this
    }

    override fun getErrorOutput(): OutputStream? = errorOutput

    override fun getCommandLine(): MutableList<String> = commandLine.toMutableList()

    override fun setCommandLine(args: MutableList<String>) {
        commandLine = args
    }

    override fun setCommandLine(vararg args: Any?) {
        throw NotImplementedError()
    }

    override fun setCommandLine(args: MutableIterable<*>?) {
        throw NotImplementedError()
    }

    override fun commandLine(vararg args: Any?): ExecSpec {
        throw NotImplementedError()
    }

    override fun commandLine(args: MutableIterable<*>?): ExecSpec {
        throw NotImplementedError()
    }

    override fun args(vararg args: Any?): ExecSpec {
        throw NotImplementedError()
    }

    override fun args(args: MutableIterable<*>?): ExecSpec {
        throw NotImplementedError()
    }

    override fun setArgs(args: MutableList<String>?): ExecSpec {
        throw NotImplementedError()
    }

    override fun setArgs(args: MutableIterable<*>?): ExecSpec {
        throw NotImplementedError()
    }

    override fun getArgs(): MutableList<String> {
        throw NotImplementedError()
    }

    override fun getArgumentProviders(): MutableList<CommandLineArgumentProvider> {
        throw NotImplementedError()
    }
}
