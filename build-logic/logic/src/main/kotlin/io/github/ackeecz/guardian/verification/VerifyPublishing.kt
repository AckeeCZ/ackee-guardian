package io.github.ackeecz.guardian.verification

import io.github.ackeecz.guardian.util.PublishableProject
import org.gradle.api.Project
import org.gradle.process.ExecOperations
import org.jetbrains.annotations.VisibleForTesting

/**
 * Verifies that all dependencies between [Project] being checked and its dependent artifacts are
 * compatible and can be safely published. This includes checking if current project
 * changed and increased its version, if it has dependent modules. At the same time it checks if
 * the dependent modules increased their versions as well, so everything is properly updated once
 * released and there are no incompatible dependencies in upcoming published artifacts. This is
 * especially important for internal modules that do not have stable API, can contain breaking
 * changes and for example if they were released without all dependent artifacts being updated and
 * released as well, this could break compatibility for the library client at runtime, because there
 * could be a newer version of the internal artifact and older versions of all or some of the dependent
 * ones, causing runtime inconsistencies and crashes.
 *
 * Uses tag retrieved from [GetPreviousTag], because this task is mainly meant to be run on CI
 * during publishing process to verify publishing and it is needed to check the previous version
 * tag, because the current one (the latest) is the one that triggered the current process of releasing
 * a new version.
 */
internal class VerifyPublishing @VisibleForTesting constructor(
    private val getPreviousTag: GetPreviousTag,
    private val getArtifactVersionFromTag: GetArtifactVersionFromTag,
    private val getReleaseDependentProjects: GetReleaseDependentProjects,
    private val checkArtifactUpdateStatus: CheckArtifactUpdateStatus,
) {

    constructor(execOperations: ExecOperations) : this(
        getPreviousTag = GetPreviousTag(execOperations),
        getArtifactVersionFromTag = GetArtifactVersionFromTag(execOperations),
        getReleaseDependentProjects = GetReleaseDependentProjects(),
        checkArtifactUpdateStatus = CheckArtifactUpdateStatus(execOperations),
    )

    operator fun invoke(project: Project): Result {
        val dependentProjects = getDependentProjects(project)
        return Impl(project, dependentProjects).invoke()
    }

    private fun getDependentProjects(project: Project): List<Project> {
        val dependentProjects = getReleaseDependentProjects(project)
        val dataStoreProjectsNames = PublishableProject.DataStore.values().map { it.projectName }
        return if (dataStoreProjectsNames.contains(project.name)) {
            // If current project is a DataStore project, filter out all dependent DataStore projects.
            // We do not need (and want) to include them during dependent projects checks because
            // they are not relevant as version of all DataStore artifacts always changes together.
            dependentProjects.filterNot { dataStoreProjectsNames.contains(it.name) }
        } else {
            dependentProjects
        }
    }

    private inner class Impl(
        private val project: Project,
        private val dependentProjects: List<Project>,
    ) {

        private val previousTagResult = getPreviousTag(project)

        operator fun invoke(): Result {
            return if (project.isVersionSameSinceLastRelease()) {
                handleNotChangedVersion()
            } else {
                handleChangedVersion()
            }
        }

        private fun Project.isVersionSameSinceLastRelease(): Boolean {
            return getArtifactVersionFromTag(project, previousTagResult)?.value == project.version.toString()
        }

        private fun handleNotChangedVersion(): Result {
            return when (checkArtifactUpdateStatus(project, previousTagResult)) {
                ArtifactUpdateStatus.UP_TO_DATE -> Result.Success
                ArtifactUpdateStatus.UPDATE_NEEDED -> handleNeededUpdate()
            }
        }

        private fun handleNeededUpdate(): Result {
            return if (dependentProjects.isEmpty()) {
                CheckIfShouldUpdate(currentProject = project)
            } else {
                ArtifactVersionNotIncreased(
                    currentProject = project,
                    dependentProjects = dependentProjects,
                )
            }
        }

        private fun handleChangedVersion(): Result {
            return if (dependentProjects.isEmpty()) {
                Result.Success
            } else {
                checkDependentProjectsVersions()
            }
        }

        private fun checkDependentProjectsVersions(): Result {
            val dependentProjectsWithNotIncreasedVersion = dependentProjects.filter {
                it.isVersionSameSinceLastRelease()
            }
            return if (dependentProjectsWithNotIncreasedVersion.isEmpty()) {
                Result.Success
            } else {
                DependentProjectsOutdated(
                    currentProject = project,
                    outdatedDependentProjects = dependentProjectsWithNotIncreasedVersion,
                )
            }
        }
    }

    sealed interface Result {

        object Success : Result

        sealed interface Error : Result {

            val message: String
        }

        sealed interface Warning : Result {

            val message: String
        }
    }
}

internal data class DependentProjectsOutdated(
    val currentProject: Project,
    val outdatedDependentProjects: List<Project>,
) : VerifyPublishing.Result.Error {

    private val currentProjectName = ":${currentProject.name}"
    private val dependentProjectsMessage = outdatedDependentProjects.joinToString("\n") {
        "name=${it.name}, version=${it.version}"
    }

    override val message = "Following dependent modules on $currentProjectName need to be updated " +
        "because $currentProjectName was updated and increased its version:\n$dependentProjectsMessage\n\n" +
        "This protects the client of this library from runtime crashes caused by possible incompatible " +
        "released versions of artifacts under the same BOM."
}

internal data class ArtifactVersionNotIncreased(
    val currentProject: Project,
    val dependentProjects: List<Project>,
) : VerifyPublishing.Result.Error {

    private val currentProjectName = ":${currentProject.name}"
    private val dependentProjectsMessage = dependentProjects.joinToString { ":${it.name}" }

    override val message = "Module $currentProjectName changed since last release, but it " +
        "didn't increase its version ${currentProject.version} and it has dependent modules " +
        "($dependentProjectsMessage), that have independent versioning and releases. This is dangerous, " +
        "because dependent modules could adapt to changes of the $currentProjectName module and " +
        "release new version without $currentProjectName module being released. This can lead to " +
        "runtime incompatibility and crashes if there were breaking changes in the $currentProjectName " +
        "module, which is quite easily possible for \"internal\" modules."
}

internal data class CheckIfShouldUpdate(
    val currentProject: Project,
) : VerifyPublishing.Result.Warning {

    private val currentProjectName = ":${currentProject.name}"

    override val message = "Module $currentProjectName changed since last release, but it " +
        "didn't increase its version ${currentProject.version}. It does not have any dependent modules " +
        "(at least not with independent versioning and releases), so it is not dangerous to not " +
        "release it, but you should check if update is not planned/needed."
}
