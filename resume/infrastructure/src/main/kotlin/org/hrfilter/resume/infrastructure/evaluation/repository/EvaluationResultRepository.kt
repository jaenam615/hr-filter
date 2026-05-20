package org.hrfilter.resume.infrastructure.evaluation.repository

import org.hrfilter.resume.batchrun.BatchRunIdentity
import org.hrfilter.resume.evaluation.EvaluationResult
import org.hrfilter.resume.evaluation.EvaluationResultIdentity
import org.hrfilter.resume.resume.ResumeIdentity

interface EvaluationResultRepository {
    fun save(evaluationResult: EvaluationResult): EvaluationResult

    fun saveAll(evaluationResults: List<EvaluationResult>): List<EvaluationResult>

    fun findByEvaluationResultIdentity(evaluationResultIdentity: EvaluationResultIdentity): EvaluationResult?

    fun findAllByResumeIdentity(resumeIdentity: ResumeIdentity): List<EvaluationResult>

    fun findAllByBatchRunIdentity(batchRunIdentity: BatchRunIdentity): List<EvaluationResult>
}
