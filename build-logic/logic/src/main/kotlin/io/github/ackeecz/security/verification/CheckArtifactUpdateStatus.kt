package io.github.ackeecz.security.verification

import io.github.ackeecz.security.util.ExecuteCommand
import org.gradle.api.GradleException
import org.gradle.api.Project

/**
 * Checks if the artifact has changed and needs to be updated (new version released) or
 * if it is up-to-date and update is not necessary.
 */
internal interface CheckArtifactUpdateStatus {

    operator fun invoke(project: Project): ArtifactUpdateStatus

    companion object {

        operator fun invoke(): CheckArtifactUpdateStatus {
            return CheckArtifactUpdateStatusImpl(
                getLastTag = GetLastTag(),
                executeCommand = ExecuteCommand(),
            )
        }
    }
}

internal class CheckArtifactUpdateStatusImpl(
    private val getLastTag: GetLastTag,
    private val executeCommand: ExecuteCommand,
) : CheckArtifactUpdateStatus {

    override operator fun invoke(project: Project): ArtifactUpdateStatus {
        val dirPathToCheck = project.file(SRC_MAIN_DIR).absolutePath
        val tagOrCommitHash = getLastTag(project).value
        val diffResult = executeCommand("git diff $tagOrCommitHash -- $dirPathToCheck", project)
        return when (diffResult) {
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
