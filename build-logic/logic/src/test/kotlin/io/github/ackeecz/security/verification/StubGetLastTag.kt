package io.github.ackeecz.security.verification

import org.gradle.api.Project

internal class StubGetLastTag : GetLastTag {

    var result: LastTagResult = LastTagResult.FirstCommitHash("")

    override fun invoke(project: Project): LastTagResult = result
}
