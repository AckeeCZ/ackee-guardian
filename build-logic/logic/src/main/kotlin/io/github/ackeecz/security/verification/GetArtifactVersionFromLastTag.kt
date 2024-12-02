package io.github.ackeecz.security.verification

import io.github.ackeecz.security.properties.LibraryProperties
import io.github.ackeecz.security.util.ExecuteCommand
import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.internal.cc.base.logger
import java.util.Properties

/**
 * Returns artifact version of the current [Project] from the last release tag.
 * [Project] needs to be publishable in order for this logic to succeed. If the returned version is
 * null, it means that the version does not exist, because the artifact was not released yet.
 */
internal interface GetArtifactVersionFromLastTag {

    operator fun invoke(project: Project): ArtifactVersion?

    companion object {

        operator fun invoke(): GetArtifactVersionFromLastTag {
            return GetArtifactVersionFromLastTagImpl(
                getLastTag = GetLastTag(),
                executeCommand = ExecuteCommand(),
            )
        }
    }
}

internal class GetArtifactVersionFromLastTagImpl(
    private val getLastTag: GetLastTag,
    private val executeCommand: ExecuteCommand,
) : GetArtifactVersionFromLastTag {

    override fun invoke(project: Project): ArtifactVersion? {
        return Impl(project).invoke()
    }

    private inner class Impl(private val project: Project) {

        fun invoke(): ArtifactVersion? {
            return when (val lastTagResult = getLastTag(project)) {
                is LastTagResult.Tag -> getVersionFromLastTag(lastTagResult)
                is LastTagResult.FirstCommitHash -> handleFirstCommitHashResult()
            }
        }

        private fun getVersionFromLastTag(lastTagResult: LastTagResult.Tag): ArtifactVersion? {
            val propertiesResult = executeCommand("git show ${lastTagResult.value}:$PROPERTIES_FILE_NAME", project)
            when (propertiesResult) {
                is ExecuteCommand.Result.Success -> return parseVersionFromProperties(propertiesResult, lastTagResult)
                is ExecuteCommand.Result.Error -> throw LastTagPropertiesException(project, lastTagResult)
            }
        }

        private fun parseVersionFromProperties(
            propertiesResult: ExecuteCommand.Result.Success,
            lastTagResult: LastTagResult.Tag,
        ): ArtifactVersion? {
            val propertiesContent = propertiesResult.commandOutput
            logger.info("Loading properties file content from the tag ${lastTagResult.value}:\n$propertiesContent")
            val properties = Properties().also { it.load(propertiesResult.commandOutput.byteInputStream()) }
            return try {
                val lastTagVersion = LibraryProperties(properties, project).getArtifactProperties().version
                ArtifactVersion(lastTagVersion)
            } catch (e: IllegalArgumentException) {
                if (project.version.toString() == INITIAL_LIBRARY_VERSION) {
                    return null
                } else {
                    throw VersionUnparseableException(project, e)
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
    override val cause: Throwable?,
) : GradleException() {

    override val message = "Version of the artifact of the project ${project.name} could not have been parsed from the last tag"
}

internal class LastTagPropertiesException(
    project: Project,
    lastTag: LastTagResult.Tag,
) : GradleException() {

    override val message = "Getting properties of last tag ${lastTag.value} failed for the project ${project.name}"
}

internal class UnexpectedInitialVersionException(
    project: Project,
    initialVersion: String,
) : GradleException() {

    override val message = "Initial version $initialVersion of the project ${project.name} is not as expected. " +
        "There is no release version tag so either the initial version is not as expected or the already created tags " +
        "are not in the expected format."
}
