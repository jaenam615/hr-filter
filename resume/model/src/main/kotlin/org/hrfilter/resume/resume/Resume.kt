package org.hrfilter.resume.resume

import java.time.Instant

data class Resume(
    override val resumeId: Long,
    override val jobPostingId: Long,
    override val applicantName: String,
    override val applicantEmail: String,
    override val objectKey: String,
    override val mimeType: String,
    override val status: ResumeStatus,
    override val createdAt: Instant,
    override val updatedAt: Instant,
) : ResumeModel
