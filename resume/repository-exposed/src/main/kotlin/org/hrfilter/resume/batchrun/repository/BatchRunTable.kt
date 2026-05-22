package org.hrfilter.resume.batchrun.repository

import org.hrfilter.resume.batchrun.BatchRunStatus
import org.jetbrains.exposed.dao.id.LongIdTable
import org.jetbrains.exposed.sql.javatime.timestamp

internal object BatchRunTable : LongIdTable(name = "batch_run", columnName = "batch_run_id") {
    val status = enumerationByName("status", 20, BatchRunStatus::class)
    val evaluatedCount = integer("evaluated_count").default(0)
    val passedCount = integer("passed_count").default(0)
    val holdCount = integer("hold_count").default(0)
    val rejectedCount = integer("rejected_count").default(0)
    val failedCount = integer("failed_count").default(0)
    val startedAt = timestamp("started_at")
    val completedAt = timestamp("completed_at").nullable()
    val createdAt = timestamp("created_at")
    val updatedAt = timestamp("updated_at")
}
