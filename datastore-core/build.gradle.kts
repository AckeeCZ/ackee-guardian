plugins {
    id("ackee.security.android.library")
    id("ackee.security.publishing")
}

android {
    namespace = "io.github.ackeecz.security.datastore.core"
}

dependencies {

    api(projects.core)
    implementation(projects.coreInternal)

    implementation(platform(libs.coroutines.bom))
    implementation(libs.coroutines.android)

    // TODO Add min version constraint for clients to use
    compileOnly(libs.tink.android)
}
