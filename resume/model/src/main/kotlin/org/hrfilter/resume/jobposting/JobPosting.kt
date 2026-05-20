package org.hrfilter.resume.jobposting

import java.time.Instant

data class JobPosting(
    override val jobPostingId: Long,
    override val title: String,
    override val description: String,
    override val requirements: String,
    override val createdAt: Instant,
    override val updatedAt: Instant,
) : JobPostingModel
