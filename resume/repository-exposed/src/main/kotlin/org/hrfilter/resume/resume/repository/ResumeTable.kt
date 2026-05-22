package org.hrfilter.resume.resume.repository

import org.hrfilter.resume.jobposting.repository.JobPostingTable
import org.hrfilter.resume.resume.ResumeStatus
import org.jetbrains.exposed.dao.id.LongIdTable
import org.jetbrains.exposed.sql.javatime.timestamp

internal object ResumeTable : LongIdTable(name = "resume", columnName = "resume_id") {
    val jobPostingId = reference("job_posting_id", JobPostingTable)
    val applicantName = varchar("applicant_name", 100)
    val applicantEmail = varchar("applicant_email", 255)
    val objectKey = varchar("object_key", 500)
    val mimeType = varchar("mime_type", 100)
    val status = enumerationByName("status", 20, ResumeStatus::class)
    val createdAt = timestamp("created_at")
    val updatedAt = timestamp("updated_at")
}
