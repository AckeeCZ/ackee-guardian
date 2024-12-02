plugins {
    alias(libs.plugins.ackeecz.security.android.library)
    alias(libs.plugins.ackeecz.security.publishing)
}

android {
    namespace = "io.github.ackeecz.security.datastore.core"
}

dependencies {

    api(projects.core)
    implementation(projects.coreInternal)

    implementation(platform(libs.coroutines.bom))
    implementation(libs.coroutines.android)

    // TODO Change to compileOnly and add min version constraint for clients to use
    implementation(libs.tink.android)
}
