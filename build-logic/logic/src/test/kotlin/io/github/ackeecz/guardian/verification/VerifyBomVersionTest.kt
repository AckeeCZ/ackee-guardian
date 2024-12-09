package io.github.ackeecz.guardian.verification

import io.github.ackeecz.guardian.testutil.buildProject
import io.github.ackeecz.guardian.testutil.withVersion
import io.github.ackeecz.guardian.verification.GetTagTest.Companion.BOM_VERSION_TAG_PREFIX
import io.kotest.core.spec.style.FunSpec
import io.kotest.datatest.withData
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import java.util.UUID

private lateinit var getCurrentTag: GetCurrentTagStub
private lateinit var getArtifactVersionFromTag: GetArtifactVersionFromTagStub
private lateinit var underTest: VerifyBomVersion

internal class VerifyBomVersionTest : FunSpec({

    beforeTest {
        getCurrentTag = GetCurrentTagStub()
        getArtifactVersionFromTag = GetArtifactVersionFromTagStub()
        underTest = VerifyBomVersion(
            getCurrentTag = getCurrentTag,
            getArtifactVersionFromTag = getArtifactVersionFromTag,
        )
    }

    test("BOM verification uses current BOM tag") {
        getCurrentTag.shouldBeInstanceOf<GetCurrentTag>()
    }

    test("get artifact version from the current tag") {
        val expected = TagResult.Tag(UUID.randomUUID().toString())
        getCurrentTag.result = expected

        underTest(buildProject())

        getArtifactVersionFromTag.receivedTagResult shouldBe expected
    }

    test("succeed when BOM artifact version matches current tag version") {
        val version = "1.0.0"
        getCurrentTag.result = TagResult.Tag("$BOM_VERSION_TAG_PREFIX$version")
        val bomProject = buildProject()
        getArtifactVersionFromTag.setProjectVersion(bomProject, ArtifactVersion(version))

        underTest(bomProject) shouldBe VerifyBomVersion.Result.Success
    }

    context("fail when BOM artifact version does not match current tag version") {
        withData(
            nameFn = { "artifactBomVersion=${it.first.value}, currentTagVersion=${it.second}" },
            ArtifactVersion("1.0.0") to "1.0.1",
            ArtifactVersion("1.0.1") to "1.0.0",
        ) { (bomArtifactVersion, currentTagVersion) ->
            getCurrentTag.result = TagResult.Tag("$BOM_VERSION_TAG_PREFIX$currentTagVersion")
            val bomProject = buildProject()
            getArtifactVersionFromTag.setProjectVersion(bomProject, bomArtifactVersion)

            underTest(bomProject)
                .shouldBeInstanceOf<VerifyBomVersion.Result.Error>()
                .shouldBeInstanceOf<VerifyBomVersion.Result.Error.NotMatchingVersion>()
                .let {
                    it.bomArtifactVersion shouldBe bomArtifactVersion
                    it.currentTagVersion shouldBe currentTagVersion
                }
        }
    }

    test("fail when tag has unexpected format") {
        val tagResult = TagResult.Tag("incorrect-format-1.0.0")
        getCurrentTag.result = tagResult
        val bomProject = buildProject()
        getArtifactVersionFromTag.setProjectVersion(bomProject, ArtifactVersion("1.0.0"))

        underTest(bomProject)
            .shouldBeInstanceOf<VerifyBomVersion.Result.Error>()
            .shouldBeInstanceOf<VerifyBomVersion.Result.Error.UnexpectedTagFormat>()
            .tag
            .shouldBe(tagResult.value)
    }

    test("fail when tag is missing") {
        getCurrentTag.result = TagResult.FirstCommitHash("de5035f5a24621ea5361279d867ad75abc967ca3")
        val bomProject = buildProject().withVersion("1.0.0")

        underTest(bomProject)
            .shouldBeInstanceOf<VerifyBomVersion.Result.Error>()
            .shouldBeInstanceOf<VerifyBomVersion.Result.Error.TagMissing>()
    }

    // This case should not be possible, but technically API allows it under the hood, so we test it
    test("fail when artifact version is missing on current tag") {
        val tagVersion = "1.0.0"
        getCurrentTag.result = TagResult.Tag("$BOM_VERSION_TAG_PREFIX$tagVersion")
        val bomProject = buildProject()
        getArtifactVersionFromTag.setProjectVersion(bomProject, version = null)

        underTest(bomProject)
            .shouldBeInstanceOf<VerifyBomVersion.Result.Error>()
            .shouldBeInstanceOf<VerifyBomVersion.Result.Error.NotMatchingVersion>().let {
                it.bomArtifactVersion shouldBe null
                it.currentTagVersion shouldBe tagVersion
            }
    }
})
