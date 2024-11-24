package io.github.ackeecz.security.verification.task

import io.github.ackeecz.security.util.Constants
import io.github.ackeecz.security.util.getTaskName
import io.github.ackeecz.security.verification.ArtifactUpdateStatus
import io.github.ackeecz.security.verification.CheckArtifactUpdateStatus
import io.github.ackeecz.security.verification.GetCurrentTag
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.api.tasks.TaskAction

/**
 * Checks if the artifact has changed and needs to be updated (new version published) since the
 * current tag. Succeeds if the artifact is up-to-date and fails if it needs to be updated.
 *
 * This task is just a helper task and only meant for ad-hoc checks during development or when
 * preparing for release to check which artifacts changed and might need to update. It checks the
 * current artifact state against the current (latest) release tag, so running this task once the tag
 * for the new upcoming version was created is pointless, because it will always succeeds due to no
 * changes. So you should rather use this task to verify what changed and needs updates and then proceed
 * to increase appropriate artifact versions and start release process. There is a different task
 * [VerifyPublishingTask] that is able to do similar checks to this task, but takes the previous
 * tag instead of the current one, so it can handle newly created tag for the current in-progress
 * release properly.
 */
internal abstract class CheckIfUpdateNeededSinceCurrentTagTask : DefaultTask() {

    private val getCurrentTag = GetCurrentTag()
    private val checkArtifactUpdateStatus = CheckArtifactUpdateStatus()

    @TaskAction
    fun executeCheck() {
        val currentTagResult = getCurrentTag(project)
        when (checkArtifactUpdateStatus(project, currentTagResult)) {
            ArtifactUpdateStatus.UP_TO_DATE -> {
                logger.info("Artifact is up-to-date. Update is not needed.")
            }
            ArtifactUpdateStatus.UPDATE_NEEDED -> {
                val message = "Artifact has changed. You should publish a new version."
                logger.warn(message)
                throw GradleException(message)
            }
        }
    }

    companion object {

        fun registerFor(project: Project) {
            val taskClass = CheckIfUpdateNeededSinceCurrentTagTask::class.java
            project.tasks.register(taskClass.getTaskName(), taskClass) {
                group = Constants.ACKEE_TASKS_GROUP
                description = "Checks if the artifact has changed since the current release tag and needs to be updated (new version published)"
            }
        }
    }
}
