package io.github.ackeecz.guardian.verification.task

import io.github.ackeecz.guardian.util.Constants
import io.github.ackeecz.guardian.util.getTaskName
import io.github.ackeecz.guardian.verification.VerifyBomVersion
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.api.tasks.TaskAction

/**
 * Task for BOM version verification. For more information see [VerifyBomVersion].
 */
public abstract class VerifyBomVersionTask : DefaultTask() {

    private val verifyBomVersion = VerifyBomVersion()

    @TaskAction
    public fun verify() {
        when (val result = verifyBomVersion(project)) {
            VerifyBomVersion.Result.Success -> Unit
            is VerifyBomVersion.Result.Error -> {
                logger.error(result.message)
                throw GradleException(result.message)
            }
        }
    }

    public companion object {

        public val NAME: String = VerifyBomVersionTask::class.java.getTaskName()

        public fun registerFor(project: Project) {
            project.tasks.register(NAME, VerifyBomVersionTask::class.java) {
                group = Constants.ACKEE_TASKS_GROUP
                description = "Verifies that current BOM version matches the current tag version"
            }
        }
    }
}
