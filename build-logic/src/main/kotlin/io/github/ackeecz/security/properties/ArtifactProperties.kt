package io.github.ackeecz.security.properties

import java.util.Properties

sealed class ArtifactProperties(
    private val properties: Properties,
    private val defaultPropertyPrefix: String,
    versionPropertyPrefix: String = defaultPropertyPrefix,
) {

    val id = getPropertyWithDefaultPrefix("ARTIFACT_ID")
    val version = getProperty(prefix = versionPropertyPrefix, name = "VERSION")
    val pomName = getPropertyWithDefaultPrefix("POM_NAME")
    val pomYear = getPropertyWithDefaultPrefix("POM_YEAR")
    val pomDescription = getPropertyWithDefaultPrefix("POM_DESCRIPTION")

    private fun getPropertyWithDefaultPrefix(name: String): String {
        return getProperty(prefix = defaultPropertyPrefix, name = name)
    }

    private fun getProperty(prefix: String, name: String): String {
        return properties.getNonNull("${prefix}_$name")
    }

    class Core(properties: Properties) : ArtifactProperties(
        properties = properties,
        defaultPropertyPrefix = "CORE",
    )

    class CoreInternal(properties: Properties) : ArtifactProperties(
        properties = properties,
        defaultPropertyPrefix = "CORE_INTERNAL",
    )

    sealed class DataStore(
        properties: Properties,
        defaultPropertyPrefix: String,
    ) : ArtifactProperties(
        properties = properties,
        defaultPropertyPrefix = defaultPropertyPrefix,
        versionPropertyPrefix = "DATASTORE",
    ) {

        class Core(properties: Properties) : DataStore(
            properties = properties,
            defaultPropertyPrefix = "DATASTORE_CORE",
        )

        class CoreInternal(properties: Properties) : DataStore(
            properties = properties,
            defaultPropertyPrefix = "DATASTORE_CORE_INTERNAL",
        )

        class Preferences(properties: Properties) : DataStore(
            properties = properties,
            defaultPropertyPrefix = "DATASTORE_PREFERENCES",
        )

        class Typed(properties: Properties) : DataStore(
            properties = properties,
            defaultPropertyPrefix = "DATASTORE",
        )
    }

    class Jetpack(properties: Properties) : ArtifactProperties(
        properties = properties,
        defaultPropertyPrefix = "JETPACK",
    )

    companion object {

        private const val CORE_MODULE_NAME = "core"
        private const val CORE_INTERNAL_MODULE_NAME = "core-internal"
        private const val DATA_STORE_MODULE_NAME = "datastore"
        private const val DATA_STORE_CORE_MODULE_NAME = "datastore-core"
        private const val DATA_STORE_CORE_INTERNAL_MODULE_NAME = "datastore-core-internal"
        private const val DATA_STORE_PREFERENCES_MODULE_NAME = "datastore-preferences"
        private const val JETPACK_MODULE_NAME = "jetpack"

        fun getFor(
            projectName: String,
            properties: Properties,
        ): ArtifactProperties = when (projectName) {
            CORE_MODULE_NAME -> Core(properties)
            CORE_INTERNAL_MODULE_NAME -> CoreInternal(properties)
            DATA_STORE_MODULE_NAME -> DataStore.Typed(properties)
            DATA_STORE_CORE_MODULE_NAME -> DataStore.Core(properties)
            DATA_STORE_CORE_INTERNAL_MODULE_NAME -> DataStore.CoreInternal(properties)
            DATA_STORE_PREFERENCES_MODULE_NAME -> DataStore.Preferences(properties)
            JETPACK_MODULE_NAME -> Jetpack(properties)
            else -> throw IllegalStateException("Unknown Gradle module with name $projectName. Please " +
                "add artifact properties for this module and corresponding mapping in " +
                "${ArtifactProperties::class.simpleName}. It is also possible that you changed module " +
                "name and in that case update the mapping as well.")
        }
    }
}
