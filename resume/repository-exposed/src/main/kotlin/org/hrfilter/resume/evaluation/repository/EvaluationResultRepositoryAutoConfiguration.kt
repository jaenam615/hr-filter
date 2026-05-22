package org.hrfilter.resume.evaluation.repository

import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.context.annotation.Bean

@AutoConfiguration
class EvaluationResultRepositoryAutoConfiguration {
    @Bean
    fun evaluationResultRepository(): EvaluationResultRepository = EvaluationResultExposedRepository()
}
