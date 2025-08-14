package io.github.ackeecz.guardian.plugin

import com.vanniktech.maven.publish.MavenPublishBaseExtension
import com.vanniktech.maven.publish.SonatypeHost
import io.github.ackeecz.guardian.properties.LibraryProperties
import io.github.ackeecz.guardian.util.PublishableProject
import io.github.ackeecz.guardian.verification.task.CheckIfUpdateNeededSinceCurrentTagTask
import io.github.ackeecz.guardian.verification.task.VerifyPublishingTask
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.UnknownConfigurationException
import org.gradle.api.component.AdhocComponentWithVariants
import org.gradle.api.publish.maven.tasks.PublishToMavenRepository
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.withType
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse

internal class PublishingPlugin : Plugin<Project> {

    override fun apply(target: Project) {
        target.configure()
    }

    private fun Project.configure() {
        // com.vanniktech.maven.publish plugin can detect and use applied Dokka plugin automatically
        pluginManager.apply(libs.plugins.dokka)
        pluginManager.apply(libs.plugins.mavenPublish)

        val libraryProperties = LibraryProperties(project)
        val artifactProperties = libraryProperties.getArtifactProperties()

        group = libraryProperties.groupId
        version = artifactProperties.version

        mavenPublishing {

            coordinates(artifactId = artifactProperties.id)

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

        excludeTestFixturesFromPublishing()
        configurePublishingSkipping(
            groupId = libraryProperties.groupId,
            artifactId = artifactProperties.id,
            version = artifactProperties.version,
        )
        CheckIfUpdateNeededSinceCurrentTagTask.registerFor(project)
        VerifyPublishingTask.registerFor(project)
    }
}

private fun Project.excludeTestFixturesFromPublishing() {
    if (project.name != PublishableProject.Bom.projectName) {
        // TODO Using afterEvaluate seems hacky, but even when using AGP DSL like onVariants, the
        //  component is not available yet and I don't know how to do it better for now
        afterEvaluate {
            try {
                // TODO It would be also better to not rely statically on this component, but using
                //  AGP DSL like onVariants does not seem to provide a component types that could skip
                //  publication like code below does. Maybe it is somehow possible using AGP API, but
                //  I didn't figure it out.
                val componentName = "release"
                val component = project.components.getByName(componentName) as AdhocComponentWithVariants
                val configurationNames = listOf(
                    "${componentName}TestFixturesVariantReleaseApiPublication",
                    "${componentName}TestFixturesVariantReleaseRuntimePublication"
                )
                configurationNames.forEach { configName ->
                    component.withVariantsFromConfiguration(configurations.getByName(configName)) { skip() }
                }
            } catch (_: UnknownConfigurationException) {
                // Thrown when the current Project does not support test fixtures, so it does not contain
                // configurations above
            }
        }
    }
}

/**
 * Configures publishing skipping, if the artifact with the given [version] already exists in
 * the Maven Central repository. This allows us to conveniently run a single publishing task
 * everytime, without worrying about what actually can be published.
 */
private fun Project.configurePublishingSkipping(groupId: String, artifactId: String, version: String) {
    tasks.withType<PublishToMavenRepository>().configureEach {
        onlyIf {
            val groupIdPath = groupId.replace('.', '/')
            val url = "https://repo1.maven.org/maven2/$groupIdPath/$artifactId/$version"
            val request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .GET()
                .build()
            val client = HttpClient.newBuilder()
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build()
            val response = client.send(request, HttpResponse.BodyHandlers.ofString())
            val httpStatusCode = response.statusCode()
            // We fail the task if we get unexpected error or redirect, except 404 which is expected.
            // Unexpected status codes might potentially lead to publishing issues, because we can't be
            // sure if the artifact is already published or not, so it is better to fail the task.
            val artifactPublishedStatusCodes = 200..299
            val artifactNotPublishedStatusCode = 404
            val isUnexpectedStatusCode = httpStatusCode !in artifactPublishedStatusCodes &&
                httpStatusCode != artifactNotPublishedStatusCode
            if (isUnexpectedStatusCode) {
                error("Unexpected HTTP status code $httpStatusCode for URL: $url")
            }
            return@onlyIf (httpStatusCode == artifactNotPublishedStatusCode)
        }
    }
}

private fun Project.mavenPublishing(action: MavenPublishBaseExtension.() -> Unit) {
    extensions.configure(MavenPublishBaseExtension::class, action)
}
