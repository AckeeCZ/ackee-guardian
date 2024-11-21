package io.github.ackeecz.security.verification

import org.gradle.api.Project

/**
 * Verifies if the last BOM tag version matches the current BOM version to keep in sync BOM versions
 * with release tags and also enforce increase of BOM version during publishing process.
 */
internal class VerifyBomVersion(
    private val getLastTag: GetLastTag,
    private val getArtifactVersionFromLastTag: GetArtifactVersionFromLastTag,
) {

    constructor() : this(
        getLastTag = GetLastTag(),
        getArtifactVersionFromLastTag = GetArtifactVersionFromLastTag(),
    )

    operator fun invoke(project: Project): Result {
        return when (val result = getLastTag(project)) {
            is LastTagResult.Tag -> processTag(result, project)
            is LastTagResult.FirstCommitHash -> Result.Error.TagMissing
        }
    }

    private fun processTag(result: LastTagResult.Tag, project: Project): Result {
        val lastTag = result.value
        val bomArtifactVersion = getArtifactVersionFromLastTag(project)
        val lastTagVersion = lastTag.substringAfter(
            delimiter = GetLastTag.BOM_VERSION_TAG_PREFIX,
            missingDelimiterValue = MISSING_TAG_PREFIX_FALLBACK,
        )
        return when (lastTagVersion) {
            bomArtifactVersion?.value -> Result.Success
            MISSING_TAG_PREFIX_FALLBACK -> Result.Error.UnexpectedTagFormat(lastTag)
            else -> Result.Error.NotMatchingVersion(
                bomArtifactVersion = bomArtifactVersion,
                lastTagVersion = lastTagVersion,
            )
        }
    }

    companion object {

        private const val MISSING_TAG_PREFIX_FALLBACK = ""
    }

    sealed interface Result {

        object Success : Result

        sealed interface Error : Result {

            val message: String

            data class NotMatchingVersion(
                val bomArtifactVersion: ArtifactVersion?,
                val lastTagVersion: String,
            ) : Error {

                override val message = "BOM artifact version (${bomArtifactVersion?.value}) and last tag version " +
                    "($lastTagVersion) do not match. You probably forgot to increase BOM version " +
                    "or you created a tag with incorrect version."
            }

            data class UnexpectedTagFormat(val tag: String) : Error {

                override val message = "BOM tag has unexpected format: $tag"
            }

            object TagMissing : Error {

                override val message = "BOM tag in format ${GetLastTag.BOM_VERSION_TAG_PREFIX}* is missing in Git history"
            }
        }
    }
}
