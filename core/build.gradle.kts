plugins {
    alias(libs.plugins.ackeecz.guardian.android.library)
    alias(libs.plugins.ackeecz.guardian.publishing)
    alias(libs.plugins.ackeecz.guardian.testing)
    alias(libs.plugins.ackeecz.guardian.testing.android)
}

android {
    namespace = "io.github.ackeecz.guardian.core"
}

dependencies {

    implementation(projects.coreInternal)

    implementation(libs.androidx.annotation)

    implementation(platform(libs.coroutines.bom))
    implementation(libs.coroutines.android)

    testImplementation(testFixtures(projects.coreInternal))
}
