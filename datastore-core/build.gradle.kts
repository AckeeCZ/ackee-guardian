plugins {
    alias(libs.plugins.ackeecz.guardian.android.library)
    alias(libs.plugins.ackeecz.guardian.publishing)
    alias(libs.plugins.ackeecz.guardian.tink)
}

android {
    namespace = "io.github.ackeecz.guardian.datastore.core"
}

dependencies {

    api(projects.core)
    implementation(projects.coreInternal)

    implementation(platform(libs.coroutines.bom))
    implementation(libs.coroutines.android)
}
