package org.hrfilter.resume.evaluation

import java.time.Instant

data class EvaluationResult(
    override val evaluationResultId: Long,
    override val resumeId: Long,
    override val batchRunId: Long,
    override val verdict: EvaluationVerdict,
    override val score: Int,
    override val breakdown: EvaluationBreakdown,
    override val reasoning: String,
    override val evaluatedAt: Instant,
    override val createdAt: Instant,
    override val updatedAt: Instant,
) : EvaluationResultModel
