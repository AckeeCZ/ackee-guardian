package io.github.ackeecz.guardian.plugin

import org.gradle.api.Plugin
import org.gradle.api.Project

internal class TestFixturesPlugin : Plugin<Project> {

    override fun apply(target: Project) {
        target.configure()
    }

    private fun Project.configure() {
        androidLibrary {

            // TODO test fixtures source code ignores Kotlin compiler options declared in KotlinPlugin
            //  convention plugin for some reason. Try to figure out why.
            @Suppress("UnstableApiUsage")
            testFixtures {
                enable = true
            }
        }
    }
}
