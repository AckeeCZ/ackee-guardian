import io.github.ackeecz.security.properties.LibraryProperties
import io.github.ackeecz.security.util.Constants

plugins {
    alias(libs.plugins.ackeecz.security.android.application)
    alias(libs.plugins.ackeecz.security.testing)
    alias(libs.plugins.ackeecz.security.testing.android)
    alias(libs.plugins.ackeecz.security.testing.protobuf)
}

private val includeArtifactsTestsProperty = "includeTests"
private val artifactsTestsPackage = "io.github.ackeecz.security.sample.*"

android {
    namespace = "io.github.ackeecz.security.sample"

    defaultConfig {
        applicationId = "io.github.ackeecz.security"
    }

    @Suppress("UnstableApiUsage")
    testOptions {
        unitTests.all {
            it.filter {
                // By default (when property is not set) we exclude artifacts tests, because they rely
                // on artifacts to be published, so we do not want them to run together with all other
                // tests using classic Gradle test tasks like testDebugUnitTest. We want to run them
                // only in a special custom task that sets this property and run this task only under
                // certain special conditions, like during pre-publish check on published artifacts to
                // Maven local before real publishing.
                if (!project.hasProperty(includeArtifactsTestsProperty)) {
                    excludeTestsMatching(artifactsTestsPackage)
                    isFailOnNoMatchingTests = false
                }
            }
        }
    }
}

@Suppress("UseTomlInstead")
dependencies {

    val bomVersion = LibraryProperties(project).bomArtifactProperties.version
    implementation(platform("io.github.ackeecz:security-bom:$bomVersion"))
    implementation("io.github.ackeecz:security-core")
    implementation("io.github.ackeecz:security-datastore")
    implementation("io.github.ackeecz:security-datastore-preferences")
    implementation("io.github.ackeecz:security-jetpack")

    // Dependency on Tink must be included explicitly and should be even with explicit version. This
    // allows clients of Ackee Security library to control the version of Tink themselves, being
    // able to keep it up-to-date as much as possible and not depend on Ackee Security releases.
    implementation(libs.tink.android)

    testImplementation(libs.bouncyCastle.bcpkix)
}

/**
 * Tests published artifacts. This verifies things like correctly published artifacts including BOM
 * or binary compatibility of the dependent artifacts.
 */
tasks.register(Constants.ARTIFACTS_TESTS_TASK_NAME) {
    group = Constants.ACKEE_TASKS_GROUP
    description = "Tests published artifacts of the library"
    ext.set(includeArtifactsTestsProperty, true)
    dependsOn("testDebugUnitTest")
}
