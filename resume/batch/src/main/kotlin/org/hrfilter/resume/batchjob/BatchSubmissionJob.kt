package org.hrfilter.resume.batchjob

import org.hrfilter.resume.batch.BatchSubmissionService
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled

class BatchSubmissionJob(
    private val batchSubmissionService: BatchSubmissionService,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @Scheduled(cron = "\${hrfilter.batch.cron}")
    fun run() {
        log.info("BatchSubmissionJob started")
        runCatching { batchSubmissionService.submit() }
            .onSuccess { log.info("BatchSubmissionJob completed") }
            .onFailure { log.error("BatchSubmissionJob failed", it) }
    }
}
