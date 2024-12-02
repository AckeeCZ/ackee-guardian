plugins {
    id("ackee.security.android.library")
    id("ackee.security.publishing")
    id("ackee.security.testing")
    id("ackee.security.testing.android")
}

android {
    namespace = "io.github.ackeecz.security.jetpack"
}

dependencies {

    // TODO This will need to by probably API to expose it to clients as well
    implementation(projects.core)
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
