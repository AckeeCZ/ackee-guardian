package io.github.ackeecz.security.verification

import io.github.ackeecz.security.util.ExecuteCommand
import io.github.ackeecz.security.verification.GetLastTag.Companion.BOM_VERSION_TAG_PREFIX
import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.internal.cc.base.logger

/**
 * Gets last release tag from git history or first commit hash if no tag found.
 */
internal interface GetLastTag {

    operator fun invoke(project: Project): LastTagResult

    companion object {

        const val BOM_VERSION_TAG_PREFIX = "bom-"

        operator fun invoke(): GetLastTag {
            return GetLastTagImpl(ExecuteCommand())
        }
    }
}

/**
 * Result of [GetLastTag] which can be either [Tag] when a release tag was found or
 * [FirstCommitHash] as a fallback.
 */
internal sealed interface LastTagResult {

    val value: String

    data class Tag(override val value: String) : LastTagResult

    data class FirstCommitHash(override val value: String) : LastTagResult
}

internal class GetLastTagImpl(
    private val executeCommand: ExecuteCommand,
) : GetLastTag {

    override fun invoke(project: Project): LastTagResult {
        return Impl(project).invoke()
    }

    private inner class Impl(private val project: Project) {

        fun invoke(): LastTagResult {
            val lastTagResult = executeCommand("git describe --tags --match \"$BOM_VERSION_TAG_PREFIX*\" --abbrev=0", project)
            return when (lastTagResult) {
                is ExecuteCommand.Result.Success -> {
                    val lastTag = lastTagResult.commandOutput
                    LastTagResult.Tag(lastTag)
                }
                is ExecuteCommand.Result.Error -> {
                    processLastTagError(lastTagResult)
                }
            }
        }

        private fun processLastTagError(error: ExecuteCommand.Result.Error): LastTagResult.FirstCommitHash {
            return if (error.exitCode == NO_TAG_FOUND_EXIT_CODE) {
                processNoTagFoundError()
            } else {
                throw LastTagException(project, error)
            }
        }

        private fun processNoTagFoundError(): LastTagResult.FirstCommitHash {
            logger.warn("No $BOM_VERSION_TAG_PREFIX* tag found. If this is not a first library release, please fix the name of the release tag.")
            val firstCommitHashResult = executeCommand("git rev-list --max-parents=0 HEAD", project)
            return when (firstCommitHashResult) {
                is ExecuteCommand.Result.Success -> {
                    val firstCommitHash = firstCommitHashResult.commandOutput
                    return LastTagResult.FirstCommitHash(firstCommitHash)
                }
                is ExecuteCommand.Result.Error -> {
                    throw FirstCommitHashException(project, firstCommitHashResult)
                }
            }
        }
    }

    companion object {

        private const val NO_TAG_FOUND_EXIT_CODE = 128
    }
}

internal class FirstCommitHashException(
    project: Project,
    error: ExecuteCommand.Result.Error
) : GradleException() {

    override val message = "Getting first commit hash during check of project ${project.name} failed with $error"
}

internal class LastTagException(
    project: Project,
    error: ExecuteCommand.Result.Error
) : GradleException() {

    override val message = "Getting last release tag during check of project ${project.name} failed with $error"
}
