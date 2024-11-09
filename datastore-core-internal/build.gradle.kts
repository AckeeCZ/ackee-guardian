plugins {
    id("ackee.security.android.library")
    id("ackee.security.publishing")
    id("ackee.security.testfixtures")
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

    // TODO Add min version constraint for clients to use
    compileOnly(libs.tink.android)

    testFixturesApi(testFixtures(projects.coreInternal))
    testFixturesImplementation(projects.datastoreCore)
    testFixturesImplementation(libs.androidx.datastore)
    testFixturesImplementation(libs.androidx.test.junitKtx)
    testFixturesImplementation(libs.coroutines.test)
    testFixturesImplementation(libs.junit4)
    testFixturesImplementation(libs.kotest.assertions.core)
    testFixturesCompileOnly(libs.tink.android)
}
