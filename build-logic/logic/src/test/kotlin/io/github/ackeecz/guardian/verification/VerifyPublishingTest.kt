package io.github.ackeecz.guardian.verification

import io.github.ackeecz.guardian.testutil.buildProject
import io.github.ackeecz.guardian.testutil.withVersion
import io.github.ackeecz.guardian.util.PublishableProject
import io.kotest.core.spec.style.FunSpec
import io.kotest.datatest.withData
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import java.util.UUID

private lateinit var getPreviousTag: GetPreviousTagStub
private lateinit var getArtifactVersionFromTag: GetArtifactVersionFromTagStub
private lateinit var getReleaseDependentProjects: GetReleaseDependentProjectsStub
private lateinit var checkArtifactUpdateStatus: CheckArtifactUpdateStatusStub

private lateinit var underTest: VerifyPublishing

internal class VerifyPublishingTest : FunSpec({

    beforeTest {
        getPreviousTag = GetPreviousTagStub()
        getArtifactVersionFromTag = GetArtifactVersionFromTagStub()
        getReleaseDependentProjects = GetReleaseDependentProjectsStub()
        checkArtifactUpdateStatus = CheckArtifactUpdateStatusStub()
        underTest = VerifyPublishing(
            getPreviousTag = getPreviousTag,
            getArtifactVersionFromTag = getArtifactVersionFromTag,
            getReleaseDependentProjects = getReleaseDependentProjects,
            checkArtifactUpdateStatus = checkArtifactUpdateStatus,
        )
    }

    test("publishing verification uses previous BOM tag") {
        getPreviousTag.shouldBeInstanceOf<GetPreviousTag>()
    }

    test("get artifact version from the previous tag") {
        val expected = TagResult.Tag(UUID.randomUUID().toString())
        getPreviousTag.result = expected

        underTest(buildProject())

        getArtifactVersionFromTag.receivedTagResult shouldBe expected
    }

    test("check artifact update status since the previous tag") {
        val expected = TagResult.Tag(UUID.randomUUID().toString())
        getPreviousTag.result = expected
        val artifactVersion = ArtifactVersion("1.0.0")
        val project = buildProject().withVersion(artifactVersion)
        getArtifactVersionFromTag.setProjectVersion(project, artifactVersion)

        underTest(project)

        checkArtifactUpdateStatus.receivedTagResult shouldBe expected
    }

    context("succeed when version of artifact increased, it has dependent artifacts and they increased their version as well") {
        withData(
            nameFn = { "previousTagVersion=$it" },
            ts = listOf(ArtifactVersion("1.0.0"), null),
        ) { previousTagVersion ->
            // Arrange
            val increasedVersion = ArtifactVersion("1.0.1")
            val rootProject = buildProject(name = "root")

            val dependentProject1 = buildProject(name = "dependent-1", parent = rootProject).withVersion(increasedVersion)
            val dependentProject2 = buildProject(name = "dependent-2", parent = rootProject).withVersion(increasedVersion)
            getReleaseDependentProjects.dependentProjects = listOf(dependentProject1, dependentProject2)

            val checkedProject = buildProject(name = "checked", parent = rootProject).withVersion(increasedVersion)

            getArtifactVersionFromTag.setProjectVersion(checkedProject, previousTagVersion)
            getArtifactVersionFromTag.setProjectVersion(dependentProject1, previousTagVersion)
            getArtifactVersionFromTag.setProjectVersion(dependentProject2, previousTagVersion)

            // Act
            val actual = underTest(checkedProject)

            // Assert
            actual.shouldBeInstanceOf<VerifyPublishing.Result.Success>()
        }
    }

    context("fail when version of artifact increased, it has dependent artifacts but they did not increase their version") {
        withData(
            nameFn = { "previousTagVersion=$it" },
            ts = listOf(ArtifactVersion("1.0.0"), null),
        ) { previousTagVersion ->
            // Arrange
            val increasedVersion = ArtifactVersion("1.0.1")
            val rootProject = buildProject(name = "root")

            val dependentProjectsOldVersion = ArtifactVersion("1.0.0")
            val dependentProject1 = buildProject(name = "dependent-1", parent = rootProject).withVersion(dependentProjectsOldVersion)
            val dependentProject2 = buildProject(name = "dependent-2", parent = rootProject).withVersion(dependentProjectsOldVersion)
            val dependentProject3 = buildProject(name = "dependent-3", parent = rootProject).withVersion(ArtifactVersion("1.0.1"))
            getReleaseDependentProjects.dependentProjects = listOf(dependentProject1, dependentProject2, dependentProject3)

            val checkedProject = buildProject(name = "checked", parent = rootProject).withVersion(increasedVersion)

            getArtifactVersionFromTag.setProjectVersion(checkedProject, previousTagVersion)
            getArtifactVersionFromTag.setProjectVersion(dependentProject1, dependentProjectsOldVersion)
            getArtifactVersionFromTag.setProjectVersion(dependentProject2, dependentProjectsOldVersion)
            getArtifactVersionFromTag.setProjectVersion(dependentProject3, dependentProjectsOldVersion)

            // Act
            val actual = underTest(checkedProject)

            // Assert
            actual.shouldBeInstanceOf<DependentProjectsOutdated>().also {
                it.currentProject shouldBe checkedProject
                it.outdatedDependentProjects shouldContainExactlyInAnyOrder listOf(dependentProject1, dependentProject2)
            }
        }
    }

    context("succeed when version of artifact increased and it has no dependent artifacts") {
        withData(
            nameFn = { "previousTagVersion=$it" },
            ts = listOf(ArtifactVersion("1.0.0"), null),
        ) { previousTagVersion ->
            // Arrange
            getReleaseDependentProjects.dependentProjects = emptyList()
            val increasedVersion = ArtifactVersion("1.0.1")
            val rootProject = buildProject(name = "root")
            val checkedProject = buildProject(name = "checked", parent = rootProject).withVersion(increasedVersion)
            getArtifactVersionFromTag.setProjectVersion(checkedProject, previousTagVersion)

            val actual = underTest(checkedProject)

            actual.shouldBeInstanceOf<VerifyPublishing.Result.Success>()
        }
    }

    test("succeed when version of artifact not increased, there are no changes in it and it has dependent artifacts") {
        // Arrange
        val checkedArtifactVersion = ArtifactVersion("1.0.0")
        val rootProject = buildProject(name = "root")

        val dependentProject = buildProject(name = "dependent", parent = rootProject)
        getReleaseDependentProjects.dependentProjects = listOf(dependentProject)

        val checkedProject = buildProject(name = "checked", parent = rootProject).withVersion(checkedArtifactVersion)
        getArtifactVersionFromTag.setProjectVersion(checkedProject, checkedArtifactVersion)

        checkArtifactUpdateStatus.artifactUpdateStatus = ArtifactUpdateStatus.UP_TO_DATE

        // Act
        val actual = underTest(checkedProject)

        // Assert
        actual.shouldBeInstanceOf<VerifyPublishing.Result.Success>()
    }

    test("succeed when version of artifact not increased, there are no changes in it and it doesn't have dependent artifacts") {
        // Arrange
        val checkedArtifactVersion = ArtifactVersion("1.0.0")
        val rootProject = buildProject(name = "root")

        getReleaseDependentProjects.dependentProjects = emptyList()

        val checkedProject = buildProject(name = "checked", parent = rootProject).withVersion(checkedArtifactVersion)
        getArtifactVersionFromTag.setProjectVersion(checkedProject, checkedArtifactVersion)

        checkArtifactUpdateStatus.artifactUpdateStatus = ArtifactUpdateStatus.UP_TO_DATE

        // Act
        val actual = underTest(checkedProject)

        // Assert
        actual.shouldBeInstanceOf<VerifyPublishing.Result.Success>()
    }

    test("fail when version of artifact not increased, there are changes in it and it has dependent artifacts") {
        // Arrange
        val checkedArtifactVersion = ArtifactVersion("1.0.0")
        val rootProject = buildProject(name = "root")

        val dependentProjects = listOf(buildProject(name = "dependent", parent = rootProject))
        getReleaseDependentProjects.dependentProjects = dependentProjects

        val checkedProject = buildProject(name = "checked", parent = rootProject).withVersion(checkedArtifactVersion)
        getArtifactVersionFromTag.setProjectVersion(checkedProject, checkedArtifactVersion)

        checkArtifactUpdateStatus.artifactUpdateStatus = ArtifactUpdateStatus.UPDATE_NEEDED

        // Act
        val actual = underTest(checkedProject)

        // Assert
        actual.shouldBeInstanceOf<ArtifactVersionNotIncreased>().also {
            it.currentProject shouldBe checkedProject
            it.dependentProjects shouldContainExactlyInAnyOrder dependentProjects
        }
    }

    test("warn when version of artifact not increased, there are changes in it but it doesn't have dependent artifacts") {
        // Arrange
        val checkedArtifactVersion = ArtifactVersion("1.0.0")
        val rootProject = buildProject(name = "root")

        getReleaseDependentProjects.dependentProjects = emptyList()

        val checkedProject = buildProject(name = "checked", parent = rootProject).withVersion(checkedArtifactVersion)
        getArtifactVersionFromTag.setProjectVersion(checkedProject, checkedArtifactVersion)

        checkArtifactUpdateStatus.artifactUpdateStatus = ArtifactUpdateStatus.UPDATE_NEEDED

        // Act
        val actual = underTest(checkedProject)

        // Assert
        actual.shouldBeInstanceOf<CheckIfShouldUpdate>()
            .currentProject
            .shouldBe(checkedProject)
    }

    dataStore()
})

