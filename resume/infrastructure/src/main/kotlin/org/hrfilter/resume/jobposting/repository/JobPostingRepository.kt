package org.hrfilter.resume.jobposting.repository

import org.hrfilter.resume.jobposting.JobPosting
import org.hrfilter.resume.jobposting.JobPostingIdentity

interface JobPostingRepository {
    fun save(jobPosting: JobPosting): JobPosting

    fun findByJobPostingIdentity(jobPostingIdentity: JobPostingIdentity): JobPosting?

    fun findAll(): List<JobPosting>
}
