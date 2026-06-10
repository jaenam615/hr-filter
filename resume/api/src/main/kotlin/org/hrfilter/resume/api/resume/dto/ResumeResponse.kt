package org.hrfilter.resume.api.resume.dto

import org.hrfilter.resume.resume.Resume
import org.hrfilter.resume.resume.ResumeStatus
import java.time.Instant

class ResumeResponse(
    val resumeId: Long,
    val jobPostingId: Long,
    val applicantName: String,
    val applicantEmail: String,
    val status: ResumeStatus,
    val createdAt: Instant,
) {
    companion object {
        fun from(resume: Resume): ResumeResponse =
            ResumeResponse(
                resumeId = resume.resumeId,
                jobPostingId = resume.jobPostingId,
                applicantName = resume.applicantName,
                applicantEmail = resume.applicantEmail,
                status = resume.status,
                createdAt = resume.createdAt,
            )
    }
}
