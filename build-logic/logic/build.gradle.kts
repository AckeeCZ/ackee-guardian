import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinVersion
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    `kotlin-dsl`
}

tasks.withType<KotlinCompile>().configureEach {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
        freeCompilerArgs.add("-Xexplicit-api=strict")
    }
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

// TODO This workarounds issues with incompatibility between Kotest and Gradle. Kotest is now
//  compiled with Kotlin 2.2.0 and even though Gradle 9.0.0 has embedded Kotlin of version 2.2.0 as well,
//  it fails tests compilation because it apparently compiles it with Kotlin compiler 2.0.0, which
//  does not understand compiled libraries with 2.2.0. There was also a warning that language level 1.8
//  is deprecated and will be removed in future versions of Kotlin. Seems like Gradle has this set to
//  1.8 internally and maybe the newest compiler can't compile it anymore, so it fallbacks to older
//  compiler version to compile the code, but then it clashes with Kotest? Increasing language version
//  to 2.2 fixes the issue, so it looks like this or some similar issue. Try to remove this workaround
//  with newer versions of Gradle than 9.0.0.
tasks.withType<KotlinCompile>()
    // Apply to test compilation tasks only to let the build logic src to be compiled with Gradle's
    // settings.
    .matching { it.name.contains("Test") }
    .configureEach {
        compilerOptions {
            languageVersion.set(KotlinVersion.KOTLIN_2_2)
            apiVersion.set(KotlinVersion.KOTLIN_2_2)
        }
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}

dependencies {
    compileOnly(files(libs::class.java.superclass.protectionDomain.codeSource.location))
    compileOnly(libs.android.gradlePlugin)
    compileOnly(libs.detekt.gradlePlugin)
    compileOnly(libs.kotlin.gradlePlugin)
    compileOnly(libs.mavenPublish.gradlePlugin)
    compileOnly(libs.protobuf.gradlePlugin)

    testImplementation(platform(libs.junit5.bom))
    testImplementation(libs.kotest.assertions.core)
    testImplementation(libs.kotest.runner.junit5)
}

gradlePlugin {
    plugins {
        plugin(
            dependency = libs.plugins.ackeecz.guardian.android.application,
            pluginClassSimpleName = "AndroidApplicationPlugin",
        )

        plugin(
            dependency = libs.plugins.ackeecz.guardian.android.library,
            pluginClassSimpleName = "AndroidLibraryPlugin",
        )

        plugin(
            dependency = libs.plugins.ackeecz.guardian.preflightchecks,
            pluginClassSimpleName = "RegisterPreflightChecksPlugin",
        )

        plugin(
            dependency = libs.plugins.ackeecz.guardian.publishing,
            pluginClassSimpleName = "PublishingPlugin",
        )

        plugin(
            dependency = libs.plugins.ackeecz.guardian.testfixtures,
            pluginClassSimpleName = "TestFixturesPlugin",
        )

        plugin(
            dependency = libs.plugins.ackeecz.guardian.testing.asProvider(),
            pluginClassSimpleName = "TestingPlugin",
        )

        plugin(
            dependency = libs.plugins.ackeecz.guardian.testing.android,
            pluginClassSimpleName = "TestingAndroidPlugin",
        )

        plugin(
            dependency = libs.plugins.ackeecz.guardian.testing.protobuf,
            pluginClassSimpleName = "TestingProtobufPlugin",
        )

        plugin(
            dependency = libs.plugins.ackeecz.guardian.tink,
            pluginClassSimpleName = "TinkPlugin",
        )
    }
}

private fun NamedDomainObjectContainer<PluginDeclaration>.plugin(
    dependency: Provider<out PluginDependency>,
    pluginClassSimpleName: String,
) {
    val pluginId = dependency.get().pluginId
    register(pluginId) {
        id = pluginId
        implementationClass = "io.github.ackeecz.guardian.plugin.$pluginClassSimpleName"
    }
}
