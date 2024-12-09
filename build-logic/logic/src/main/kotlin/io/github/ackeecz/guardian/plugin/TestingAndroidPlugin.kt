package io.github.ackeecz.guardian.plugin

import com.android.build.gradle.BaseExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.dependencies

/**
 * Contains testing build logic common to any module that contains Android tests, i.e. tests that
 * contain Android framework dependencies.
 */
internal class TestingAndroidPlugin : Plugin<Project> {

    override fun apply(target: Project) {
        target.configure()
    }

    private fun Project.configure() {
        androidBase {
            configureDefaultConfig()
        }
        applyDependencies()
    }

    private fun BaseExtension.configureDefaultConfig() {
        defaultConfig {
            testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        }
    }

    private fun Project.applyDependencies() {
        dependencies {

            testImplementation(libs.androidx.test.junitKtx)
            testImplementation(libs.junit4)
            testImplementation(libs.robolectric)
        }
    }
}
