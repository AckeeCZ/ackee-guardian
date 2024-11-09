plugins {
    id("ackee.security.android.library")
    id("ackee.security.publishing")
    id("ackee.security.testing")
    id("ackee.security.testing.android")
}

android {
    namespace = "io.github.ackeecz.security.datastore.preferences"
}

dependencies {

    api(projects.datastoreCore)
    implementation(projects.datastoreCoreInternal)

    api(libs.androidx.datastore.preferences)

    implementation(platform(libs.coroutines.bom))
    implementation(libs.coroutines.android)

    testImplementation(testFixtures(projects.datastoreCoreInternal))
}
