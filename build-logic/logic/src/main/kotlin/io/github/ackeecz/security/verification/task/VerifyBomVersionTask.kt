package io.github.ackeecz.security.verification.task

import io.github.ackeecz.security.util.getTaskName
import io.github.ackeecz.security.verification.VerifyBomVersion
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

        public fun registerFor(project: Project) {
            val taskClass = VerifyBomVersionTask::class.java
            project.tasks.register(taskClass.getTaskName(), taskClass) {
                group = "verification"
                description = "Verifies that current BOM version matches the current tag version"
            }
        }
    }
}
