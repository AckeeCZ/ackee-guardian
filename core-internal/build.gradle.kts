plugins {
    id("ackee.security.android.library")
    id("ackee.security.publishing")
    id("ackee.security.testfixtures")
}

android {
    namespace = "io.github.ackeecz.security.core.internal"
}

dependencies {

    implementation(platform(libs.coroutines.bom))
    implementation(libs.coroutines.android)

    testFixturesImplementation(libs.androidx.test.junitKtx)
    testFixturesImplementation(libs.bouncyCastle.bcpkix)
    testFixturesImplementation(libs.coroutines.test)
    testFixturesImplementation(libs.junit4)
    testFixturesImplementation(libs.kotest.framework.api)
    testFixturesImplementation(libs.robolectric)
    testFixturesImplementation(libs.tink.android)
}
