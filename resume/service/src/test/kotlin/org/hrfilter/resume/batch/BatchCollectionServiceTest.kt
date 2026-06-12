package org.hrfilter.resume.batch

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
import org.hrfilter.resume.llm.BatchProcessingStatus
import org.hrfilter.resume.llm.BatchResults
import org.hrfilter.resume.llm.EvaluationJobResult
import org.hrfilter.resume.llm.LlmEvaluator
import org.hrfilter.resume.notifier.Notifier
import org.hrfilter.resume.resume.ResumeStatus
import org.hrfilter.resume.resume.repository.ResumeRepository
import java.time.Duration
import java.time.Instant

class BatchCollectionServiceTest : DescribeSpec({
    describe("collect") {
        it("RUNNING 배치가 없으면 폴링하지 않는다") {
            val s = CollectSut()
            every { s.batchRunRepository.findByStatus(BatchRunStatus.RUNNING) } returns emptyList()

            s.service.collect()

            verify(exactly = 0) { s.llmEvaluator.pollResults(any()) }
        }

        it("COMPLETED면 결과 저장 + 이력서 EVALUATED + complete(COMPLETED) + 알림") {
            val s = CollectSut()
            s.stubRunning(running())
            every { s.llmEvaluator.pollResults(any()) } returns
                BatchResults(status = BatchProcessingStatus.COMPLETED, items = listOf(passResult(1L)))
            every { s.batchRunRepository.findByBatchRunIdentity(any()) } returns
                running().copy(status = BatchRunStatus.COMPLETED, evaluatedCount = 1, passedCount = 1)

            s.service.collect()

            verify { s.batchRunRepository.incrementCounts(any(), EvaluationVerdict.PASS) }
            verify { s.resumeRepository.updateStatus(any(), ResumeStatus.EVALUATED) }
            verify { s.evaluationResultRepository.saveAll(match { it.size == 1 }) }
            verify { s.batchRunRepository.complete(any(), BatchRunStatus.COMPLETED, any()) }
            verify { s.notifier.notify(match { it.status == BatchRunStatus.COMPLETED }) }
        }

        it("IN_PROGRESS이고 stale이 아니면 아무 변경 없음") {
            val s = CollectSut()
            s.stubRunning(running(startedAt = Instant.now()))
            every { s.llmEvaluator.pollResults(any()) } returns
                BatchResults(status = BatchProcessingStatus.IN_PROGRESS, items = emptyList())

            s.service.collect()

            verify(exactly = 0) { s.evaluationResultRepository.saveAll(any()) }
            verify(exactly = 0) { s.batchRunRepository.complete(any(), any(), any()) }
            verify(exactly = 0) { s.notifier.notify(any()) }
        }

        it("IN_PROGRESS여도 maxAge를 넘으면 FAILED 처리하고 SUBMITTED를 되돌린다") {
            val s = CollectSut(maxAgeHours = 25)
            s.stubRunning(running(startedAt = Instant.now().minus(Duration.ofHours(26))))
            every { s.llmEvaluator.pollResults(any()) } returns
                BatchResults(status = BatchProcessingStatus.IN_PROGRESS, items = emptyList())
            every { s.batchRunRepository.findByBatchRunIdentity(any()) } returns
                running().copy(status = BatchRunStatus.FAILED)

            s.service.collect()

            verify { s.resumeRepository.findAllByStatus(ResumeStatus.SUBMITTED) }
            verify { s.batchRunRepository.complete(any(), BatchRunStatus.FAILED, any()) }
            verify { s.notifier.notify(match { it.status == BatchRunStatus.FAILED }) }
        }

        it("FAILED면 결과 저장 없이 FAILED 처리한다") {
            val s = CollectSut()
            s.stubRunning(running())
            every { s.llmEvaluator.pollResults(any()) } returns
                BatchResults(status = BatchProcessingStatus.FAILED, items = emptyList())
            every { s.batchRunRepository.findByBatchRunIdentity(any()) } returns
                running().copy(status = BatchRunStatus.FAILED)

            s.service.collect()

            verify(exactly = 0) { s.evaluationResultRepository.saveAll(any()) }
            verify { s.batchRunRepository.complete(any(), BatchRunStatus.FAILED, any()) }
        }
    }
})

private class CollectSut(maxAgeHours: Long = 25) {
    val batchRunRepository: BatchRunRepository = mockk(relaxed = true)
    val resumeRepository: ResumeRepository = mockk(relaxed = true)
    val evaluationResultRepository: EvaluationResultRepository = mockk(relaxed = true)
    val llmEvaluator: LlmEvaluator = mockk(relaxed = true)
    val notifier: Notifier = mockk(relaxed = true)

    val service =
        BatchCollectionServiceImpl(
            batchRunRepository = batchRunRepository,
            resumeRepository = resumeRepository,
            evaluationResultRepository = evaluationResultRepository,
            llmEvaluator = llmEvaluator,
            notifier = notifier,
            maxAgeHours = maxAgeHours,
        )

    fun stubRunning(run: BatchRun) {
        every { batchRunRepository.findByStatus(BatchRunStatus.RUNNING) } returns listOf(run)
    }
}

private val COLLECT_NOW: Instant = Instant.parse("2026-06-12T00:00:00Z")

private fun running(startedAt: Instant = COLLECT_NOW) =
    BatchRun(
        batchRunId = 10L,
        status = BatchRunStatus.RUNNING,
        evaluatedCount = 0,
        passedCount = 0,
        holdCount = 0,
        rejectedCount = 0,
        failedCount = 0,
        startedAt = startedAt,
        completedAt = null,
        providerBatchId = "batch_xyz",
        createdAt = startedAt,
        updatedAt = startedAt,
    )

private fun passResult(resumeId: Long) =
    EvaluationJobResult(
        resumeId = resumeId,
        verdict = EvaluationVerdict.PASS,
        score = 85,
        breakdown = EvaluationBreakdown(education = 80, experience = 90, skills = 85, fit = 80),
        reasoning = "요구사항 충족",
    )
