plugins {
    alias(libs.plugins.ackeecz.guardian.android.library)
    alias(libs.plugins.ackeecz.guardian.publishing)
    alias(libs.plugins.ackeecz.guardian.testing)
    alias(libs.plugins.ackeecz.guardian.testing.android)
    alias(libs.plugins.ackeecz.guardian.tink)
}

android {
    namespace = "io.github.ackeecz.guardian.jetpack"
}

dependencies {

    api(projects.core)
    implementation(projects.coreInternal)

    implementation(libs.androidx.annotation)
    implementation(libs.androidx.collection)

    implementation(platform(libs.coroutines.bom))
    implementation(libs.coroutines.android)

    testImplementation(testFixtures(projects.coreInternal))
    testImplementation(libs.androidx.coreKtx)
    testImplementation(libs.tink.android)
}
