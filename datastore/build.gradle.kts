import org.gradle.kotlin.dsl.android

plugins {
    alias(libs.plugins.ackeecz.guardian.android.library)
    alias(libs.plugins.ackeecz.guardian.publishing)
    alias(libs.plugins.ackeecz.guardian.testing)
    alias(libs.plugins.ackeecz.guardian.testing.android)
    alias(libs.plugins.ackeecz.guardian.testing.protobuf)
}

android {
    namespace = "io.github.ackeecz.guardian.datastore"
}

dependencies {

    api(projects.datastoreCore)
    implementation(projects.datastoreCoreInternal)

    implementation(platform(libs.coroutines.bom))
    implementation(libs.coroutines.android)

    api(libs.androidx.datastore)

    testImplementation(testFixtures(projects.datastoreCoreInternal))
}
