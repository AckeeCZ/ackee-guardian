plugins {
    alias(libs.plugins.ackeecz.security.android.library)
    alias(libs.plugins.ackeecz.security.publishing)
    alias(libs.plugins.ackeecz.security.testing)
    alias(libs.plugins.ackeecz.security.testing.android)
    alias(libs.plugins.ackeecz.security.tink)
}

android {
    namespace = "io.github.ackeecz.security.jetpack"
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
