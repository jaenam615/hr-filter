package org.hrfilter.resume.jobposting.repository

import org.hrfilter.resume.jobposting.JobPosting
import org.hrfilter.resume.jobposting.JobPostingIdentity
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction

internal class JobPostingExposedRepository : JobPostingRepository {
    override fun findByJobPostingIdentity(jobPostingIdentity: JobPostingIdentity): JobPosting? =
        transaction {
            JobPostingTable
                .selectAll()
                .where { JobPostingTable.id eq jobPostingIdentity.jobPostingId }
                .singleOrNull()
                ?.toJobPosting()
        }

    private fun ResultRow.toJobPosting(): JobPosting =
        JobPosting(
            jobPostingId = this[JobPostingTable.id].value,
            title = this[JobPostingTable.title],
            description = this[JobPostingTable.description],
            requirements = this[JobPostingTable.requirements],
            createdAt = this[JobPostingTable.createdAt],
            updatedAt = this[JobPostingTable.updatedAt],
        )
}
