package org.hrfilter.resume.llm

import org.hrfilter.resume.evaluation.EvaluationBreakdown
import org.hrfilter.resume.evaluation.EvaluationVerdict

interface LlmEvaluator {
    fun submitBatch(jobs: List<EvaluationJob>): BatchHandle

    fun pollResults(handle: BatchHandle): BatchResults
}

class EvaluationJob(
    val resumeId: Long,
    val resumeText: String,
    val jobPostingTitle: String,
    val jobPostingDescription: String,
    val jobPostingRequirements: String,
)

data class BatchHandle(
    val providerBatchId: String,
)

data class BatchResults(
    val status: BatchProcessingStatus,
    val items: List<EvaluationJobResult>,
)

enum class BatchProcessingStatus { IN_PROGRESS, COMPLETED, FAILED }

data class EvaluationJobResult(
    val resumeId: Long,
    val verdict: EvaluationVerdict,
    val score: Int,
    val breakdown: EvaluationBreakdown,
    val reasoning: String,
)
