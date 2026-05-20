package org.hrfilter.resume.jobposting

interface JobPostingIdentity {
    companion object

    val jobPostingId: Long
}

internal data class SimpleJobPostingIdentity(
    override val jobPostingId: Long,
) : JobPostingIdentity

fun JobPostingIdentity.Companion.of(jobPostingId: Long): JobPostingIdentity = SimpleJobPostingIdentity(jobPostingId)
