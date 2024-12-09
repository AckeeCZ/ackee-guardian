plugins {
    alias(libs.plugins.ackeecz.guardian.preflightchecks) apply true
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.detekt) apply false
    alias(libs.plugins.dokka) apply false
    alias(libs.plugins.gradle.testLogger) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.binaryCompatibilityValidator) apply true
    alias(libs.plugins.mavenPublish) apply false
    alias(libs.plugins.protobuf) apply false
}

apiValidation {
    ignoredProjects.addAll(
        listOf(
            "app",
            "core-internal",
            "datastore-core-internal",
        )
    )
}
