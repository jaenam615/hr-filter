package org.hrfilter.resume.notifier

import org.hrfilter.resume.batchrun.BatchRunStatus

interface Notifier {
    fun notify(summary: BatchSummary)
}

data class BatchSummary(
    val batchRunId: Long,
    val status: BatchRunStatus,
    val evaluatedCount: Int,
    val passedCount: Int,
    val holdCount: Int,
    val rejectedCount: Int,
    val failedCount: Int,
    val dashboardUrl: String?,
)
