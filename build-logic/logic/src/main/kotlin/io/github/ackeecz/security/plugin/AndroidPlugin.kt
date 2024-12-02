package io.github.ackeecz.security.plugin

import com.android.build.gradle.BaseExtension
import io.github.ackeecz.security.util.Constants
import org.gradle.api.Plugin
import org.gradle.api.Project

internal class AndroidPlugin : Plugin<Project> {

    override fun apply(target: Project) {
        target.configure()
    }

    private fun Project.configure() {
        androidBase {
            configureSdkVersions()
            configureCompileOptions()
        }
    }

    private fun BaseExtension.configureSdkVersions() {
        compileSdkVersion(Constants.COMPILE_SDK)
        defaultConfig {
            minSdk = Constants.MIN_SDK
        }
    }

    private fun BaseExtension.configureCompileOptions() {
        compileOptions {
            sourceCompatibility = Constants.JAVA_VERSION
            targetCompatibility = Constants.JAVA_VERSION
        }
    }
}
