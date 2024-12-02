package io.github.ackeecz.security.util

import org.gradle.api.Project

internal class ExecuteCommandStub : ExecuteCommand {

    private val _commands: MutableList<String> = mutableListOf()
    val commands: List<String> = _commands

    var resultStrategy: ResultStrategy = ResultStrategy.OneRepeating(ExecuteCommand.Result.Success(""))

    override fun invoke(command: String, project: Project): ExecuteCommand.Result {
        _commands += command
        return when (val resultStrategy = resultStrategy) {
            is ResultStrategy.OneRepeating -> resultStrategy.result
            is ResultStrategy.MultipleExact -> resultStrategy.results[_commands.size - 1]
        }
    }

    sealed interface ResultStrategy {

        data class OneRepeating(val result: ExecuteCommand.Result) : ResultStrategy

        class MultipleExact(vararg val results: ExecuteCommand.Result) : ResultStrategy
    }
}