@Suppress("MaxLineLength")
private fun FunSpec.dataStore() = context("datastore") {
    context("warn instead of failure when version of artifact not increased, there are changes in it and dependent modules are only other datastore artifacts") {
        withData(PublishableProject.DataStore.values().toList()) { dataStoreProject ->
            // Arrange
            val dataStoreArtifactVersion = ArtifactVersion("1.0.0")
            val rootProject = buildProject(name = "root")

            getReleaseDependentProjects.dependentProjects = PublishableProject.DataStore.values()
                .filterNot { it == dataStoreProject }
                .map { buildProject(name = it.projectName, parent = rootProject) }

            val checkedProject = buildProject(
                name = dataStoreProject.projectName,
                parent = rootProject,
            ).withVersion(dataStoreArtifactVersion)
            getArtifactVersionFromTag.setProjectVersion(checkedProject, dataStoreArtifactVersion)

            checkArtifactUpdateStatus.artifactUpdateStatus = ArtifactUpdateStatus.UPDATE_NEEDED

            // Act
            val actual = underTest(checkedProject)

            // Assert
            actual.shouldBeInstanceOf<CheckIfShouldUpdate>()
                .currentProject
                .shouldBe(checkedProject)
        }
    }

    context("fail when version of artifact not increased, there are changes in it and dependent modules are not only other datastore artifacts") {
        withData(PublishableProject.DataStore.values().toList()) { dataStoreProject ->
            // Arrange
            val dataStoreArtifactVersion = ArtifactVersion("1.0.0")
            val rootProject = buildProject(name = "root")

            val otherDependentProject = buildProject(name = "other-dependent", parent = rootProject)
            getReleaseDependentProjects.dependentProjects = PublishableProject.DataStore.values()
                .filterNot { it == dataStoreProject }
                .map { buildProject(name = it.projectName, parent = rootProject) }
                .plus(otherDependentProject)

            val checkedProject = buildProject(
                name = dataStoreProject.projectName,
                parent = rootProject,
            ).withVersion(dataStoreArtifactVersion)
            getArtifactVersionFromTag.setProjectVersion(checkedProject, dataStoreArtifactVersion)

            checkArtifactUpdateStatus.artifactUpdateStatus = ArtifactUpdateStatus.UPDATE_NEEDED

            // Act
            val actual = underTest(checkedProject)

            // Assert
            actual.shouldBeInstanceOf<ArtifactVersionNotIncreased>().also {
                it.currentProject shouldBe checkedProject
                it.dependentProjects shouldContainExactlyInAnyOrder listOf(otherDependentProject)
            }
        }
    }
}
