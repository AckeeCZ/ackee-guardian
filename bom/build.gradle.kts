plugins {
    `java-platform`
    id("ackee.security.publishing")
}

dependencies {
    constraints {
        api(projects.core)
        api(projects.datastore)
        api(projects.datastorePreferences)
        api(projects.jetpack)
    }
}
