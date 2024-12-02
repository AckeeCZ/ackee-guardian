import org.gradle.kotlin.dsl.android
import org.gradle.kotlin.dsl.protobuf

plugins {
    id("ackee.security.android.library")
    id("ackee.security.publishing")
    id("ackee.security.testing")
    id("ackee.security.testing.android")
    alias(libs.plugins.protobuf)
}

android {
    namespace = "io.github.ackeecz.security.datastore"
}

dependencies {

    api(projects.datastoreCore)
    implementation(projects.datastoreCoreInternal)

    implementation(platform(libs.coroutines.bom))
    implementation(libs.coroutines.android)

    api(libs.androidx.datastore)

    testImplementation(testFixtures(projects.datastoreCoreInternal))
    testImplementation(libs.protobuf.kotlin.lite)
}

// Setup protobuf configuration, generating lite Java and Kotlin classes
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

// TODO Seems like this can be removed and still be able see generated classes
androidComponents {
    beforeVariants(selector = selector()) {
        android.sourceSets.named(it.name) {
            val buildDir = layout.buildDirectory.get().asFile
            val baseProtoPath = "generated/source/proto/${it.name}"
            java.srcDir(buildDir.resolve("$baseProtoPath/java"))
            kotlin.srcDir(buildDir.resolve("$baseProtoPath/kotlin"))
            kotlin.srcDir("src/main/java")
        }
    }
}
