package org.hrfilter.resume.batch

import org.hrfilter.resume.batchrun.BatchRun
import org.hrfilter.resume.batchrun.BatchRunStatus
import org.hrfilter.resume.evaluation.EvaluationResult
import org.hrfilter.resume.infrastructure.batchrun.repository.BatchRunRepository
import org.hrfilter.resume.infrastructure.evaluation.repository.EvaluationResultRepository
import org.hrfilter.resume.infrastructure.jobposting.repository.JobPostingRepository
import org.hrfilter.resume.infrastructure.llm.BatchProcessingStatus
import org.hrfilter.resume.infrastructure.llm.BatchResults
import org.hrfilter.resume.infrastructure.llm.EvaluationJob
import org.hrfilter.resume.infrastructure.llm.LlmEvaluator
import org.hrfilter.resume.infrastructure.notifier.BatchSummary
import org.hrfilter.resume.infrastructure.notifier.Notifier
import org.hrfilter.resume.infrastructure.parser.ResumeParser
import org.hrfilter.resume.infrastructure.resume.repository.ResumeRepository
import org.hrfilter.resume.infrastructure.storage.ResumeStorage
import org.hrfilter.resume.jobposting.JobPosting
import org.hrfilter.resume.jobposting.JobPostingIdentity
import org.hrfilter.resume.jobposting.of
import org.hrfilter.resume.resume.Resume
import org.hrfilter.resume.resume.ResumeIdentity
import org.hrfilter.resume.resume.ResumeStatus
import org.hrfilter.resume.resume.of
import java.time.Instant

interface BatchEvaluationService {
    fun evaluate()
}

internal class BatchEvaluationServiceImpl(
    private val batchRunRepository: BatchRunRepository,
    private val resumeRepository: ResumeRepository,
    private val jobPostingRepository: JobPostingRepository,
    private val evaluationResultRepository: EvaluationResultRepository,
    private val storage: ResumeStorage,
    private val resumeParser: ResumeParser,
    private val llmEvaluator: LlmEvaluator,
    private val notifier: Notifier,
) : BatchEvaluationService {
    override fun evaluate() {
        val resumes = resumeRepository.findAllByStatus(status = ResumeStatus.UPLOADED)
        if (resumes.isEmpty()) return

        val batchRun = batchRunRepository.save(batchRun = newRunningBatchRun(now = Instant.now()))

        try {
            val jobPostingCache = mutableMapOf<Long, JobPosting>()
            val jobs =
                resumes.map { resume ->
                    resume.toEvaluationJob(loadJobPosting = { jobPostingCache.lookup(it) })
                }

            val batchHandle = llmEvaluator.submitBatch(jobs = jobs)
            var batchResults = llmEvaluator.pollResults(handle = batchHandle)
            while (batchResults.status == BatchProcessingStatus.IN_PROGRESS) {
                Thread.sleep(30_000)
                batchResults = llmEvaluator.pollResults(handle = batchHandle)
            }
            if (batchResults.status == BatchProcessingStatus.FAILED) {
                finishBatch(batchRun = batchRun, status = BatchRunStatus.FAILED)
                return
            }
            saveBatchResults(batchResults = batchResults, batchRun = batchRun)

            finishBatch(batchRun = batchRun, status = BatchRunStatus.COMPLETED)
        } catch (e: Exception) {
            finishBatch(batchRun = batchRun, status = BatchRunStatus.FAILED)
            throw e
        }
    }

    private fun finishBatch(
        batchRun: BatchRun,
        status: BatchRunStatus,
    ) {
        batchRunRepository.complete(
            identity = batchRun,
            status = status,
            completedAt = Instant.now(),
        )
        val finalRun =
            batchRunRepository.findByBatchRunIdentity(batchRunIdentity = batchRun)
                ?: error("batch run vanished after complete: batchRunId=${batchRun.batchRunId}")
        notifier.notify(
            summary =
                BatchSummary(
                    batchRunId = finalRun.batchRunId,
                    status = finalRun.status,
                    evaluatedCount = finalRun.evaluatedCount,
                    passedCount = finalRun.passedCount,
                    holdCount = finalRun.holdCount,
                    rejectedCount = finalRun.rejectedCount,
                    failedCount = finalRun.failedCount,
                    dashboardUrl = null,
                ),
        )
    }

    private fun newRunningBatchRun(now: Instant): BatchRun =
        BatchRun(
            batchRunId = 0L,
            status = BatchRunStatus.RUNNING,
            evaluatedCount = 0,
            passedCount = 0,
            holdCount = 0,
            rejectedCount = 0,
            failedCount = 0,
            startedAt = now,
            completedAt = null,
            createdAt = now,
            updatedAt = now,
        )

    private fun Resume.toEvaluationJob(loadJobPosting: (Long) -> JobPosting): EvaluationJob {
        val bytes =
            storage.download(objectKey = objectKey)
                ?: error("resume file missing: resumeId=$resumeId, objectKey=$objectKey")
        val text = resumeParser.parse(input = bytes, mimeType = mimeType)
        val jobPosting = loadJobPosting(jobPostingId)
        return EvaluationJob(
            resumeId = resumeId,
            resumeText = text,
            jobPostingTitle = jobPosting.title,
            jobPostingDescription = jobPosting.description,
            jobPostingRequirements = jobPosting.requirements,
        )
    }

    private fun MutableMap<Long, JobPosting>.lookup(jobPostingId: Long): JobPosting =
        getOrPut(jobPostingId) {
            jobPostingRepository.findByJobPostingIdentity(
                jobPostingIdentity = JobPostingIdentity.of(jobPostingId = jobPostingId),
            ) ?: error("job posting missing: jobPostingId=$jobPostingId")
        }

    private fun saveBatchResults(
        batchResults: BatchResults,
        batchRun: BatchRun,
    ) {
        val now = Instant.now()
        val batchRunId = batchRun.batchRunId
        val results =
            batchResults.items.map { item ->
                batchRunRepository.incrementCounts(identity = batchRun, verdict = item.verdict)
                resumeRepository.updateStatus(
                    resumeIdentity = ResumeIdentity.of(resumeId = item.resumeId),
                    status = ResumeStatus.EVALUATED,
                )
                EvaluationResult(
                    evaluationResultId = 0L,
                    resumeId = item.resumeId,
                    batchRunId = batchRunId,
                    verdict = item.verdict,
                    score = item.score,
                    breakdown = item.breakdown,
                    reasoning = item.reasoning,
                    evaluatedAt = now,
                    createdAt = now,
                    updatedAt = now,
                )
            }
        evaluationResultRepository.saveAll(evaluationResults = results)
    }
}
