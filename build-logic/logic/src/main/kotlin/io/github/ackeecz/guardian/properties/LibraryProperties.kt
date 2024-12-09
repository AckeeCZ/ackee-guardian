package io.github.ackeecz.guardian.properties

import io.github.ackeecz.guardian.util.PublishableProject
import org.gradle.api.Project
import java.io.File
import java.util.Properties

public class LibraryProperties internal constructor(
    private val properties: Properties,
    private val project: Project,
) {

    public val groupId: String = getProperty("GROUP_ID")
    public val pomUrl: String = getProperty("POM_URL")

    public val pomDeveloperId: String = getProperty("POM_DEVELOPER_ID")
    public val pomDeveloperName: String = getProperty("POM_DEVELOPER_NAME")
    public val pomDeveloperEmail: String = getProperty("POM_DEVELOPER_EMAIL")

    public val pomLicenceName: String = getProperty("POM_LICENCE_NAME")
    public val pomLicenceUrl: String = getProperty("POM_LICENCE_URL")

    public val pomScmConnection: String = getProperty("POM_SCM_CONNECTION")
    public val pomScmDeveloperConnection: String = getProperty("POM_SCM_DEVELOPER_CONNECTION")
    public val pomScmUrl: String = getProperty("POM_SCM_URL")

    public val bomArtifactProperties: ArtifactProperties
        get() = ArtifactProperties.getFor(
            projectName = PublishableProject.Bom.projectName,
            properties = properties,
        )

    public constructor(project: Project) : this(
        properties = Properties().also {
            it.load(File("${project.rootProject.rootDir}/lib.properties").reader())
        },
        project = project,
    )

    public fun getArtifactProperties(): ArtifactProperties {
        return ArtifactProperties.getFor(projectName = project.name, properties = properties)
    }

    private fun getProperty(name: String) = properties.getNonNull(name)
}

internal fun Properties.getNonNull(name: String) = requireNotNull(getProperty(name))
