import org.gradle.kotlin.dsl.android

plugins {
    alias(libs.plugins.ackeecz.security.android.library)
    alias(libs.plugins.ackeecz.security.publishing)
    alias(libs.plugins.ackeecz.security.testing)
    alias(libs.plugins.ackeecz.security.testing.android)
    alias(libs.plugins.ackeecz.security.testing.protobuf)
}

android {
    namespace = "io.github.ackeecz.security.datastore"
}

dependencies {

    api(projects.datastoreCore)
    implementation(projects.datastoreCoreInternal)

    implementation(platform(libs.coroutines.bom))
    implementation(libs.coroutines.android)

    api(libs.androidx.datastore)

    testImplementation(testFixtures(projects.datastoreCoreInternal))
}
