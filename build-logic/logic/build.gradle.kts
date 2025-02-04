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
    compileOnly(files(libs::class.java.superclass.protectionDomain.codeSource.location))
    compileOnly(libs.android.gradlePlugin)
    compileOnly(libs.detekt.gradlePlugin)
    compileOnly(libs.kotlin.gradlePlugin)
    compileOnly(libs.mavenPublish.gradlePlugin)
    compileOnly(libs.protobuf.gradlePlugin)

    testImplementation(platform(libs.junit5.bom))
    testImplementation(libs.kotest.assertions.core)
    testImplementation(libs.kotest.framework.api)
    testImplementation(libs.kotest.framework.datatest)
    testRuntimeOnly(libs.kotest.runner.junit5)
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
