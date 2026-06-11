package org.hrfilter.resume.batch

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.hrfilter.resume.batchrun.BatchRun
import org.hrfilter.resume.batchrun.BatchRunStatus
import org.hrfilter.resume.batchrun.repository.BatchRunRepository
import org.hrfilter.resume.evaluation.EvaluationBreakdown
import org.hrfilter.resume.evaluation.EvaluationVerdict
import org.hrfilter.resume.evaluation.repository.EvaluationResultRepository
import org.hrfilter.resume.jobposting.JobPosting
import org.hrfilter.resume.jobposting.repository.JobPostingRepository
import org.hrfilter.resume.llm.BatchHandle
import org.hrfilter.resume.llm.BatchProcessingStatus
import org.hrfilter.resume.llm.BatchResults
import org.hrfilter.resume.llm.EvaluationJobResult
import org.hrfilter.resume.llm.LlmEvaluator
import org.hrfilter.resume.notifier.Notifier
import org.hrfilter.resume.parser.ResumeParser
import org.hrfilter.resume.resume.Resume
import org.hrfilter.resume.resume.ResumeStatus
import org.hrfilter.resume.resume.repository.ResumeRepository
import org.hrfilter.resume.storage.ResumeStorage
import java.time.Instant

// 모든 Out-Port를 relaxed mock으로 두고, 반환값을 코드가 실제로 쓰는 메서드만 명시 스텁한다.
// (Thread.sleep(30s)가 있는 IN_PROGRESS 폴링 루프는 첫 폴링이 COMPLETED/FAILED를 돌려주는
//  경로만 테스트해 회피한다.)
class BatchEvaluationServiceTest : DescribeSpec({
    describe("evaluate") {
        it("UPLOADED 이력서가 없으면 배치를 시작하지 않는다") {
            val s = Sut()
            every { s.resumeRepository.findAllByStatus(ResumeStatus.UPLOADED) } returns emptyList()

            s.service.evaluate()

            verify(exactly = 0) { s.batchRunRepository.save(any()) }
            verify(exactly = 0) { s.llmEvaluator.submitBatch(any()) }
        }

        it("정상 흐름: 다운로드→파싱→제출→결과 저장→COMPLETED 알림") {
            val s = Sut()
            s.stubHappyPath(
                results =
                    BatchResults(
                        status = BatchProcessingStatus.COMPLETED,
                        items = listOf(passResult(resumeId = 1L)),
                    ),
                finalRun = finalRun(status = BatchRunStatus.COMPLETED, passed = 1, evaluated = 1),
            )

            s.service.evaluate()

            verify { s.llmEvaluator.submitBatch(any()) }
            verify { s.batchRunRepository.incrementCounts(any(), EvaluationVerdict.PASS) }
            verify { s.resumeRepository.updateStatus(any(), ResumeStatus.EVALUATED) }
            verify {
                s.evaluationResultRepository.saveAll(
                    match { it.size == 1 && it.first().verdict == EvaluationVerdict.PASS && it.first().score == 85 },
                )
            }
            verify { s.batchRunRepository.complete(any(), BatchRunStatus.COMPLETED, any()) }
            verify {
                s.notifier.notify(match { it.status == BatchRunStatus.COMPLETED && it.passedCount == 1 })
            }
        }

        it("LLM 결과가 FAILED면 결과 저장 없이 배치를 FAILED 처리한다") {
            val s = Sut()
            s.stubHappyPath(
                results = BatchResults(status = BatchProcessingStatus.FAILED, items = emptyList()),
                finalRun = finalRun(status = BatchRunStatus.FAILED),
            )

            s.service.evaluate()

            verify(exactly = 0) { s.evaluationResultRepository.saveAll(any()) }
            verify { s.batchRunRepository.complete(any(), BatchRunStatus.FAILED, any()) }
            verify { s.notifier.notify(match { it.status == BatchRunStatus.FAILED }) }
        }

        it("중간 예외가 나면 배치를 FAILED 처리하고 예외를 전파한다") {
            val s = Sut()
            s.stubHappyPath(
                results = BatchResults(status = BatchProcessingStatus.COMPLETED, items = emptyList()),
                finalRun = finalRun(status = BatchRunStatus.FAILED),
            )
            every { s.llmEvaluator.submitBatch(any()) } throws RuntimeException("boom")

            shouldThrow<RuntimeException> { s.service.evaluate() }

            verify { s.batchRunRepository.complete(any(), BatchRunStatus.FAILED, any()) }
        }
    }
})

