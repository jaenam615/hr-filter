package org.hrfilter.resume.jobposting

import org.hrfilter.resume.exception.JobPostingNotFoundException
import org.hrfilter.resume.infrastructure.jobposting.repository.JobPostingRepository

interface JobPostingReaderService {
    fun get(jobPostingIdentity: JobPostingIdentity): JobPosting
}

internal class JobPostingReaderServiceImpl(
    private val jobPostingRepository: JobPostingRepository,
) : JobPostingReaderService {
    override fun get(jobPostingIdentity: JobPostingIdentity): JobPosting =
        jobPostingRepository.findByJobPostingIdentity(jobPostingIdentity = jobPostingIdentity)
            ?: throw JobPostingNotFoundException()
}
