package io.github.ackeecz.guardian.util

import org.gradle.process.ExecOperations
import java.io.ByteArrayOutputStream

internal interface ExecuteCommand {

    operator fun invoke(command: String): Result

    sealed interface Result {

        val commandOutput: String

        data class Success(override val commandOutput: String) : Result

        data class Error(
            override val commandOutput: String,
            val exitCode: Int,
        ) : Result
    }

    companion object {

        operator fun invoke(execOperations: ExecOperations): ExecuteCommand {
            return ExecuteCommandImpl(execOperations)
        }
    }
}

private class ExecuteCommandImpl(private val execOperations: ExecOperations) : ExecuteCommand {

    override fun invoke(command: String): ExecuteCommand.Result {
        val standardOutputStream = ByteArrayOutputStream()
        val errorOutputStream = ByteArrayOutputStream()
        val exitCode = execOperations.exec {
            commandLine = listOf("sh", "-c", command)
            isIgnoreExitValue = true
            standardOutput = standardOutputStream
            errorOutput = errorOutputStream
        }.exitValue
        return if (exitCode == 0) {
            ExecuteCommand.Result.Success(
                commandOutput = standardOutputStream.getString(),
            )
        } else {
            ExecuteCommand.Result.Error(
                commandOutput = errorOutputStream.getString(),
                exitCode = exitCode,
            )
        }
    }

    private fun ByteArrayOutputStream.getString() = toString(Charsets.UTF_8).trim()
}
