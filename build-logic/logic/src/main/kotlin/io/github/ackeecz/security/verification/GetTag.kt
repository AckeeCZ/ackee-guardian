package io.github.ackeecz.security.verification

import io.github.ackeecz.security.util.ExecuteCommand
import io.github.ackeecz.security.verification.GetTag.Companion.BOM_VERSION_TAG_PREFIX
import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.internal.cc.base.logger
import org.jetbrains.annotations.VisibleForTesting

/**
 * Gets release tag from git history or first commit hash if no tag found.
 */
internal interface GetTag {

    operator fun invoke(project: Project): TagResult

    companion object {

        // Deploy Github workflow relies on this tag format, so if you need to change it, you need
        // to change it there as well.
        const val BOM_VERSION_TAG_PREFIX = "bom-"
    }
}

/**
 * Result of [GetTag] which can be either [Tag] when a release tag was found or
 * [FirstCommitHash] as a fallback.
 */
internal sealed interface TagResult {

    val value: String

    data class Tag(override val value: String) : TagResult

    data class FirstCommitHash(override val value: String) : TagResult
}

/**
 * Gets current release tag from git history or first commit hash if no tag found. This is the latest
 * tag found in the git history, which can be a tag of the last published version or a tag of the
 * version being currently published, depending on when this is called.
 */
internal interface GetCurrentTag : GetTag {

    companion object {

        operator fun invoke(): GetCurrentTag {
            return GetCurrentTagImpl(ExecuteCommand())
        }
    }
}

/**
 * Gets previous release tag from git history or first commit hash if no tag found. This is the last
 * but one tag found in the git history. For example when this is called during a publishing process,
 * the current (last) tag is the one of the version being published right now and the last but one
 * tag return by this interface is the tag of the latest published version on the production.
 */
internal interface GetPreviousTag : GetTag {

    companion object {

        operator fun invoke(): GetPreviousTag {
            return GetPreviousTagImpl(ExecuteCommand())
        }
    }
}

@VisibleForTesting
internal class GetCurrentTagImpl(
    private val executeCommand: ExecuteCommand,
) : GetCurrentTag {

    override fun invoke(project: Project): TagResult {
        val getCurrentTagCommand = "git describe --tags --match \"$BOM_VERSION_TAG_PREFIX*\" --abbrev=0"
        return GetTagImpl(
            project = project,
            executeCommand = executeCommand,
            getTagCommand = getCurrentTagCommand,
        ).invoke()
    }
}

@VisibleForTesting
internal class GetPreviousTagImpl(
    private val executeCommand: ExecuteCommand,
) : GetPreviousTag {

    override fun invoke(project: Project): TagResult {
        val getPreviousTagCommand = "git describe --tags --match \"$BOM_VERSION_TAG_PREFIX*\" " +
            "--abbrev=0 \$(git rev-list --tags=\"$BOM_VERSION_TAG_PREFIX*\" --skip=1 --max-count=1 HEAD)"
        return GetTagImpl(
            project = project,
            executeCommand = executeCommand,
            getTagCommand = getPreviousTagCommand,
        ).invoke()
    }
}

private class GetTagImpl(
    private val project: Project,
    private val executeCommand: ExecuteCommand,
    private val getTagCommand: String,
) {

    fun invoke(): TagResult {
        return when (val tagResult = executeCommand(getTagCommand, project)) {
            is ExecuteCommand.Result.Success -> TagResult.Tag(tagResult.commandOutput)
            is ExecuteCommand.Result.Error -> processTagError(tagResult)
        }
    }

    private fun processTagError(error: ExecuteCommand.Result.Error): TagResult.FirstCommitHash {
        return if (error.exitCode == NO_TAG_FOUND_EXIT_CODE) {
            processNoTagFoundError()
        } else {
            throw TagException(project, error)
        }
    }

    private fun processNoTagFoundError(): TagResult.FirstCommitHash {
        logger.warn("No $BOM_VERSION_TAG_PREFIX* tag found. If this is unexpected, please fix the name of the release tag.")
        val getFirstCommitHashCommand = "git rev-list --max-parents=0 HEAD"
        when (val firstCommitHashResult = executeCommand(getFirstCommitHashCommand, project)) {
            is ExecuteCommand.Result.Success -> {
                val firstCommitHash = firstCommitHashResult.commandOutput
                return TagResult.FirstCommitHash(firstCommitHash)
            }
            is ExecuteCommand.Result.Error -> {
                throw FirstCommitHashException(project, firstCommitHashResult)
            }
        }
    }

    companion object {

        private const val NO_TAG_FOUND_EXIT_CODE = 128
    }
}

internal class FirstCommitHashException(
    project: Project,
    error: ExecuteCommand.Result.Error,
) : GradleException() {

    override val message = "Getting first commit hash during check of project ${project.name} failed with $error"
}

internal class TagException(
    project: Project,
    error: ExecuteCommand.Result.Error,
) : GradleException() {

    override val message = "Getting release tag during check of project ${project.name} failed with $error"
}
