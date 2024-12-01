import io.github.ackeecz.security.properties.LibraryProperties

plugins {
    alias(libs.plugins.ackeecz.security.android.application)
    alias(libs.plugins.ackeecz.security.testing)
    alias(libs.plugins.ackeecz.security.testing.android)
    alias(libs.plugins.ackeecz.security.testing.protobuf)
}

android {
    namespace = "io.github.ackeecz.security.sample"

    defaultConfig {
        applicationId = "io.github.ackeecz.security"
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
