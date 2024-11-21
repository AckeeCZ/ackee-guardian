package io.github.ackeecz.security.verification

import org.gradle.api.Project

internal class GetArtifactVersionFromLastTagStub : GetArtifactVersionFromLastTag {

    private val projectsVersions: MutableMap<String, ArtifactVersion?> = mutableMapOf()

    private val Project.id get() = path

    fun setProjectVersion(project: Project, version: ArtifactVersion?) {
        projectsVersions[project.id] = version
    }

    override fun invoke(project: Project): ArtifactVersion? = projectsVersions[project.id]
}
