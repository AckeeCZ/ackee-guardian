package io.github.ackeecz.security.plugin

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.dependencies

internal class TinkPlugin : Plugin<Project> {

    override fun apply(target: Project) {
        target.configure()
    }

    private fun Project.configure() {
        dependencies {
            compileOnly(libs.tink.android)
        }
    }
}
