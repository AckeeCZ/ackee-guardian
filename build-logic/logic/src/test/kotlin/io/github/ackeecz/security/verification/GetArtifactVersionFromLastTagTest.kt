package io.github.ackeecz.security.verification

import io.github.ackeecz.security.testutil.buildProject
import io.github.ackeecz.security.util.ExecuteCommand
import io.github.ackeecz.security.util.StubExecuteCommand
import io.github.ackeecz.security.util.createErrorExecuteCommandResult
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import java.io.StringWriter
import java.util.Properties

private const val PROPERTIES_FILE_NAME = "lib.properties"

private const val PROJECT_NAME = "core"
private const val VERSION_PROPERTY_NAME = "CORE_VERSION"

private const val EXPECTED_INITIAL_VERSION = "1.0.0"
private const val NOT_INITIAL_VERSION = "1.0.1"

private const val PROPERTIES_FILE_CONTENT = """
    GROUP_ID=io.github.ackeecz
    POM_URL=https://github.com/AckeeCZ/ackee-security
    POM_DEVELOPER_ID=ackee
    POM_DEVELOPER_NAME=Ackee
    POM_DEVELOPER_EMAIL=info@ackee.cz
    POM_LICENCE_NAME=The Apache Software License, Version 2.0
    POM_LICENCE_URL=http://www.apache.org/licenses/LICENSE-2.0.txt
    POM_SCM_CONNECTION=scm:git:github.com/AckeeCZ/ackee-security.git
    POM_SCM_DEVELOPER_CONNECTION=scm:git:ssh://github.com/AckeeCZ/ackee-security.git
    POM_SCM_URL=https://github.com/AckeeCZ/ackee-security/tree/main
    
    # Core artifact
    CORE_ARTIFACT_ID=security-core
    CORE_POM_NAME=Ackee Security Core
    CORE_POM_YEAR=2024
    CORE_POM_DESCRIPTION=Core artifact of the Ackee Security library. Contains general-purpose security features.
"""

private lateinit var getLastTag: StubGetLastTag
private lateinit var executeCommand: StubExecuteCommand
private lateinit var properties: Properties

internal class GetArtifactVersionFromLastTagTest : FunSpec({

    fun createSut(): GetArtifactVersionFromLastTag {
        return GetArtifactVersionFromLastTagImpl(
            getLastTag = getLastTag,
            executeCommand = executeCommand,
        )
    }

    beforeEach {
        getLastTag = StubGetLastTag()
        executeCommand = StubExecuteCommand()
        properties = Properties().also { it.load(PROPERTIES_FILE_CONTENT.trimIndent().byteInputStream()) }
    }

    test("call correct git command with last tag") {
        val lastTagResult = LastTagResult.Tag("bom-1.0.0")
        getLastTag.result = lastTagResult
        val expectedCommand = "git show ${lastTagResult.value}:$PROPERTIES_FILE_NAME"

        runCatching { createSut().invoke(buildProject()) }

        executeCommand.commands.firstOrNull() shouldBe expectedCommand
    }

    test("throw if last tag exists, but git command for getting last tag properties fails") {
        getLastTag.result = LastTagResult.Tag("bom-1.0.0")
        executeCommand.resultStrategy = StubExecuteCommand.ResultStrategy.OneRepeating(
            createErrorExecuteCommandResult(),
        )
        val underTest = createSut()

        shouldThrow<LastTagPropertiesException> { underTest(buildProject()) }
    }

    test("get artifact version from last tag") {
        getLastTag.result = LastTagResult.Tag("bom-1.1.0")
        val expectedVersion = "1.0.0"
        properties[VERSION_PROPERTY_NAME] = expectedVersion
        executeCommand.resultStrategy = StubExecuteCommand.ResultStrategy.OneRepeating(
            // Return properties from the above tag
            ExecuteCommand.Result.Success(properties.writeToString())
        )
        val underTest = createSut()

        underTest(buildProject(name = PROJECT_NAME))?.value shouldBe expectedVersion
    }

    // This means that a new artifact was added to the library and was not released yet, which is a valid state
    @Suppress("MaxLineLength")
    test("get null artifact version when last tag exists, properties are parsed, but parsing of the version fails and current project version is initial one") {
        getLastTag.result = LastTagResult.Tag("bom-1.1.0")
        executeCommand.resultStrategy = StubExecuteCommand.ResultStrategy.OneRepeating(
            // Return properties from the above tag
            ExecuteCommand.Result.Success(properties.writeToString())
        )
        val project = buildProject(name = PROJECT_NAME).also { it.version = EXPECTED_INITIAL_VERSION }
        val underTest = createSut()

        underTest(project).shouldBeNull()
    }

    test("throw if last tag exists, properties are parsed, but parsing of the version fails and current project version is not initial one") {
        getLastTag.result = LastTagResult.Tag("bom-1.1.0")
        executeCommand.resultStrategy = StubExecuteCommand.ResultStrategy.OneRepeating(
            // Return properties from the above tag
            ExecuteCommand.Result.Success(properties.writeToString())
        )
        val project = buildProject(name = PROJECT_NAME).also { it.version = NOT_INITIAL_VERSION }
        val underTest = createSut()

        shouldThrow<VersionUnparseableException> { underTest(project) }
    }

    test("throw if last tag exists, but parsing of properties fails") {
        getLastTag.result = LastTagResult.Tag("bom-1.1.0")
        executeCommand.resultStrategy = StubExecuteCommand.ResultStrategy.OneRepeating(
            // Return properties from the above tag
            ExecuteCommand.Result.Success("invalid properties content")
        )
        val underTest = createSut()

        shouldThrow<VersionUnparseableException> { underTest(buildProject(name = PROJECT_NAME)) }
    }

    test("get null artifact version when fallback to first commit hash and project version matches the expected initial version") {
        getLastTag.result = LastTagResult.FirstCommitHash("de5035f5a24621ea5361279d867ad75abc967ca3")
        executeCommand.resultStrategy = StubExecuteCommand.ResultStrategy.OneRepeating(
            // Return properties from the above commit
            ExecuteCommand.Result.Success(properties.writeToString())
        )
        val project = buildProject().also { it.version = EXPECTED_INITIAL_VERSION }
        val underTest = createSut()

        underTest(project).shouldBeNull()
    }

    test("throw if fallback to first commit hash and project version does not match the expected initial version") {
        getLastTag.result = LastTagResult.FirstCommitHash("de5035f5a24621ea5361279d867ad75abc967ca3")
        executeCommand.resultStrategy = StubExecuteCommand.ResultStrategy.OneRepeating(
            // Return properties from the above commit
            ExecuteCommand.Result.Success(properties.writeToString())
        )
        val project = buildProject().also { it.version = NOT_INITIAL_VERSION }
        val underTest = createSut()

        shouldThrow<UnexpectedInitialVersionException> { underTest(project) }
    }
})

private fun Properties.writeToString(): String {
    return StringWriter().also {
        properties.store(it, "Properties content")
    }.toString()
}