package org.hrfilter.resume.evaluation.repository

import org.hrfilter.resume.batchrun.BatchRunIdentity
import org.hrfilter.resume.evaluation.EvaluationResult
import org.hrfilter.resume.evaluation.EvaluationResultIdentity
import org.hrfilter.resume.resume.ResumeIdentity
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.batchInsert
import org.jetbrains.exposed.sql.insertAndGetId
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction

internal class EvaluationResultExposedRepository : EvaluationResultRepository {
    override fun save(evaluationResult: EvaluationResult): EvaluationResult =
        transaction {
            val newId =
                EvaluationResultTable.insertAndGetId {
                    it[resumeId] = evaluationResult.resumeId
                    it[batchRunId] = evaluationResult.batchRunId
                    it[verdict] = evaluationResult.verdict
                    it[score] = evaluationResult.score
                    it[breakdown] = evaluationResult.breakdown
                    it[reasoning] = evaluationResult.reasoning
                    it[evaluatedAt] = evaluationResult.evaluatedAt
                    it[createdAt] = evaluationResult.createdAt
                    it[updatedAt] = evaluationResult.updatedAt
                }
            evaluationResult.copy(evaluationResultId = newId.value)
        }

    override fun saveAll(evaluationResults: List<EvaluationResult>): List<EvaluationResult> =
        transaction {
            val rows =
                EvaluationResultTable.batchInsert(evaluationResults) { item ->
                    this[EvaluationResultTable.resumeId] = item.resumeId
                    this[EvaluationResultTable.batchRunId] = item.batchRunId
                    this[EvaluationResultTable.verdict] = item.verdict
                    this[EvaluationResultTable.score] = item.score
                    this[EvaluationResultTable.breakdown] = item.breakdown
                    this[EvaluationResultTable.reasoning] = item.reasoning
                    this[EvaluationResultTable.evaluatedAt] = item.evaluatedAt
                    this[EvaluationResultTable.createdAt] = item.createdAt
                    this[EvaluationResultTable.updatedAt] = item.updatedAt
                }
            evaluationResults.zip(rows) { result, row ->
                result.copy(evaluationResultId = row[EvaluationResultTable.id].value)
            }
        }

    override fun findByEvaluationResultIdentity(evaluationResultIdentity: EvaluationResultIdentity): EvaluationResult? =
        transaction {
            EvaluationResultTable
                .selectAll()
                .where { EvaluationResultTable.id eq evaluationResultIdentity.evaluationResultId }
                .singleOrNull()
                ?.toEvaluationResult()
        }

    override fun findAllByResumeIdentity(resumeIdentity: ResumeIdentity): List<EvaluationResult> =
        transaction {
            EvaluationResultTable
                .selectAll()
                .where { EvaluationResultTable.resumeId eq resumeIdentity.resumeId }
                .map { it.toEvaluationResult() }
        }

    override fun findAllByBatchRunIdentity(batchRunIdentity: BatchRunIdentity): List<EvaluationResult> =
        transaction {
            EvaluationResultTable
                .selectAll()
                .where { EvaluationResultTable.batchRunId eq batchRunIdentity.batchRunId }
                .map { it.toEvaluationResult() }
        }

    private fun ResultRow.toEvaluationResult(): EvaluationResult =
        EvaluationResult(
            evaluationResultId = this[EvaluationResultTable.id].value,
            resumeId = this[EvaluationResultTable.resumeId].value,
            batchRunId = this[EvaluationResultTable.batchRunId].value,
            verdict = this[EvaluationResultTable.verdict],
            score = this[EvaluationResultTable.score],
            breakdown = this[EvaluationResultTable.breakdown],
            reasoning = this[EvaluationResultTable.reasoning],
            evaluatedAt = this[EvaluationResultTable.evaluatedAt],
            createdAt = this[EvaluationResultTable.createdAt],
            updatedAt = this[EvaluationResultTable.updatedAt],
        )
}
