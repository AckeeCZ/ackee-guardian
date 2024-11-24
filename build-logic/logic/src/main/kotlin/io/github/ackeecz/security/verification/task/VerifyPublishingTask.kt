package io.github.ackeecz.security.verification.task

import io.github.ackeecz.security.util.Constants
import io.github.ackeecz.security.util.getTaskName
import io.github.ackeecz.security.verification.VerifyPublishing
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

        fun registerFor(project: Project) {
            val taskClass = VerifyPublishingTask::class.java
            project.tasks.register(taskClass.getTaskName(), taskClass) {
                group = Constants.ACKEE_TASKS_GROUP
                description = "Verifies that all dependencies between this library artifacts are compatible"
            }
        }
    }
}
