package org.hrfilter.resume.jobposting.repository

import org.hrfilter.resume.jobposting.JobPosting
import org.hrfilter.resume.jobposting.JobPostingIdentity

interface JobPostingRepository {
    fun findByJobPostingIdentity(jobPostingIdentity: JobPostingIdentity): JobPosting?
}
