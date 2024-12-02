package io.github.ackeecz.security.plugin

import io.github.ackeecz.security.Constants
import org.gradle.api.Plugin
import org.gradle.api.Project

internal class AndroidApplicationPlugin : Plugin<Project> {

    private val androidPlugin = AndroidPlugin()
    private val detektPlugin = DetektPlugin()
    private val kotlinPlugin = KotlinPlugin()

    override fun apply(target: Project) {
        target.configure()
        androidPlugin.apply(target)
        detektPlugin.apply(target)
        kotlinPlugin.apply(target)
    }

    private fun Project.configure() {
        pluginManager.apply(libs.findPlugin("android.application"))
        pluginManager.apply(libs.findPlugin("kotlin.android"))

        androidApp {
            compileSdk = Constants.COMPILE_SDK

            defaultConfig {
                minSdk = Constants.MIN_SDK
                targetSdk = Constants.TARGET_SDK
                versionCode = 1
                versionName = "1.0"
            }

            buildTypes {
                release {
                    isMinifyEnabled = true
                    proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
                }
            }
        }
    }
}