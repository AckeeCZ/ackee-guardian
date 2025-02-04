package io.github.ackeecz.guardian.plugin

import io.gitlab.arturbosch.detekt.extensions.DetektExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.MinimalExternalModuleDependency
import org.gradle.api.provider.Provider
import org.gradle.kotlin.dsl.DependencyHandlerScope
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies

internal class DetektPlugin : Plugin<Project> {

    override fun apply(target: Project) {
        target.configure()
    }

    private fun Project.configure() {
        pluginManager.apply(libs.plugins.detekt)

        detekt {
            buildUponDefaultConfig = true
            config.setFrom(
                files("$rootDir/detekt-config.yml")
            )
            ignoreFailures = false
        }

        dependencies {
            detektPlugins(libs.detekt.formatting)
        }
    }

    private fun Project.detekt(action: DetektExtension.() -> Unit) {
        extensions.configure(DetektExtension::class, action)
    }

    private fun DependencyHandlerScope.detektPlugins(provider: Provider<MinimalExternalModuleDependency>) {
        add("detektPlugins", provider.get())
    }
}
