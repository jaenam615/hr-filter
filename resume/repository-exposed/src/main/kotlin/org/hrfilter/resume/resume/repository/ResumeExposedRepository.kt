package org.hrfilter.resume.resume.repository

import org.hrfilter.resume.resume.Resume
import org.hrfilter.resume.resume.ResumeIdentity
import org.hrfilter.resume.resume.ResumeStatus
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.insertAndGetId
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update
import java.time.Instant

internal class ResumeExposedRepository : ResumeRepository {
    override fun save(resume: Resume): Resume =
        transaction {
            val newId = ResumeTable.insertAndGetId {
                it[jobPostingId] = resume.jobPostingId
                it[applicantName] = resume.applicantName
                it[applicantEmail] = resume.applicantEmail
                it[objectKey] = resume.objectKey
                it[mimeType] = resume.mimeType
                it[status] = resume.status
                it[createdAt] = resume.createdAt
                it[updatedAt] = resume.updatedAt
            }
            resume.copy(resumeId = newId.value)
        }

    override fun findByResumeIdentity(resumeIdentity: ResumeIdentity): Resume? =
        transaction {
            ResumeTable
                .selectAll()
                .where { ResumeTable.id eq resumeIdentity.resumeId }
                .singleOrNull()
                ?.toResume()
        }

    override fun findAllByStatus(status: ResumeStatus): List<Resume> =
        transaction {
            ResumeTable
                .selectAll()
                .where { ResumeTable.status eq status }
                .map { it.toResume() }
        }

    override fun updateStatus(
        resumeIdentity: ResumeIdentity,
        status: ResumeStatus,
    ): Resume =
        transaction {
            ResumeTable.update({ ResumeTable.id eq resumeIdentity.resumeId }) {
                it[ResumeTable.status] = status
                it[updatedAt] = Instant.now()
            }
            ResumeTable
                .selectAll()
                .where { ResumeTable.id eq resumeIdentity.resumeId }
                .single()
                .toResume()
        }

    private fun ResultRow.toResume(): Resume =
        Resume(
            resumeId = this[ResumeTable.id].value,
            jobPostingId = this[ResumeTable.jobPostingId].value,
            applicantName = this[ResumeTable.applicantName],
            applicantEmail = this[ResumeTable.applicantEmail],
            objectKey = this[ResumeTable.objectKey],
            mimeType = this[ResumeTable.mimeType],
            status = this[ResumeTable.status],
            createdAt = this[ResumeTable.createdAt],
            updatedAt = this[ResumeTable.updatedAt],
        )
}
