package org.hrfilter.resume.batch

import org.hrfilter.resume.batchrun.BatchRun
import org.hrfilter.resume.batchrun.BatchRunStatus
import org.hrfilter.resume.infrastructure.batchrun.repository.BatchRunRepository
import org.hrfilter.resume.infrastructure.evaluation.repository.EvaluationResultRepository
import org.hrfilter.resume.infrastructure.jobposting.repository.JobPostingRepository
import org.hrfilter.resume.infrastructure.llm.EvaluationJob
import org.hrfilter.resume.infrastructure.llm.LlmEvaluator
import org.hrfilter.resume.infrastructure.parser.ResumeParser
import org.hrfilter.resume.infrastructure.resume.repository.ResumeRepository
import org.hrfilter.resume.infrastructure.storage.ResumeStorage
import org.hrfilter.resume.jobposting.JobPosting
import org.hrfilter.resume.jobposting.JobPostingIdentity
import org.hrfilter.resume.jobposting.of
import org.hrfilter.resume.resume.Resume
import org.hrfilter.resume.resume.ResumeStatus
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
) : BatchEvaluationService {
    override fun evaluate() {
        val resumes = resumeRepository.findAllByStatus(status = ResumeStatus.UPLOADED)
        if (resumes.isEmpty()) return

        val batchRun = batchRunRepository.save(batchRun = newRunningBatchRun(now = Instant.now()))

        // 3. 각 resume → storage.download → parser.parse → EvaluationJob 생성
        val jobPostingCache = mutableMapOf<Long, JobPosting>()
        val jobs: List<EvaluationJob> =
            resumes.map { resume ->
                resume.toEvaluationJob(loadJobPosting = jobPostingCache::lookup)
            }

        // TODO: 4. llmEvaluator.submitBatch(jobs)
        // TODO: 5. 결과 폴링
        // TODO: 6. EvaluationResult 저장 + BatchRun 카운트 ++ + Resume status=EVALUATED
        // TODO: 7. BatchRun complete + Notifier
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
        val text = resumeParser.parse(content = bytes, mimeType = mimeType)
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
}
