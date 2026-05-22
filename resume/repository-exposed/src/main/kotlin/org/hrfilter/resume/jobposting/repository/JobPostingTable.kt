package org.hrfilter.resume.jobposting.repository

import org.jetbrains.exposed.dao.id.LongIdTable
import org.jetbrains.exposed.sql.javatime.timestamp

internal object JobPostingTable : LongIdTable(name = "job_posting", columnName = "job_posting_id") {
    val title = varchar("title", 200)
    val description = text("description")
    val requirements = text("requirements")
    val createdAt = timestamp("created_at")
    val updatedAt = timestamp("updated_at")
}
