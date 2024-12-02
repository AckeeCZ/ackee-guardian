package io.github.ackeecz.security.verification

import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.ConfigurationContainer
import org.gradle.api.artifacts.ProjectDependency

/**
 * Returns list of projects that depend on passed [Project] but only using release configurations
 * like "implementation", "api", "releaseImplementation", etc. that are relevant for publishing.
 */
internal interface GetReleaseDependentProjects {

    operator fun invoke(project: Project): List<Project>

    companion object {

        operator fun invoke(): GetReleaseDependentProjects {
            return GetReleaseDependentProjectsImpl()
        }
    }
}

private class GetReleaseDependentProjectsImpl : GetReleaseDependentProjects {

    override fun invoke(project: Project): List<Project> {
        return project.rootProject
            .getWholeHierarchyChildProjects()
            .filterNot { it.path == project.path } // Filter out current project from check
            .filter { child ->
                val dependencyOnTargetProject = child.configurations
                    .filterAcceptableConfigurations()
                    .asSequence()
                    .flatMap { it.dependencies }
                    .filterIsInstance<ProjectDependency>()
                    .distinctBy { it.dependencyProject.path }
                    .find { it.dependencyProject.path == project.path }
                dependencyOnTargetProject != null
            }
    }

    private fun Project.getWholeHierarchyChildProjects(): List<Project> {
        return childProjects.values + childProjects.values.flatMap { it.getWholeHierarchyChildProjects() }
    }

    private fun ConfigurationContainer.filterAcceptableConfigurations(): List<Configuration> {
        return filter {
            ACCEPTABLE_CONFIGURATIONS.contains(it.name) || it.name.startsWith(RELEASE_CONFIGURATION_PREFIX)
        }
    }

    companion object {

        private val ACCEPTABLE_CONFIGURATIONS = listOf(
            "api",
            "compileOnly",
            "compileOnlyApi",
            "implementation",
            "runtimeOnly",
        )

        private const val RELEASE_CONFIGURATION_PREFIX = "release"
    }
}
