package io.github.ackeecz.security.properties

import java.util.Properties

sealed class ArtifactProperties(
    private val properties: Properties,
    private val artifactPropertyPrefix: String,
) {

    val id = getProperty("ARTIFACT_ID")
    val version = getProperty("VERSION")
    val pomName = getProperty("POM_NAME")
    val pomYear = getProperty("POM_YEAR")
    val pomDescription = getProperty("POM_DESCRIPTION")

    private fun getProperty(name: String): String {
        return properties.getNonNull("${artifactPropertyPrefix}_$name")
    }

    class Core(properties: Properties) : ArtifactProperties(
        properties = properties,
        artifactPropertyPrefix = "CORE",
    )

    class CoreInternal(properties: Properties) : ArtifactProperties(
        properties = properties,
        artifactPropertyPrefix = "CORE_INTERNAL",
    )

    class Jetpack(properties: Properties) : ArtifactProperties(
        properties = properties,
        artifactPropertyPrefix = "JETPACK",
    )

    companion object {

        private const val CORE_MODULE_NAME = "core"
        private const val CORE_INTERNAL_MODULE_NAME = "core-internal"
        private const val JETPACK_MODULE_NAME = "jetpack"

        fun getCore(properties: Properties): Core = Core(properties)

        fun getFor(
            projectName: String,
            properties: Properties,
        ): ArtifactProperties = when (projectName) {
            CORE_MODULE_NAME -> Core(properties)
            CORE_INTERNAL_MODULE_NAME -> CoreInternal(properties)
            JETPACK_MODULE_NAME -> Jetpack(properties)
            else -> throw IllegalStateException("Unknown Gradle module with name $projectName. Please " +
                "add artifact properties for this module and corresponding mapping in " +
                "${ArtifactProperties::class.simpleName}. It is also possible that you changed module " +
                "name and in that case update the mapping as well.")
        }
    }
}
