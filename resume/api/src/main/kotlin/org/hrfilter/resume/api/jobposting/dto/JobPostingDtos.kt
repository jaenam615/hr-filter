package org.hrfilter.resume.api.jobposting.dto

import org.hrfilter.resume.jobposting.JobPosting
import java.time.Instant

data class JobPostingCreateRequest(
    val title: String,
    val description: String,
    val requirements: String,
)

class JobPostingResponse(
    val jobPostingId: Long,
    val title: String,
    val description: String,
    val requirements: String,
    val createdAt: Instant,
) {
    companion object {
        fun from(jobPosting: JobPosting): JobPostingResponse =
            JobPostingResponse(
                jobPostingId = jobPosting.jobPostingId,
                title = jobPosting.title,
                description = jobPosting.description,
                requirements = jobPosting.requirements,
                createdAt = jobPosting.createdAt,
            )
    }
}
