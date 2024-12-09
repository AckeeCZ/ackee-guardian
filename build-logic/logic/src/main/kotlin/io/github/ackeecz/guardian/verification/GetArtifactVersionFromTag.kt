package io.github.ackeecz.guardian.verification

import io.github.ackeecz.guardian.properties.LibraryProperties
import io.github.ackeecz.guardian.util.ExecuteCommand
import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.internal.cc.base.logger
import org.jetbrains.annotations.VisibleForTesting
import java.util.Properties

/**
 * Returns artifact version of the current [Project] from the passed release tag.
 * [Project] needs to be publishable in order for this logic to succeed. If the returned version is
 * null, it means that the version does not exist, because the artifact was not released yet.
 */
internal interface GetArtifactVersionFromTag {

    operator fun invoke(project: Project, tagResult: TagResult): ArtifactVersion?

    companion object {

        operator fun invoke(): GetArtifactVersionFromTag {
            return GetArtifactVersionFromTagImpl(executeCommand = ExecuteCommand())
        }
    }
}

@VisibleForTesting
internal class GetArtifactVersionFromTagImpl(
    private val executeCommand: ExecuteCommand,
) : GetArtifactVersionFromTag {

    override fun invoke(project: Project, tagResult: TagResult): ArtifactVersion? {
        return Impl(project).invoke(tagResult)
    }

    private inner class Impl(private val project: Project) {

        fun invoke(tagResult: TagResult): ArtifactVersion? {
            return when (tagResult) {
                is TagResult.Tag -> getVersionFromTag(tagResult)
                is TagResult.FirstCommitHash -> handleFirstCommitHashResult()
            }
        }

        private fun getVersionFromTag(tagResult: TagResult.Tag): ArtifactVersion? {
            val command = "git show ${tagResult.value}:$PROPERTIES_FILE_NAME"
            when (val propertiesResult = executeCommand(command, project)) {
                is ExecuteCommand.Result.Success -> return parseVersionFromProperties(propertiesResult, tagResult)
                is ExecuteCommand.Result.Error -> throw TagPropertiesException(project, tagResult)
            }
        }

        private fun parseVersionFromProperties(
            propertiesResult: ExecuteCommand.Result.Success,
            tagResult: TagResult.Tag,
        ): ArtifactVersion? {
            val propertiesContent = propertiesResult.commandOutput
            logger.info("Loading properties file content from the tag ${tagResult.value}:\n$propertiesContent")
            val properties = Properties().also { it.load(propertiesResult.commandOutput.byteInputStream()) }
            return try {
                val version = LibraryProperties(properties, project).getArtifactProperties().version
                ArtifactVersion(version)
            } catch (e: IllegalArgumentException) {
                if (project.version.toString() == INITIAL_LIBRARY_VERSION) {
                    return null
                } else {
                    throw VersionUnparseableException(project, tagResult, e)
                }
            }
        }

        private fun handleFirstCommitHashResult(): ArtifactVersion? {
            val projectVersion = project.version.toString()
            if (projectVersion == INITIAL_LIBRARY_VERSION) {
                return null
            } else {
                throw UnexpectedInitialVersionException(project, projectVersion)
            }
        }
    }

    companion object {

        private const val PROPERTIES_FILE_NAME = "lib.properties"
        private const val INITIAL_LIBRARY_VERSION = "1.0.0"
    }
}

@JvmInline
internal value class ArtifactVersion(val value: String)

internal class VersionUnparseableException(
    project: Project,
    tag: TagResult.Tag,
    override val cause: Throwable?,
) : GradleException() {

    override val message = "Version of the artifact of the project ${project.name} could not have " +
        "been parsed from the tag ${tag.value}"
}

internal class TagPropertiesException(
    project: Project,
    tag: TagResult.Tag,
) : GradleException() {

    override val message = "Getting properties of the tag ${tag.value} failed for the project ${project.name}"
}

internal class UnexpectedInitialVersionException(
    project: Project,
    initialVersion: String,
) : GradleException() {

    override val message = "Initial version $initialVersion of the project ${project.name} is not as expected. " +
        "There is no release version tag so either the initial version is not as expected or the already created tags " +
        "are not in the expected format."
}
