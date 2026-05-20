package org.hrfilter.resume.jobposting

import org.hrfilter.resume.exception.JobPostingNotFoundException
import org.hrfilter.resume.infrastructure.jobposting.repository.JobPostingRepository

interface JobPostingService {
    fun get(identity: JobPostingIdentity): JobPosting
}

internal class JobPostingServiceImpl(
    private val jobPostingRepository: JobPostingRepository,
) : JobPostingService {
    override fun get(identity: JobPostingIdentity): JobPosting =
        jobPostingRepository.findByJobPostingIdentity(identity)
            ?: throw JobPostingNotFoundException()
}
