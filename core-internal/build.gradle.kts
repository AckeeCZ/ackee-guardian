plugins {
    alias(libs.plugins.ackeecz.guardian.android.library)
    alias(libs.plugins.ackeecz.guardian.publishing)
    alias(libs.plugins.ackeecz.guardian.testfixtures)
}

android {
    namespace = "io.github.ackeecz.guardian.core.internal"
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
