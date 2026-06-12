package org.hrfilter.resume.batchjob

import org.hrfilter.resume.batch.BatchCollectionService
import org.hrfilter.resume.batch.BatchSubmissionService
import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.scheduling.annotation.EnableScheduling

@AutoConfiguration
@EnableScheduling
class BatchJobAutoConfiguration {
    @Bean
    fun batchSubmissionJob(batchSubmissionService: BatchSubmissionService): BatchSubmissionJob =
        BatchSubmissionJob(batchSubmissionService = batchSubmissionService)

    @Bean
    fun batchCollectionJob(batchCollectionService: BatchCollectionService): BatchCollectionJob =
        BatchCollectionJob(batchCollectionService = batchCollectionService)
}
