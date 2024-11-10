package io.github.ackeecz.security.util

internal sealed interface PublishableProject {

    val projectName: String

    object Bom : PublishableProject {

        override val projectName = "bom"
    }

    object Core : PublishableProject {

        override val projectName = "core"
    }

    object CoreInternal : PublishableProject {

        override val projectName = "core-internal"
    }

    // It is important for publishing verification logic for all DataStore artifacts to be grouped
    // together in this enum, but also use the same artifact version at the same time. Having some
    // DataStore artifact under this enum with a different versioning than the rest could lead to
    // problems with publishing verification logic.
    enum class DataStore(override val projectName: String) : PublishableProject {

        DATA_STORE(projectName = "datastore"),
        CORE(projectName = "datastore-core"),
        CORE_INTERNAL(projectName = "datastore-core-internal"),
        PREFERENCES(projectName = "datastore-preferences"),
    }

    object Jetpack : PublishableProject {

        override val projectName = "jetpack"
    }
}
