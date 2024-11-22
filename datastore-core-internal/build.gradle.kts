plugins {
    alias(libs.plugins.ackeecz.security.android.library)
    alias(libs.plugins.ackeecz.security.publishing)
    alias(libs.plugins.ackeecz.security.testfixtures)
    alias(libs.plugins.ackeecz.security.tink)
}

android {
    namespace = "io.github.ackeecz.security.datastore.core.internal"
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
