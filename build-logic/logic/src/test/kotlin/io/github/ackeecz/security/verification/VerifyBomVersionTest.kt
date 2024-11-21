package io.github.ackeecz.security.verification

import io.github.ackeecz.security.testutil.buildProject
import io.github.ackeecz.security.testutil.withVersion
import io.github.ackeecz.security.verification.GetLastTagTest.Companion.BOM_VERSION_TAG_PREFIX
import io.kotest.core.spec.style.FunSpec
import io.kotest.datatest.withData
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

private lateinit var getLastTag: GetLastTagStub
private lateinit var getArtifactVersionFromLastTag: GetArtifactVersionFromLastTagStub
private lateinit var underTest: VerifyBomVersion

internal class VerifyBomVersionTest : FunSpec({

    beforeTest {
        getLastTag = GetLastTagStub()
        getArtifactVersionFromLastTag = GetArtifactVersionFromLastTagStub()
        underTest = VerifyBomVersion(
            getLastTag = getLastTag,
            getArtifactVersionFromLastTag = getArtifactVersionFromLastTag,
        )
    }

    test("succeed when BOM artifact version matches last tag version") {
        val version = "1.0.0"
        getLastTag.result = LastTagResult.Tag("$BOM_VERSION_TAG_PREFIX$version")
        val bomProject = buildProject()
        getArtifactVersionFromLastTag.setProjectVersion(bomProject, ArtifactVersion(version))

        underTest(bomProject) shouldBe VerifyBomVersion.Result.Success
    }

    context("fail when BOM artifact version does not match last tag version") {
        withData(
            nameFn = { "artifactBomVersion=${it.first.value}, lastTagVersion=${it.second}" },
            ArtifactVersion("1.0.0") to "1.0.1",
            ArtifactVersion("1.0.1") to "1.0.0",
        ) { (bomArtifactVersion, lastTagVersion) ->
            getLastTag.result = LastTagResult.Tag("$BOM_VERSION_TAG_PREFIX$lastTagVersion")
            val bomProject = buildProject()
            getArtifactVersionFromLastTag.setProjectVersion(bomProject, bomArtifactVersion)

            underTest(bomProject)
                .shouldBeInstanceOf<VerifyBomVersion.Result.Error>()
                .shouldBeInstanceOf<VerifyBomVersion.Result.Error.NotMatchingVersion>()
                .let {
                    it.bomArtifactVersion shouldBe bomArtifactVersion
                    it.lastTagVersion shouldBe lastTagVersion
                }
        }
    }

    test("fail when last tag has unexpected format") {
        val tagResult = LastTagResult.Tag("incorrect-format-1.0.0")
        getLastTag.result = tagResult
        val bomProject = buildProject()
        getArtifactVersionFromLastTag.setProjectVersion(bomProject, ArtifactVersion("1.0.0"))

        underTest(bomProject)
            .shouldBeInstanceOf<VerifyBomVersion.Result.Error>()
            .shouldBeInstanceOf<VerifyBomVersion.Result.Error.UnexpectedTagFormat>()
            .tag
            .shouldBe(tagResult.value)
    }

    test("fail when tag is missing") {
        getLastTag.result = LastTagResult.FirstCommitHash("de5035f5a24621ea5361279d867ad75abc967ca3")
        val bomProject = buildProject().withVersion("1.0.0")

        underTest(bomProject)
            .shouldBeInstanceOf<VerifyBomVersion.Result.Error>()
            .shouldBeInstanceOf<VerifyBomVersion.Result.Error.TagMissing>()
    }

    // This case should not be possible, but technically API allows it under the hood, so we test it
    test("fail when artifact version is missing on last tag") {
        val lastTagVersion = "1.0.0"
        getLastTag.result = LastTagResult.Tag("$BOM_VERSION_TAG_PREFIX$lastTagVersion")
        val bomProject = buildProject()
        getArtifactVersionFromLastTag.setProjectVersion(bomProject, version = null)

        underTest(bomProject)
            .shouldBeInstanceOf<VerifyBomVersion.Result.Error>()
            .shouldBeInstanceOf<VerifyBomVersion.Result.Error.NotMatchingVersion>().let {
                it.bomArtifactVersion shouldBe null
                it.lastTagVersion shouldBe lastTagVersion
            }
    }
})
