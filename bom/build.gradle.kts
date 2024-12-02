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
    }
}
