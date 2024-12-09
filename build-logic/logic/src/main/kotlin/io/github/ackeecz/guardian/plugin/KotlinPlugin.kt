package io.github.ackeecz.guardian.plugin

import io.github.ackeecz.guardian.util.Constants
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.withType
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

internal class KotlinPlugin : Plugin<Project> {

    override fun apply(target: Project) {
        target.configure()
    }

    private fun Project.configure() {
        tasks.withType<KotlinCompile>().configureEach {
            compilerOptions {
                jvmTarget.set(Constants.JVM_TARGET)
                allWarningsAsErrors.set(true)
                freeCompilerArgs.addAll(
                    "-opt-in=kotlin.ExperimentalStdlibApi",
                    "-opt-in=kotlin.RequiresOptIn",
                    "-opt-in=kotlinx.coroutines.ExperimentalCoroutinesApi",
                    "-Xexplicit-api=strict",
                    "-Xconsistent-data-class-copy-visibility"
                )
            }
        }
    }
}
