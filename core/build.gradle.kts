import org.gradle.kotlin.dsl.android

plugins {
    id("ackee.security.android.library")
    id("ackee.security.publishing")
    id("ackee.security.testing")
    id("ackee.security.testing.android")
}

android {
    namespace = "io.github.ackeecz.security.core"

    // TODO test fixtures source code ignores Kotlin compiler options declared in KotlinPlugin
    //  convention plugin for some reason. Try to figure out why.
    @Suppress("UnstableApiUsage")
    testFixtures {
        enable = true
    }
}

dependencies {

    implementation(libs.androidx.annotation)

    implementation(platform(libs.coroutines.bom))
    implementation(libs.coroutines.android)

    testFixturesImplementation(libs.androidx.test.junitKtx)
    testFixturesImplementation(libs.bouncyCastle.bcpkix)
    testFixturesImplementation(libs.coroutines.test)
    testFixturesImplementation(libs.junit4)
    testFixturesImplementation(libs.kotest.framework.api)
    testFixturesImplementation(libs.robolectric)
}