private class Sut {
    val batchRunRepository: BatchRunRepository = mockk(relaxed = true)
    val resumeRepository: ResumeRepository = mockk(relaxed = true)
    val jobPostingRepository: JobPostingRepository = mockk(relaxed = true)
    val evaluationResultRepository: EvaluationResultRepository = mockk(relaxed = true)
    val storage: ResumeStorage = mockk(relaxed = true)
    val resumeParser: ResumeParser = mockk(relaxed = true)
    val llmEvaluator: LlmEvaluator = mockk(relaxed = true)
    val notifier: Notifier = mockk(relaxed = true)

    val service =
        BatchEvaluationServiceImpl(
            batchRunRepository = batchRunRepository,
            resumeRepository = resumeRepository,
            jobPostingRepository = jobPostingRepository,
            evaluationResultRepository = evaluationResultRepository,
            storage = storage,
            resumeParser = resumeParser,
            llmEvaluator = llmEvaluator,
            notifier = notifier,
        )

    fun stubHappyPath(
        results: BatchResults,
        finalRun: BatchRun,
    ) {
        every { resumeRepository.findAllByStatus(ResumeStatus.UPLOADED) } returns listOf(uploadedResume())
        every { batchRunRepository.save(any()) } returns batchRun(BatchRunStatus.RUNNING)
        every { jobPostingRepository.findByJobPostingIdentity(any()) } returns jobPosting()
        every { storage.download(any()) } returns "이력서 원본".toByteArray()
        every { resumeParser.parse(any(), any()) } returns "파싱된 이력서 텍스트"
        every { llmEvaluator.submitBatch(any()) } returns BatchHandle(providerBatchId = "batch_123")
        every { llmEvaluator.pollResults(any()) } returns results
        every { batchRunRepository.findByBatchRunIdentity(any()) } returns finalRun
    }
}

private val NOW: Instant = Instant.parse("2026-06-11T00:00:00Z")

private fun uploadedResume() =
    Resume(
        resumeId = 1L,
        jobPostingId = 100L,
        applicantName = "홍길동",
        applicantEmail = "hong@example.com",
        objectKey = "resumes/100/sample.txt",
        mimeType = "text/plain",
        status = ResumeStatus.UPLOADED,
        createdAt = NOW,
        updatedAt = NOW,
    )

private fun jobPosting() =
    JobPosting(
        jobPostingId = 100L,
        title = "백엔드 엔지니어",
        description = "코틀린 백엔드",
        requirements = "코틀린, Spring",
        createdAt = NOW,
        updatedAt = NOW,
    )

private fun passResult(resumeId: Long) =
    EvaluationJobResult(
        resumeId = resumeId,
        verdict = EvaluationVerdict.PASS,
        score = 85,
        breakdown = EvaluationBreakdown(education = 80, experience = 90, skills = 85, fit = 80),
        reasoning = "요구사항 충족",
    )

private fun finalRun(
    status: BatchRunStatus,
    passed: Int = 0,
    evaluated: Int = 0,
) = batchRun(status = status, passed = passed, evaluated = evaluated)

private fun batchRun(
    status: BatchRunStatus,
    passed: Int = 0,
    evaluated: Int = 0,
) = BatchRun(
    batchRunId = 10L,
    status = status,
    evaluatedCount = evaluated,
    passedCount = passed,
    holdCount = 0,
    rejectedCount = 0,
    failedCount = 0,
    startedAt = NOW,
    completedAt = null,
    createdAt = NOW,
    updatedAt = NOW,
)
