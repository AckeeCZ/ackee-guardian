plugins {
    `kotlin-dsl`
}

dependencies {
    compileOnly(libs.android.gradlePlugin)
    compileOnly(libs.detekt.gradlePlugin)
    compileOnly(libs.kotlin.gradlePlugin)
    compileOnly(libs.mavenPublish.gradlePlugin)
}

gradlePlugin {
    plugins {
        val basePluginId = "ackee.security"
        val pluginPackage = "io.github.ackeecz.security.plugin"

        register("android-application") {
            id = "$basePluginId.android.application"
            implementationClass = "$pluginPackage.AndroidApplicationPlugin"
        }

        register("android-library") {
            id = "$basePluginId.android.library"
            implementationClass = "$pluginPackage.AndroidLibraryPlugin"
        }

        register("publishing") {
            id = "$basePluginId.publishing"
            implementationClass = "$pluginPackage.PublishingPlugin"
        }

        register("test-fixtures") {
            id = "$basePluginId.testfixtures"
            implementationClass = "$pluginPackage.TestFixturesPlugin"
        }

        register("testing") {
            id = "$basePluginId.testing"
            implementationClass = "$pluginPackage.TestingPlugin"
        }

        register("testing-android") {
            id = "$basePluginId.testing.android"
            implementationClass = "$pluginPackage.TestingAndroidPlugin"
        }
    }
}
