package io.github.ackeecz.guardian.util

internal fun createErrorExecuteCommandResult(
    commandOutput: String = "",
    exitCode: Int = 1,
): ExecuteCommand.Result.Error {
    return ExecuteCommand.Result.Error(commandOutput = commandOutput, exitCode = exitCode)
}
