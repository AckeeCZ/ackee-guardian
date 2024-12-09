package io.github.ackeecz.guardian.verification

import org.gradle.api.Project

internal interface GetTagStubDefinition : GetTag {

    var result: TagResult
}

internal class GetTagStub : GetTagStubDefinition {

    override var result: TagResult = TagResult.FirstCommitHash("")

    override fun invoke(project: Project): TagResult = result
}

internal class GetCurrentTagStub : GetCurrentTag, GetTagStubDefinition by GetTagStub()

internal class GetPreviousTagStub : GetPreviousTag, GetTagStubDefinition by GetTagStub()
