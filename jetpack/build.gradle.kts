plugins {
    alias(libs.plugins.ackeecz.security.android.library)
    alias(libs.plugins.ackeecz.security.publishing)
    alias(libs.plugins.ackeecz.security.testing)
    alias(libs.plugins.ackeecz.security.testing.android)
}

android {
    namespace = "io.github.ackeecz.security.jetpack"
}

dependencies {

    api(projects.core)
    implementation(projects.coreInternal)

    implementation(libs.androidx.annotation)
    implementation(libs.androidx.collectionKtx)

    implementation(platform(libs.coroutines.bom))
    implementation(libs.coroutines.android)

    // TODO Change to compile-only
    implementation(libs.tink.android)

    testImplementation(testFixtures(projects.coreInternal))
    testImplementation(libs.androidx.coreKtx)
}
