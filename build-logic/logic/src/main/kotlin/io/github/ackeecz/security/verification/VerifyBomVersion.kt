package io.github.ackeecz.security.verification

import org.gradle.api.Project
import org.jetbrains.annotations.VisibleForTesting

/**
 * Verifies if the current BOM tag version matches the current BOM version to keep in sync BOM versions
 * with release tags and also enforce increase of BOM version during publishing process.
 */
internal class VerifyBomVersion @VisibleForTesting constructor(
    private val getCurrentTag: GetCurrentTag,
    private val getArtifactVersionFromTag: GetArtifactVersionFromTag,
) {

    constructor() : this(
        getCurrentTag = GetCurrentTag(),
        getArtifactVersionFromTag = GetArtifactVersionFromTag(),
    )

    operator fun invoke(project: Project): Result {
        return when (val result = getCurrentTag(project)) {
            is TagResult.Tag -> processTag(result, project)
            is TagResult.FirstCommitHash -> Result.Error.TagMissing
        }
    }

    private fun processTag(tagResult: TagResult.Tag, project: Project): Result {
        val tagValue = tagResult.value
        val bomArtifactVersion = getArtifactVersionFromTag(project, tagResult)
        val currentTagVersion = tagValue.substringAfter(
            delimiter = GetTag.BOM_VERSION_TAG_PREFIX,
            missingDelimiterValue = MISSING_TAG_PREFIX_FALLBACK,
        )
        return when (currentTagVersion) {
            bomArtifactVersion?.value -> Result.Success
            MISSING_TAG_PREFIX_FALLBACK -> Result.Error.UnexpectedTagFormat(tagValue)
            else -> Result.Error.NotMatchingVersion(
                bomArtifactVersion = bomArtifactVersion,
                currentTagVersion = currentTagVersion,
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
                val currentTagVersion: String,
            ) : Error {

                override val message = "BOM artifact version (${bomArtifactVersion?.value}) and current tag version " +
                    "($currentTagVersion) do not match. You probably forgot to increase BOM version " +
                    "or you created a tag with incorrect version."
            }

            data class UnexpectedTagFormat(val tag: String) : Error {

                override val message = "BOM tag has unexpected format. Expected '$EXPECTED_TAG_FORMAT' but was '$tag'"
            }

            object TagMissing : Error {

                override val message = "BOM tag in format $EXPECTED_TAG_FORMAT is missing in Git history"
            }
        }

        companion object {

            private const val EXPECTED_TAG_FORMAT = "${GetTag.BOM_VERSION_TAG_PREFIX}*"
        }
    }
}
