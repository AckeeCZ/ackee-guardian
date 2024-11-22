import io.github.ackeecz.security.verification.task.VerifyBomVersionTask

plugins {
    `java-platform`
    alias(libs.plugins.ackeecz.security.publishing)
}

dependencies {
    constraints {
        api(projects.core)
        api(projects.datastore)
        api(projects.datastorePreferences)
        api(projects.jetpack)

        // This enforces minimal Tink version for the client of this BOM that is equal to the version
        // that this library is compiled against.
        api(libs.tink.android)
    }
}

VerifyBomVersionTask.registerFor(project)
