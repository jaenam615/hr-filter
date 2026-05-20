package org.hrfilter.resume.infrastructure.resume.repository

import org.hrfilter.resume.evaluation.EvaluationVerdict
import org.hrfilter.resume.jobposting.JobPostingIdentity
import org.hrfilter.resume.resume.Resume
import org.hrfilter.resume.resume.ResumeIdentity
import org.hrfilter.resume.resume.ResumeStatus
import java.time.ZonedDateTime

interface ResumeRepository {
    fun save(resume: Resume): Resume

    fun findByResumeIdentity(resumeIdentity: ResumeIdentity): Resume?

    fun findAllByStatusAndCreatedBefore(
        status: ResumeStatus,
        date: ZonedDateTime,
    ): List<Resume>

    fun updateStatus(
        resumeIdentity: ResumeIdentity,
        status: ResumeStatus,
    ): Resume
}
