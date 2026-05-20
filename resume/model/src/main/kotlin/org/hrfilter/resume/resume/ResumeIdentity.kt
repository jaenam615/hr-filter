package org.hrfilter.resume.resume

interface ResumeIdentity {
    companion object

    val resumeId: Long
}

internal data class SimpleResumeIdentity(
    override val resumeId: Long,
) : ResumeIdentity

fun ResumeIdentity.Companion.of(resumeId: Long): ResumeIdentity = SimpleResumeIdentity(resumeId)
