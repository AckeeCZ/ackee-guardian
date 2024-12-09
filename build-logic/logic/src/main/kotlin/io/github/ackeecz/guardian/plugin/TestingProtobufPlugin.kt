package io.github.ackeecz.guardian.plugin

import com.google.protobuf.gradle.ProtobufExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies

/**
 * Sets up Protobuf but only for test source set to be used in tests
 */
internal class TestingProtobufPlugin : Plugin<Project> {

    override fun apply(target: Project) {
        target.configure()
    }

    private fun Project.configure() {
        pluginManager.apply(libs.plugins.protobuf)

        // Set up protobuf configuration, generating lite Java and Kotlin classes
        protobuf {
            protoc {
                artifact = libs.protobuf.protoc.get().toString()
            }
            generateProtoTasks {
                // We only need to configure protobuf for tests
                ofTest().forEach { task ->
                    task.builtins {
                        register("java") {
                            option("lite")
                        }
                        register("kotlin") {
                            option("lite")
                        }
                    }
                }
            }
        }

        dependencies {

            testImplementation(libs.protobuf.kotlin.lite)
        }
    }
}

private fun Project.protobuf(action: ProtobufExtension.() -> Unit) {
    extensions.configure(ProtobufExtension::class, action)
}
