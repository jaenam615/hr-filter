package org.hrfilter.resume.evaluation

import org.hrfilter.resume.batchrun.BatchRunIdentity
import org.hrfilter.resume.evaluation.repository.EvaluationResultRepository
import org.hrfilter.resume.exception.EvaluationResultNotFoundException
import org.hrfilter.resume.resume.ResumeIdentity

interface EvaluationReaderService {
    fun get(evaluationResultIdentity: EvaluationResultIdentity): EvaluationResult

    fun getAllByResume(resumeIdentity: ResumeIdentity): List<EvaluationResult>

    fun getAllByBatch(batchRunIdentity: BatchRunIdentity): List<EvaluationResult>
}

internal class EvaluationReaderServiceImpl(
    private val evaluationResultRepository: EvaluationResultRepository,
) : EvaluationReaderService {
    override fun get(evaluationResultIdentity: EvaluationResultIdentity): EvaluationResult =
        evaluationResultRepository.findByEvaluationResultIdentity(evaluationResultIdentity = evaluationResultIdentity)
            ?: throw EvaluationResultNotFoundException()

    override fun getAllByResume(resumeIdentity: ResumeIdentity): List<EvaluationResult> =
        evaluationResultRepository.findAllByResumeIdentity(resumeIdentity = resumeIdentity)

    override fun getAllByBatch(batchRunIdentity: BatchRunIdentity): List<EvaluationResult> =
        evaluationResultRepository.findAllByBatchRunIdentity(batchRunIdentity = batchRunIdentity)
}
