package io.github.ackeecz.security.plugin

import com.android.build.gradle.BaseExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.dependencies

/**
 * Contains testing build logic common to any module that contains any tests
 */
internal class TestingPlugin : Plugin<Project> {

    override fun apply(target: Project) {
        target.configure()
    }

    private fun Project.configure() {
        pluginManager.apply(libs.plugins.gradle.testLogger)

        androidBase {
            configureTestOptions()
        }
        applyDependencies()
    }

    private fun BaseExtension.configureTestOptions() {
        testOptions {
            animationsDisabled = true
            unitTests.all { it.useJUnitPlatform() }
        }
    }

    private fun Project.applyDependencies() {
        dependencies {

            // Coroutines
            testImplementation(platform(libs.coroutines.bom))
            testImplementation(libs.coroutines.test)

            // JUnit
            testImplementation(platform(libs.junit5.bom))
            testRuntimeOnly(libs.junit.vintage.engine)

            // Kotest
            testImplementation(libs.kotest.assertions.core)
            testImplementation(libs.kotest.framework.api)
            testImplementation(libs.kotest.framework.datatest)
            testRuntimeOnly(libs.kotest.runner.junit5)
        }
    }
}
