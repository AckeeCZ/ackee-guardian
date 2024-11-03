package io.github.ackeecz.security.plugin

import org.gradle.api.Plugin
import org.gradle.api.Project

internal class AndroidLibraryPlugin : Plugin<Project> {

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
        pluginManager.apply(libs.findPlugin("android.library"))
        pluginManager.apply(libs.findPlugin("kotlin.android"))

        androidBase {
            defaultConfig {
                consumerProguardFiles("consumer-rules.pro")
            }
        }
    }
}
