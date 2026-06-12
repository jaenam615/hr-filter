package org.hrfilter.resume.batch

import org.hrfilter.resume.batchrun.BatchRun
import org.hrfilter.resume.batchrun.BatchRunStatus
import org.hrfilter.resume.batchrun.repository.BatchRunRepository
import org.hrfilter.resume.evaluation.EvaluationResult
import org.hrfilter.resume.evaluation.repository.EvaluationResultRepository
import org.hrfilter.resume.llm.BatchHandle
import org.hrfilter.resume.llm.BatchProcessingStatus
import org.hrfilter.resume.llm.BatchResults
import org.hrfilter.resume.llm.LlmEvaluator
import org.hrfilter.resume.notifier.BatchSummary
import org.hrfilter.resume.notifier.Notifier
import org.hrfilter.resume.resume.ResumeIdentity
import org.hrfilter.resume.resume.ResumeStatus
import org.hrfilter.resume.resume.of
import org.hrfilter.resume.resume.repository.ResumeRepository
import java.time.Duration
import java.time.Instant

/**
 * 배치 수거(논블로킹 재조정). 주기적으로 RUNNING 배치를 1회씩 폴링해
 * 완료면 결과 저장 + 알림, 실패/지연이면 FAILED 처리 후 이력서를 재시도 가능 상태로 되돌린다.
 *
 * 단일 in-flight 배치 불변식([BatchSubmissionService] 참고)에 기대어, 실패 시 SUBMITTED 이력서 전체를
 * 안전하게 UPLOADED로 되돌린다(동시 진행 배치가 없으므로 모두 같은 배치 소속).
 */
interface BatchCollectionService {
    fun collect()
}

internal class BatchCollectionServiceImpl(
    private val batchRunRepository: BatchRunRepository,
    private val resumeRepository: ResumeRepository,
    private val evaluationResultRepository: EvaluationResultRepository,
    private val llmEvaluator: LlmEvaluator,
    private val notifier: Notifier,
    private val maxAgeHours: Long,
) : BatchCollectionService {
    override fun collect() {
        batchRunRepository.findByStatus(status = BatchRunStatus.RUNNING).forEach { run ->
            val providerBatchId = run.providerBatchId ?: return@forEach
            val results = llmEvaluator.pollResults(handle = BatchHandle(providerBatchId = providerBatchId))
            when (results.status) {
                BatchProcessingStatus.IN_PROGRESS -> if (isStale(run)) failBatch(run)
                BatchProcessingStatus.COMPLETED -> completeBatch(run = run, results = results)
                BatchProcessingStatus.FAILED -> failBatch(run)
            }
        }
    }

    private fun isStale(run: BatchRun): Boolean = Duration.between(run.startedAt, Instant.now()).toHours() >= maxAgeHours

    private fun completeBatch(
        run: BatchRun,
        results: BatchResults,
    ) {
        saveBatchResults(results = results, batchRun = run)
        // 결과에 안 담긴(공급자단에서 errored) 이력서는 다음 회차 재시도하도록 되돌린다.
        revertSubmittedToUploaded()
        finishBatch(batchRun = run, status = BatchRunStatus.COMPLETED)
    }

    private fun failBatch(run: BatchRun) {
        revertSubmittedToUploaded()
        finishBatch(batchRun = run, status = BatchRunStatus.FAILED)
    }

    private fun revertSubmittedToUploaded() {
        resumeRepository.findAllByStatus(status = ResumeStatus.SUBMITTED).forEach {
            resumeRepository.updateStatus(resumeIdentity = ResumeIdentity.of(resumeId = it.resumeId), status = ResumeStatus.UPLOADED)
        }
    }

    private fun saveBatchResults(
        results: BatchResults,
        batchRun: BatchRun,
    ) {
        val now = Instant.now()
        val evaluationResults =
            results.items.map { item ->
                batchRunRepository.incrementCounts(identity = batchRun, verdict = item.verdict)
                resumeRepository.updateStatus(
                    resumeIdentity = ResumeIdentity.of(resumeId = item.resumeId),
                    status = ResumeStatus.EVALUATED,
                )
                EvaluationResult(
                    evaluationResultId = 0L,
                    resumeId = item.resumeId,
                    batchRunId = batchRun.batchRunId,
                    verdict = item.verdict,
                    score = item.score,
                    breakdown = item.breakdown,
                    reasoning = item.reasoning,
                    evaluatedAt = now,
                    createdAt = now,
                    updatedAt = now,
                )
            }
        evaluationResultRepository.saveAll(evaluationResults = evaluationResults)
    }

    private fun finishBatch(
        batchRun: BatchRun,
        status: BatchRunStatus,
    ) {
        batchRunRepository.complete(identity = batchRun, status = status, completedAt = Instant.now())
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
}
