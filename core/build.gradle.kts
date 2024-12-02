plugins {
    id("ackee.security.android.library")
    id("ackee.security.publishing")
    id("ackee.security.testing")
    id("ackee.security.testing.android")
}

android {
    namespace = "io.github.ackeecz.security.core"
}

dependencies {

    implementation(libs.androidx.annotation)

    implementation(platform(libs.coroutines.bom))
    implementation(libs.coroutines.android)

    testImplementation(testFixtures(projects.coreInternal))
}
