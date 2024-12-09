plugins {
    alias(libs.plugins.ackeecz.guardian.android.library)
    alias(libs.plugins.ackeecz.guardian.publishing)
    alias(libs.plugins.ackeecz.guardian.testfixtures)
    alias(libs.plugins.ackeecz.guardian.tink)
}

android {
    namespace = "io.github.ackeecz.guardian.datastore.core.internal"
}

dependencies {

    implementation(projects.coreInternal)
    implementation(projects.datastoreCore)

    implementation(platform(libs.coroutines.bom))
    implementation(libs.coroutines.android)

    implementation(libs.androidx.datastore)

    testFixturesApi(testFixtures(projects.coreInternal))
    testFixturesImplementation(projects.datastoreCore)
    testFixturesImplementation(libs.androidx.datastore)
    testFixturesImplementation(libs.androidx.test.junitKtx)
    testFixturesImplementation(libs.coroutines.test)
    testFixturesImplementation(libs.junit4)
    testFixturesImplementation(libs.kotest.assertions.core)
    testFixturesImplementation(libs.tink.android)
}
