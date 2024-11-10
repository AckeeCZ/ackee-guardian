import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    `kotlin-dsl`
}

kotlin {
    tasks.withType<KotlinCompile>().configureEach {
        compilerOptions {
            freeCompilerArgs.addAll(
                "-Xexplicit-api=strict",
            )
        }
    }
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}

dependencies {
    compileOnly(libs.android.gradlePlugin)
    compileOnly(libs.detekt.gradlePlugin)
    compileOnly(libs.kotlin.gradlePlugin)
    compileOnly(libs.mavenPublish.gradlePlugin)

    testImplementation(platform(libs.junit5.bom))
    testImplementation(libs.kotest.assertions.core)
    testImplementation(libs.kotest.framework.api)
    testImplementation(libs.kotest.framework.datatest)
    testRuntimeOnly(libs.kotest.runner.junit5)
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
