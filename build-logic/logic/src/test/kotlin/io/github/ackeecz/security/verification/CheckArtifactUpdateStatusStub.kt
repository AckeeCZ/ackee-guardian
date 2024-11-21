package io.github.ackeecz.security.verification

import org.gradle.api.Project

internal class CheckArtifactUpdateStatusStub : CheckArtifactUpdateStatus {

    var artifactUpdateStatus: ArtifactUpdateStatus = ArtifactUpdateStatus.UPDATE_NEEDED

    override fun invoke(project: Project): ArtifactUpdateStatus = artifactUpdateStatus
}
