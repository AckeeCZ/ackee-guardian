package io.github.ackeecz.security.verification

import io.github.ackeecz.security.testutil.IMPLEMENTATION_CONFIGURATION
import io.github.ackeecz.security.testutil.addDependencies
import io.github.ackeecz.security.testutil.addImplementationDependencies
import io.github.ackeecz.security.testutil.buildProject
import io.kotest.core.spec.style.FunSpec
import io.kotest.inspectors.forAll
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import org.gradle.api.Project

private lateinit var underTest: GetReleaseDependentProjects

internal class GetReleaseDependentProjectsTest : FunSpec({

    beforeEach {
        underTest = GetReleaseDependentProjects()
    }

    test("get all dependent projects for complex flat hierarchy (root and child projects)") {
        // Arrange
        val rootProject = buildProject(name = "root")
        val checkedProject = buildProject(name = "checked", parent = rootProject)
        val notDependentProject1 = buildProject(name = "not-dependent-1", parent = rootProject)
        val notDependentProject2 = buildProject(name = "not-dependent-2", parent = rootProject)
            .addImplementationDependencies(notDependentProject1)

        val dependentProject1 = buildProject(name = "dependent-1", parent = rootProject)
            .addImplementationDependencies(checkedProject, notDependentProject2)

        val dependentProject2 = buildProject(name = "dependent-2", parent = rootProject)
            .addImplementationDependencies(checkedProject, dependentProject1)

        // Act
        val actual = underTest(checkedProject)

        // Assert
        actual shouldContainProjectsExactlyInAnyOrder listOf(dependentProject1, dependentProject2)
    }

    test("get dependent projects for release configurations") {
        listOf(
            "api",
            "compileOnly",
            "compileOnlyApi",
            IMPLEMENTATION_CONFIGURATION,
            "releaseApi",
            "releaseCompileOnly",
            "releaseCompileOnlyApi",
            "releaseImplementation",
            "releaseRuntimeOnly",
            "runtimeOnly",
        ).forAll { configuration ->
            val rootProject = buildProject(name = "root")
            val checkedProject = buildProject(name = "checked", parent = rootProject)
            val notDependentProject = buildProject(name = "not-dependent", parent = rootProject)
            val dependentProject = buildProject(name = "dependent", parent = rootProject)
                .addDependencies(configuration, checkedProject, notDependentProject)

            val actual = underTest(checkedProject)

            actual shouldContainProjectsExactlyInAnyOrder listOf(dependentProject)
        }
    }

    test("get no dependent projects for non-release configurations") {
        listOf(
            "androidTestApi",
            "androidTestImplementation",
            "debugApi",
            "debugImplementation",
            "testDebugImplementation",
            "testFixturesImplementation",
            "testImplementation",
        ).forAll { configuration ->
            val rootProject = buildProject(name = "root")
            val checkedProject = buildProject(name = "checked", parent = rootProject)
            buildProject(name = "dependent-with-non-release-configuration", parent = rootProject)
                .addDependencies(configuration, checkedProject)

            val actual = underTest(checkedProject)

            actual.shouldBeEmpty()
        }
    }

    test("get all dependent projects for complex nested hierarchy (root, child, grandchild projects)") {
        // Arrange
        val rootProject = buildProject(name = "root")
        val checkedProject = buildProject(name = "checked", parent = rootProject)
        val notDependentChildProject = buildProject(name = "not-dependent-child", parent = rootProject)
        val dependentChildProject = buildProject(name = "dependent-child", parent = rootProject)
            .addImplementationDependencies(checkedProject, notDependentChildProject)

        val notDependentGrandChildProject = buildProject(
            name = "not-dependent-grand-child",
            parent = notDependentChildProject,
        )
        val dependentGrandChildProject = buildProject(
            name = "dependent-grand-child",
            parent = dependentChildProject,
        ).addImplementationDependencies(checkedProject, notDependentGrandChildProject)

        // Act
        val actual = underTest(checkedProject)

        // Assert
        actual shouldContainProjectsExactlyInAnyOrder listOf(dependentChildProject, dependentGrandChildProject)
    }

    // This is happening for some unit test configurations that they depend on itself, so we need to
    // filter these cases out, because we care only about other dependent projects
    test("get no dependent projects when project depend only on itself") {
        val rootProject = buildProject(name = "root")
        val checkedProject = buildProject(name = "checked", parent = rootProject).also {
            it.addDependencies("releaseUnitTestCompileClasspath", it)
        }

        val actual = underTest(checkedProject)

        actual.shouldBeEmpty()
    }
})

private infix fun List<Project>.shouldContainProjectsExactlyInAnyOrder(expected: List<Project>) {
    map { it.path } shouldContainExactlyInAnyOrder expected.map { it.path }
}
