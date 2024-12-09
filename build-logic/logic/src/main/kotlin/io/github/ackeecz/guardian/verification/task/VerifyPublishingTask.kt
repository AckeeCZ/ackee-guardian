package io.github.ackeecz.guardian.verification.task

import io.github.ackeecz.guardian.util.Constants
import io.github.ackeecz.guardian.util.getTaskName
import io.github.ackeecz.guardian.verification.VerifyPublishing
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.api.tasks.TaskAction

/**
 * Task for publishing verification. For more information see [VerifyPublishing].
 */
internal abstract class VerifyPublishingTask : DefaultTask() {

    private val verifyPublishing = VerifyPublishing()

    @TaskAction
    fun verify() {
        when (val result = verifyPublishing(project)) {
            VerifyPublishing.Result.Success -> Unit
            is VerifyPublishing.Result.Warning -> {
                logger.warn(result.message)
            }
            is VerifyPublishing.Result.Error -> {
                logger.error(result.message)
                throw GradleException(result.message)
            }
        }
    }

    companion object {

        val NAME = VerifyPublishingTask::class.java.getTaskName()

        fun registerFor(project: Project) {
            project.tasks.register(NAME, VerifyPublishingTask::class.java) {
                group = Constants.ACKEE_TASKS_GROUP
                description = "Verifies that all dependencies between this library artifacts are compatible"
            }
        }
    }
}
