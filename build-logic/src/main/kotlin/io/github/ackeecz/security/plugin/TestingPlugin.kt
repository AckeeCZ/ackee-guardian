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
            testImplementation(platform(libs.findLibrary("coroutines.bom").get()))
            testImplementation(libs.findLibrary("coroutines.test"))

            // JUnit
            testImplementation(platform(libs.findLibrary("junit5.bom").get()))
            testRuntimeOnly(libs.findLibrary("junit.vintage.engine"))

            // Kotest
            testImplementation(libs.findLibrary("kotest.assertions.core"))
            testImplementation(libs.findLibrary("kotest.framework.api"))
            testRuntimeOnly(libs.findLibrary("kotest.runner.junit5"))
        }
    }
}
