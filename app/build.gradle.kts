import io.github.ackeecz.guardian.properties.LibraryProperties

plugins {
    alias(libs.plugins.ackeecz.guardian.android.application)
    alias(libs.plugins.ackeecz.guardian.testing)
    alias(libs.plugins.ackeecz.guardian.testing.android)
    alias(libs.plugins.ackeecz.guardian.testing.protobuf)
}

android {
    namespace = "io.github.ackeecz.guardian.sample"

    defaultConfig {
        applicationId = "io.github.ackeecz.guardian"
    }
}

@Suppress("UseTomlInstead")
dependencies {

    val bomVersion = LibraryProperties(project).bomArtifactProperties.version
    implementation(platform("io.github.ackeecz:guardian-bom:$bomVersion"))
    implementation("io.github.ackeecz:guardian-core")
    implementation("io.github.ackeecz:guardian-datastore")
    implementation("io.github.ackeecz:guardian-datastore-preferences")
    implementation("io.github.ackeecz:guardian-jetpack")

    // Dependency on Tink must be included explicitly and should be even with explicit version. This
    // allows clients of Ackee Guardian library to control the version of Tink themselves, being
    // able to keep it up-to-date as much as possible and not depend on Ackee Guardian releases.
    implementation(libs.tink.android)

    testImplementation(libs.bouncyCastle.bcpkix)
}
