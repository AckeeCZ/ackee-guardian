plugins {
    alias(libs.plugins.ackeecz.guardian.android.library)
    alias(libs.plugins.ackeecz.guardian.publishing)
    alias(libs.plugins.ackeecz.guardian.testing)
    alias(libs.plugins.ackeecz.guardian.testing.android)
}

android {
    namespace = "io.github.ackeecz.guardian.datastore.preferences"
}

dependencies {

    api(projects.datastoreCore)
    implementation(projects.datastoreCoreInternal)

    api(libs.androidx.datastore.preferences)

    implementation(platform(libs.coroutines.bom))
    implementation(libs.coroutines.android)

    testImplementation(testFixtures(projects.datastoreCoreInternal))
}
