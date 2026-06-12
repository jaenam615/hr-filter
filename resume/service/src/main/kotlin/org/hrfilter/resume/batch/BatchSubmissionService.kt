package org.hrfilter.resume.batch

import org.hrfilter.resume.batchrun.BatchRun
import org.hrfilter.resume.batchrun.BatchRunStatus
import org.hrfilter.resume.batchrun.repository.BatchRunRepository
import org.hrfilter.resume.jobposting.JobPosting
import org.hrfilter.resume.jobposting.JobPostingIdentity
import org.hrfilter.resume.jobposting.of
import org.hrfilter.resume.jobposting.repository.JobPostingRepository
import org.hrfilter.resume.llm.EvaluationJob
import org.hrfilter.resume.llm.LlmEvaluator
import org.hrfilter.resume.parser.ResumeParser
import org.hrfilter.resume.resume.Resume
import org.hrfilter.resume.resume.ResumeIdentity
import org.hrfilter.resume.resume.ResumeStatus
import org.hrfilter.resume.resume.of
import org.hrfilter.resume.resume.repository.ResumeRepository
import org.hrfilter.resume.storage.ResumeStorage
import java.time.Instant

/**
 * 배치 제출(논블로킹). 신규 이력서를 모아 LLM 배치에 제출하고 즉시 리턴한다.
 * 제출 결과는 수거 잡([BatchCollectionService])이 폴링해 가져온다.
 */
interface BatchSubmissionService {
    fun submit()
}

internal class BatchSubmissionServiceImpl(
    private val batchRunRepository: BatchRunRepository,
    private val resumeRepository: ResumeRepository,
    private val jobPostingRepository: JobPostingRepository,
    private val storage: ResumeStorage,
    private val resumeParser: ResumeParser,
    private val llmEvaluator: LlmEvaluator,
) : BatchSubmissionService {
    override fun submit() {
        // 단일 in-flight 배치 불변식: 진행 중(RUNNING) 배치가 있으면 중복 제출하지 않는다.
        if (batchRunRepository.findByStatus(status = BatchRunStatus.RUNNING).isNotEmpty()) return

        val resumes = resumeRepository.findAllByStatus(status = ResumeStatus.UPLOADED)
        if (resumes.isEmpty()) return

        val jobPostingCache = mutableMapOf<Long, JobPosting>()
        val jobs = resumes.map { resume -> resume.toEvaluationJob { jobPostingCache.lookup(it) } }

        val handle = llmEvaluator.submitBatch(jobs = jobs)

        val now = Instant.now()
        batchRunRepository.save(
            batchRun =
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
                    providerBatchId = handle.providerBatchId,
                    createdAt = now,
                    updatedAt = now,
                ),
        )

        // 재제출 방지: 제출한 이력서를 SUBMITTED로 표시.
        resumes.forEach {
            resumeRepository.updateStatus(resumeIdentity = ResumeIdentity.of(resumeId = it.resumeId), status = ResumeStatus.SUBMITTED)
        }
    }

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
}
