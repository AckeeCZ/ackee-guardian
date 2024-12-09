package io.github.ackeecz.guardian.properties

import io.github.ackeecz.guardian.util.PublishableProject
import java.util.Properties

public sealed class ArtifactProperties(
    private val properties: Properties,
    private val defaultPropertyPrefix: String,
    versionPropertyPrefix: String = defaultPropertyPrefix,
) {

    public val id: String = getPropertyWithDefaultPrefix("ARTIFACT_ID")
    public val version: String = getProperty(prefix = versionPropertyPrefix, name = "VERSION")
    public val pomName: String = getPropertyWithDefaultPrefix("POM_NAME")
    public val pomYear: String = getPropertyWithDefaultPrefix("POM_YEAR")
    public val pomDescription: String = getPropertyWithDefaultPrefix("POM_DESCRIPTION")

    private fun getPropertyWithDefaultPrefix(name: String): String {
        return getProperty(prefix = defaultPropertyPrefix, name = name)
    }

    private fun getProperty(prefix: String, name: String): String {
        return properties.getNonNull("${prefix}_$name")
    }

    public class Bom(properties: Properties) : ArtifactProperties(
        properties = properties,
        defaultPropertyPrefix = "BOM",
    )

    public class Core(properties: Properties) : ArtifactProperties(
        properties = properties,
        defaultPropertyPrefix = "CORE",
    )

    public class CoreInternal(properties: Properties) : ArtifactProperties(
        properties = properties,
        defaultPropertyPrefix = "CORE_INTERNAL",
    )

    public sealed class DataStore(
        properties: Properties,
        defaultPropertyPrefix: String,
    ) : ArtifactProperties(
        properties = properties,
        defaultPropertyPrefix = defaultPropertyPrefix,
        versionPropertyPrefix = "DATASTORE",
    ) {

        public class Core(properties: Properties) : DataStore(
            properties = properties,
            defaultPropertyPrefix = "DATASTORE_CORE",
        )

        public class CoreInternal(properties: Properties) : DataStore(
            properties = properties,
            defaultPropertyPrefix = "DATASTORE_CORE_INTERNAL",
        )

        public class Preferences(properties: Properties) : DataStore(
            properties = properties,
            defaultPropertyPrefix = "DATASTORE_PREFERENCES",
        )

        public class Typed(properties: Properties) : DataStore(
            properties = properties,
            defaultPropertyPrefix = "DATASTORE",
        )
    }

    public class Jetpack(properties: Properties) : ArtifactProperties(
        properties = properties,
        defaultPropertyPrefix = "JETPACK",
    )

    internal companion object {

        fun getFor(
            projectName: String,
            properties: Properties,
        ): ArtifactProperties = when (projectName) {
            PublishableProject.Bom.projectName -> Bom(properties)
            PublishableProject.Core.projectName -> Core(properties)
            PublishableProject.CoreInternal.projectName -> CoreInternal(properties)
            PublishableProject.DataStore.DATA_STORE.projectName -> DataStore.Typed(properties)
            PublishableProject.DataStore.CORE.projectName -> DataStore.Core(properties)
            PublishableProject.DataStore.CORE_INTERNAL.projectName -> DataStore.CoreInternal(properties)
            PublishableProject.DataStore.PREFERENCES.projectName -> DataStore.Preferences(properties)
            PublishableProject.Jetpack.projectName -> Jetpack(properties)
            else -> throw IllegalStateException("Unknown Gradle module with name $projectName. Please " +
                "add artifact properties for this module and corresponding mapping in " +
                "${ArtifactProperties::class.simpleName}. It is also possible that you changed module " +
                "name and in that case update the mapping as well.")
        }
    }
}
