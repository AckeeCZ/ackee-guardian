package io.github.ackeecz.security.testutil

import io.github.ackeecz.security.verification.ArtifactVersion
import org.gradle.api.Project
import org.gradle.kotlin.dsl.dependencies
import org.gradle.testfixtures.ProjectBuilder

internal const val IMPLEMENTATION_CONFIGURATION = "implementation"

internal fun buildProject(name: String, parent: Project): Project {
    return buildProject {
        withName(name)
        withParent(parent)
    }
}

internal fun buildProject(name: String): Project {
    return buildProject { withName(name) }
}

internal fun buildProject(block: ProjectBuilder.() -> Unit = {}): Project {
    return ProjectBuilder.builder().apply(block).build()
}

internal fun Project.withVersion(version: ArtifactVersion) = withVersion(version.value)

internal fun Project.withVersion(version: String) = also { it.version = version }

internal fun Project.addImplementationDependencies(vararg dependency: Project): Project {
    return addDependencies(configuration = IMPLEMENTATION_CONFIGURATION, *dependency)
}

internal fun Project.addDependencies(configuration: String, vararg dependency: Project): Project {
    configurations.maybeCreate(configuration)
    dependencies {
        dependency.forEach { add(configuration, it) }
    }
    return this
}
