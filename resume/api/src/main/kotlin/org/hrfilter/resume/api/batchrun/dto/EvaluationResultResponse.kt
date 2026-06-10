package org.hrfilter.resume.api.batchrun.dto

import org.hrfilter.resume.evaluation.EvaluationResult
import org.hrfilter.resume.evaluation.EvaluationVerdict
import java.time.Instant

class EvaluationResultResponse(
    val evaluationResultId: Long,
    val resumeId: Long,
    val batchRunId: Long,
    val verdict: EvaluationVerdict,
    val score: Int,
    val reasoning: String,
    val evaluatedAt: Instant,
) {
    companion object {
        fun from(evaluationResult: EvaluationResult): EvaluationResultResponse =
            EvaluationResultResponse(
                evaluationResultId = evaluationResult.evaluationResultId,
                resumeId = evaluationResult.resumeId,
                batchRunId = evaluationResult.batchRunId,
                verdict = evaluationResult.verdict,
                score = evaluationResult.score,
                reasoning = evaluationResult.reasoning,
                evaluatedAt = evaluationResult.evaluatedAt,
            )
    }
}
