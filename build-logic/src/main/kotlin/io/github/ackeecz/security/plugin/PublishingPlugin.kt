package io.github.ackeecz.security.plugin

import com.vanniktech.maven.publish.MavenPublishBaseExtension
import com.vanniktech.maven.publish.SonatypeHost
import io.github.ackeecz.security.properties.LibraryProperties
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.component.AdhocComponentWithVariants
import org.gradle.kotlin.dsl.configure

// TODO Provide signing secrets and ossrh username and password on CI using env variables
// TODO Explore binary compatibility validator
internal class PublishingPlugin : Plugin<Project> {

    override fun apply(target: Project) {
        target.configure()
    }

    private fun Project.configure() {
        // com.vanniktech.maven.publish plugin can detect and use applied Dokka plugin automatically
        pluginManager.apply(libs.findPlugin("dokka"))
        pluginManager.apply(libs.findPlugin("mavenPublish"))

        val libraryProperties = LibraryProperties(project)
        val artifactProperties = libraryProperties.getArtifactProperties()

        mavenPublishing {

            coordinates(
                groupId = libraryProperties.groupId,
                artifactId = artifactProperties.id,
                version = artifactProperties.version,
            )

            pom {
                name.set(artifactProperties.pomName)
                description.set(artifactProperties.pomDescription)
                inceptionYear.set(artifactProperties.pomYear)
                url.set(libraryProperties.pomUrl)
                licenses {
                    license {
                        name.set(libraryProperties.pomLicenceName)
                        url.set(libraryProperties.pomLicenceUrl)
                        distribution.set(libraryProperties.pomLicenceUrl)
                    }
                }
                developers {
                    developer {
                        id.set(libraryProperties.pomDeveloperId)
                        name.set(libraryProperties.pomDeveloperName)
                        url.set(libraryProperties.pomDeveloperEmail)
                    }
                }
                scm {
                    url.set(libraryProperties.pomScmUrl)
                    connection.set(libraryProperties.pomScmConnection)
                    developerConnection.set(libraryProperties.pomScmDeveloperConnection)
                }
            }

            signAllPublications()
            publishToMavenCentral(SonatypeHost.CENTRAL_PORTAL)
        }

        // TODO Using afterEvaluate seems hacky, but even when using AGP DSL like onVariants, the
        //  component is not available yet and I don't know how to do it better for now
        afterEvaluate {
            // TODO It would be also better to not rely statically on this component, but using
            //  AGP DSL like onVariants does not seem to provide a component types that could skip
            //  publication like code below does. Maybe it is somehow possible using AGP API, but
            //  I didn't figure it out.
            val componentName = "release"
            val component = project.components.getByName(componentName) as AdhocComponentWithVariants
            component.withVariantsFromConfiguration(configurations.getByName("${componentName}TestFixturesVariantReleaseApiPublication")) { skip() }
            component.withVariantsFromConfiguration(configurations.getByName("${componentName}TestFixturesVariantReleaseRuntimePublication")) { skip() }
        }
    }
}

private fun Project.mavenPublishing(action: MavenPublishBaseExtension.() -> Unit) {
    extensions.configure(MavenPublishBaseExtension::class, action)
}
