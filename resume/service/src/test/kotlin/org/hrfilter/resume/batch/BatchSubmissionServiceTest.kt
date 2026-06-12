package org.hrfilter.resume.batch

import io.kotest.core.spec.style.DescribeSpec
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.hrfilter.resume.batchrun.BatchRun
import org.hrfilter.resume.batchrun.BatchRunStatus
import org.hrfilter.resume.batchrun.repository.BatchRunRepository
import org.hrfilter.resume.jobposting.JobPosting
import org.hrfilter.resume.jobposting.repository.JobPostingRepository
import org.hrfilter.resume.llm.BatchHandle
import org.hrfilter.resume.llm.LlmEvaluator
import org.hrfilter.resume.parser.ResumeParser
import org.hrfilter.resume.resume.Resume
import org.hrfilter.resume.resume.ResumeStatus
import org.hrfilter.resume.resume.repository.ResumeRepository
import org.hrfilter.resume.storage.ResumeStorage
import java.time.Instant

class BatchSubmissionServiceTest : DescribeSpec({
    describe("submit") {
        it("진행 중(RUNNING) 배치가 있으면 중복 제출하지 않는다") {
            val s = SubmitSut()
            every { s.batchRunRepository.findByStatus(BatchRunStatus.RUNNING) } returns listOf(runningRun())

            s.service.submit()

            verify(exactly = 0) { s.resumeRepository.findAllByStatus(any()) }
            verify(exactly = 0) { s.llmEvaluator.submitBatch(any()) }
        }

        it("UPLOADED 이력서가 없으면 제출하지 않는다") {
            val s = SubmitSut()
            every { s.batchRunRepository.findByStatus(BatchRunStatus.RUNNING) } returns emptyList()
            every { s.resumeRepository.findAllByStatus(ResumeStatus.UPLOADED) } returns emptyList()

            s.service.submit()

            verify(exactly = 0) { s.llmEvaluator.submitBatch(any()) }
            verify(exactly = 0) { s.batchRunRepository.save(any()) }
        }

        it("정상: 다운로드→파싱→제출→batch_run(RUNNING+providerBatchId) 저장→이력서 SUBMITTED") {
            val s = SubmitSut()
            every { s.batchRunRepository.findByStatus(BatchRunStatus.RUNNING) } returns emptyList()
            every { s.resumeRepository.findAllByStatus(ResumeStatus.UPLOADED) } returns listOf(uploadedResume())
            every { s.jobPostingRepository.findByJobPostingIdentity(any()) } returns jobPosting()
            every { s.storage.download(any()) } returns "이력서 원본".toByteArray()
            every { s.resumeParser.parse(any(), any()) } returns "파싱된 텍스트"
            every { s.llmEvaluator.submitBatch(any()) } returns BatchHandle(providerBatchId = "batch_xyz")

            s.service.submit()

            verify { s.llmEvaluator.submitBatch(any()) }
            verify {
                s.batchRunRepository.save(
                    match { it.status == BatchRunStatus.RUNNING && it.providerBatchId == "batch_xyz" },
                )
            }
            verify { s.resumeRepository.updateStatus(any(), ResumeStatus.SUBMITTED) }
        }
    }
})

private class SubmitSut {
    val batchRunRepository: BatchRunRepository = mockk(relaxed = true)
    val resumeRepository: ResumeRepository = mockk(relaxed = true)
    val jobPostingRepository: JobPostingRepository = mockk(relaxed = true)
    val storage: ResumeStorage = mockk(relaxed = true)
    val resumeParser: ResumeParser = mockk(relaxed = true)
    val llmEvaluator: LlmEvaluator = mockk(relaxed = true)

    val service =
        BatchSubmissionServiceImpl(
            batchRunRepository = batchRunRepository,
            resumeRepository = resumeRepository,
            jobPostingRepository = jobPostingRepository,
            storage = storage,
            resumeParser = resumeParser,
            llmEvaluator = llmEvaluator,
        )
}

private val SUBMIT_NOW: Instant = Instant.parse("2026-06-12T00:00:00Z")

private fun uploadedResume() =
    Resume(
        resumeId = 1L,
        jobPostingId = 100L,
        applicantName = "홍길동",
        applicantEmail = "hong@example.com",
        objectKey = "resumes/100/sample.txt",
        mimeType = "text/plain",
        status = ResumeStatus.UPLOADED,
        createdAt = SUBMIT_NOW,
        updatedAt = SUBMIT_NOW,
    )

private fun jobPosting() =
    JobPosting(
        jobPostingId = 100L,
        title = "백엔드 엔지니어",
        description = "코틀린 백엔드",
        requirements = "코틀린, Spring",
        createdAt = SUBMIT_NOW,
        updatedAt = SUBMIT_NOW,
    )

private fun runningRun() =
    BatchRun(
        batchRunId = 10L,
        status = BatchRunStatus.RUNNING,
        evaluatedCount = 0,
        passedCount = 0,
        holdCount = 0,
        rejectedCount = 0,
        failedCount = 0,
        startedAt = SUBMIT_NOW,
        completedAt = null,
        providerBatchId = "batch_old",
        createdAt = SUBMIT_NOW,
        updatedAt = SUBMIT_NOW,
    )
