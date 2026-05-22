package org.hrfilter.resume.resume

import org.hrfilter.resume.exception.ResumeNotFoundException
import org.hrfilter.resume.resume.repository.ResumeRepository

interface ResumeReaderService {
    fun get(resumeIdentity: ResumeIdentity): Resume
}

internal class ResumeReaderServiceImpl(
    private val resumeRepository: ResumeRepository,
) : ResumeReaderService {
    override fun get(resumeIdentity: ResumeIdentity): Resume =
        resumeRepository.findByResumeIdentity(resumeIdentity = resumeIdentity)
            ?: throw ResumeNotFoundException()
}
