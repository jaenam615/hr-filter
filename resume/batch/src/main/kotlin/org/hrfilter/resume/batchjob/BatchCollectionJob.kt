package org.hrfilter.resume.batchjob

import org.hrfilter.resume.batch.BatchCollectionService
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled

class BatchCollectionJob(
    private val batchCollectionService: BatchCollectionService,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @Scheduled(cron = "\${hrfilter.batch.collect-cron}")
    fun run() {
        runCatching { batchCollectionService.collect() }
            .onFailure { log.error("BatchCollectionJob failed", it) }
    }
}
