package io.github.ackeecz.guardian.verification

import io.github.ackeecz.guardian.util.ExecuteCommand
import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.process.ExecOperations
import org.jetbrains.annotations.VisibleForTesting

/**
 * Checks if the artifact has changed since the version specified by the passed [TagResult] and
 * needs to be updated (new version released) or if it is up-to-date and update is not necessary.
 */
internal interface CheckArtifactUpdateStatus {

    operator fun invoke(project: Project, tagResult: TagResult): ArtifactUpdateStatus

    companion object {

        operator fun invoke(execOperations: ExecOperations): CheckArtifactUpdateStatus {
            return CheckArtifactUpdateStatusImpl(executeCommand = ExecuteCommand(execOperations))
        }
    }
}

@VisibleForTesting
internal class CheckArtifactUpdateStatusImpl(
    private val executeCommand: ExecuteCommand,
) : CheckArtifactUpdateStatus {

    override operator fun invoke(
        project: Project,
        tagResult: TagResult,
    ): ArtifactUpdateStatus {
        val dirPathToCheck = project.file(SRC_MAIN_DIR).absolutePath
        val command = "git diff ${tagResult.value} -- $dirPathToCheck"
        return when (val diffResult = executeCommand(command)) {
            is ExecuteCommand.Result.Success -> {
                if (diffResult.commandOutput.isBlank()) {
                    ArtifactUpdateStatus.UP_TO_DATE
                } else {
                    ArtifactUpdateStatus.UPDATE_NEEDED
                }
            }
            is ExecuteCommand.Result.Error -> {
                throw DiffCheckException(project, diffResult)
            }
        }
    }

    companion object {

        private const val SRC_MAIN_DIR = "src/main"
    }
}

internal enum class ArtifactUpdateStatus {

    UP_TO_DATE,
    UPDATE_NEEDED,
}

internal class DiffCheckException(
    project: Project,
    error: ExecuteCommand.Result.Error
) : GradleException() {

    override val message = "Diff check of project ${project.name} failed with $error"
}
