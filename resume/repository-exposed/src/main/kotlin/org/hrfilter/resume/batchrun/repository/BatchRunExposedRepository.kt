package org.hrfilter.resume.batchrun.repository

import org.hrfilter.resume.batchrun.BatchRun
import org.hrfilter.resume.batchrun.BatchRunIdentity
import org.hrfilter.resume.batchrun.BatchRunStatus
import org.hrfilter.resume.evaluation.EvaluationVerdict
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.SqlExpressionBuilder.plus
import org.jetbrains.exposed.sql.insertAndGetId
import org.jetbrains.exposed.sql.intLiteral
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update
import java.time.Instant

internal class BatchRunExposedRepository : BatchRunRepository {
    override fun save(batchRun: BatchRun): BatchRun =
        transaction {
            val newId =
                BatchRunTable.insertAndGetId {
                    it[status] = batchRun.status
                    it[evaluatedCount] = batchRun.evaluatedCount
                    it[passedCount] = batchRun.passedCount
                    it[holdCount] = batchRun.holdCount
                    it[rejectedCount] = batchRun.rejectedCount
                    it[failedCount] = batchRun.failedCount
                    it[startedAt] = batchRun.startedAt
                    it[completedAt] = batchRun.completedAt
                    it[providerBatchId] = batchRun.providerBatchId
                    it[createdAt] = batchRun.createdAt
                    it[updatedAt] = batchRun.updatedAt
                }
            batchRun.copy(batchRunId = newId.value)
        }

    override fun findByBatchRunIdentity(batchRunIdentity: BatchRunIdentity): BatchRun? =
        transaction {
            BatchRunTable
                .selectAll()
                .where { BatchRunTable.id eq batchRunIdentity.batchRunId }
                .singleOrNull()
                ?.toBatchRun()
        }

    override fun findByStatus(status: BatchRunStatus): List<BatchRun> =
        transaction {
            BatchRunTable
                .selectAll()
                .where { BatchRunTable.status eq status }
                .orderBy(BatchRunTable.startedAt to SortOrder.ASC)
                .map { it.toBatchRun() }
        }

    override fun findLatestN(
        limit: Int,
        offset: Int,
    ): List<BatchRun> =
        transaction {
            BatchRunTable
                .selectAll()
                .orderBy(BatchRunTable.startedAt to SortOrder.DESC)
                .limit(limit)
                .offset(offset.toLong())
                .map { it.toBatchRun() }
        }

    override fun incrementCounts(
        identity: BatchRunIdentity,
        verdict: EvaluationVerdict,
    ) {
        transaction {
            BatchRunTable.update({ BatchRunTable.id eq identity.batchRunId }) {
                it[evaluatedCount] = evaluatedCount + intLiteral(1)
                when (verdict) {
                    EvaluationVerdict.PASS -> it[passedCount] = passedCount + intLiteral(1)
                    EvaluationVerdict.HOLD -> it[holdCount] = holdCount + intLiteral(1)
                    EvaluationVerdict.REJECT -> it[rejectedCount] = rejectedCount + intLiteral(1)
                }
                it[updatedAt] = Instant.now()
            }
        }
    }

    override fun complete(
        identity: BatchRunIdentity,
        status: BatchRunStatus,
        completedAt: Instant,
    ) {
        transaction {
            BatchRunTable.update({ BatchRunTable.id eq identity.batchRunId }) {
                it[BatchRunTable.status] = status
                it[BatchRunTable.completedAt] = completedAt
                it[updatedAt] = Instant.now()
            }
        }
    }

    private fun ResultRow.toBatchRun(): BatchRun =
        BatchRun(
            batchRunId = this[BatchRunTable.id].value,
            status = this[BatchRunTable.status],
            evaluatedCount = this[BatchRunTable.evaluatedCount],
            passedCount = this[BatchRunTable.passedCount],
            holdCount = this[BatchRunTable.holdCount],
            rejectedCount = this[BatchRunTable.rejectedCount],
            failedCount = this[BatchRunTable.failedCount],
            startedAt = this[BatchRunTable.startedAt],
            completedAt = this[BatchRunTable.completedAt],
            providerBatchId = this[BatchRunTable.providerBatchId],
            createdAt = this[BatchRunTable.createdAt],
            updatedAt = this[BatchRunTable.updatedAt],
        )
}
