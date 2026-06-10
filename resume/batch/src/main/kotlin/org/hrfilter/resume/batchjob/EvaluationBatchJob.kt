package org.hrfilter.resume.batchjob

import org.hrfilter.resume.batch.BatchEvaluationService
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled

class EvaluationBatchJob(
    private val batchEvaluationService: BatchEvaluationService,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @Scheduled(cron = "\${hrfilter.batch.cron}")
    fun run() {
        log.info("EvaluationBatchJob started")
        runCatching { batchEvaluationService.evaluate() }
            .onSuccess { log.info("EvaluationBatchJob completed") }
            .onFailure { log.error("EvaluationBatchJob failed", it) }
    }
}
