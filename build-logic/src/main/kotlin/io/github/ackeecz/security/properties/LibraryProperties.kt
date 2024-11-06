package io.github.ackeecz.security.properties

import org.gradle.api.Project
import java.io.File
import java.util.Properties

class LibraryProperties(private val project: Project) {

    private val properties = Properties().also {
        it.load(File("${project.rootProject.rootDir}/lib.properties").reader())
    }

    val groupId: String = getProperty("GROUP_ID")
    val pomUrl: String = getProperty("POM_URL")

    val pomDeveloperId: String = getProperty("POM_DEVELOPER_ID")
    val pomDeveloperName: String = getProperty("POM_DEVELOPER_NAME")
    val pomDeveloperEmail: String = getProperty("POM_DEVELOPER_EMAIL")

    val pomLicenceName: String = getProperty("POM_LICENCE_NAME")
    val pomLicenceUrl: String = getProperty("POM_LICENCE_URL")

    val pomScmConnection: String = getProperty("POM_SCM_CONNECTION")
    val pomScmDeveloperConnection: String = getProperty("POM_SCM_DEVELOPER_CONNECTION")
    val pomScmUrl: String = getProperty("POM_SCM_URL")

    fun getArtifactProperties(): ArtifactProperties {
        return ArtifactProperties.getFor(projectName = project.name, properties = properties)
    }

    private fun getProperty(name: String) = properties.getNonNull(name)
}

internal fun Properties.getNonNull(name: String) = requireNotNull(getProperty(name))
