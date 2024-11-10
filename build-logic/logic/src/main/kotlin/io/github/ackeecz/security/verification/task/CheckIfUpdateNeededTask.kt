package io.github.ackeecz.security.verification.task

import io.github.ackeecz.security.util.getTaskName
import io.github.ackeecz.security.verification.ArtifactUpdateStatus
import io.github.ackeecz.security.verification.CheckArtifactUpdateStatus
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.api.tasks.TaskAction

/**
 * Checks if the artifact has changed and needs to be updated (new version published). Succeeds
 * if the artifact is up-to-date and fails if it needs to be updated.
 */
internal abstract class CheckIfUpdateNeededTask : DefaultTask() {

    private val checkArtifactUpdateStatus = CheckArtifactUpdateStatus()

    @TaskAction
    fun executeCheck() {
        when (checkArtifactUpdateStatus(project)) {
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
            val taskClass = CheckIfUpdateNeededTask::class.java
            project.tasks.register(taskClass.getTaskName(), taskClass) {
                group = "verification"
                description = "Checks if the artifact has changed and needs to be updated (new version published)"
            }
        }
    }
}
