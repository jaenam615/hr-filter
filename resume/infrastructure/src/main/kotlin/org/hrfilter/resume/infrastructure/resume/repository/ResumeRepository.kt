package org.hrfilter.resume.infrastructure.resume.repository

import org.hrfilter.resume.resume.Resume
import org.hrfilter.resume.resume.ResumeIdentity
import org.hrfilter.resume.resume.ResumeStatus

interface ResumeRepository {
    fun save(resume: Resume): Resume

    fun findByResumeIdentity(resumeIdentity: ResumeIdentity): Resume?

    fun findAllByStatus(status: ResumeStatus): List<Resume>

    fun updateStatus(
        resumeIdentity: ResumeIdentity,
        status: ResumeStatus,
    ): Resume
}
