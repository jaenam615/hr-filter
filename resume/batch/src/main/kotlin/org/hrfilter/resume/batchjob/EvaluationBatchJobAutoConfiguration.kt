package org.hrfilter.resume.batchjob

import org.hrfilter.resume.batch.BatchEvaluationService
import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.scheduling.annotation.EnableScheduling

@AutoConfiguration
@EnableScheduling
class EvaluationBatchJobAutoConfiguration {
    @Bean
    fun evaluationBatchJob(batchEvaluationService: BatchEvaluationService): EvaluationBatchJob =
        EvaluationBatchJob(batchEvaluationService = batchEvaluationService)
}
