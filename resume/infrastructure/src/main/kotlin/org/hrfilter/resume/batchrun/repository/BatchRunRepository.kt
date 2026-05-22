package org.hrfilter.resume.batchrun.repository

import org.hrfilter.resume.batchrun.BatchRun
import org.hrfilter.resume.batchrun.BatchRunIdentity
import org.hrfilter.resume.batchrun.BatchRunStatus
import org.hrfilter.resume.evaluation.EvaluationVerdict
import org.hrfilter.resume.resume.Resume
import java.time.Instant

interface BatchRunRepository {
    fun save(batchRun: BatchRun): BatchRun

    fun findByBatchRunIdentity(batchRunIdentity: BatchRunIdentity): BatchRun?

    fun findLatestN(
        limit: Int,
        offset: Int,
    ): List<BatchRun>

    fun incrementCounts(
        identity: BatchRunIdentity,
        verdict: EvaluationVerdict,
    )

    fun complete(
        identity: BatchRunIdentity,
        status: BatchRunStatus,
        completedAt: Instant,
    )
}
