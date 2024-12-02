package io.github.ackeecz.security.verification

import org.gradle.api.Project

internal class GetArtifactVersionFromTagStub : GetArtifactVersionFromTag {

    private val projectsVersions: MutableMap<String, ArtifactVersion?> = mutableMapOf()

    private val Project.id get() = path

    var receivedTagResult: TagResult? = null
        private set

    fun setProjectVersion(project: Project, version: ArtifactVersion?) {
        projectsVersions[project.id] = version
    }

    override fun invoke(
        project: Project,
        tagResult: TagResult,
    ): ArtifactVersion? {
        receivedTagResult = tagResult
        return projectsVersions[project.id]
    }
}
