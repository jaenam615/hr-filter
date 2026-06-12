package org.hrfilter.resume.api.jobposting.dto

import jakarta.validation.constraints.NotBlank
import org.hrfilter.resume.jobposting.JobPosting
import java.time.Instant

data class JobPostingCreateRequest(
    @field:NotBlank(message = "title은 필수입니다")
    val title: String,
    @field:NotBlank(message = "description은 필수입니다")
    val description: String,
    @field:NotBlank(message = "requirements는 필수입니다")
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
