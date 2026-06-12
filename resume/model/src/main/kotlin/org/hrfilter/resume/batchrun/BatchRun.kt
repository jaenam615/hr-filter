package org.hrfilter.resume.batchrun

import java.time.Instant

data class BatchRun(
    override val batchRunId: Long,
    override val status: BatchRunStatus,
    override val evaluatedCount: Int,
    override val passedCount: Int,
    override val holdCount: Int,
    override val rejectedCount: Int,
    override val failedCount: Int,
    override val startedAt: Instant,
    override val completedAt: Instant?,
    override val providerBatchId: String?,
    override val createdAt: Instant,
    override val updatedAt: Instant,
) : BatchRunModel
