plugins {
    alias(libs.plugins.ackeecz.security.android.library)
    alias(libs.plugins.ackeecz.security.publishing)
    alias(libs.plugins.ackeecz.security.tink)
}

android {
    namespace = "io.github.ackeecz.security.datastore.core"
}

dependencies {

    api(projects.core)
    implementation(projects.coreInternal)

    implementation(platform(libs.coroutines.bom))
    implementation(libs.coroutines.android)
}
