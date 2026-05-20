package org.hrfilter.resume.evaluation

interface EvaluationResultIdentity {
    companion object

    val evaluationResultId: Long
}

internal data class SimpleEvaluationResultIdentity(
    override val evaluationResultId: Long,
) : EvaluationResultIdentity

fun EvaluationResultIdentity.Companion.of(evaluationResultId: Long): EvaluationResultIdentity =
    SimpleEvaluationResultIdentity(evaluationResultId)
