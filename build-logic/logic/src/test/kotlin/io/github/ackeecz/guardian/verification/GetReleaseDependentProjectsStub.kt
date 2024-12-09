package io.github.ackeecz.guardian.verification

import org.gradle.api.Project

internal class GetReleaseDependentProjectsStub : GetReleaseDependentProjects {

    var dependentProjects: List<Project> = emptyList()

    override fun invoke(project: Project): List<Project> = dependentProjects
}
