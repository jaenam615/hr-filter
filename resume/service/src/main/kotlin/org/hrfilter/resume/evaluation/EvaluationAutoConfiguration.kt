package org.hrfilter.resume.evaluation

import org.hrfilter.resume.evaluation.repository.EvaluationResultRepository
import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.context.annotation.Bean

@AutoConfiguration
class EvaluationAutoConfiguration {
    @Bean
    fun evaluationReaderServiceImpl(evaluationResultRepository: EvaluationResultRepository): EvaluationReaderService =
        EvaluationReaderServiceImpl(evaluationResultRepository = evaluationResultRepository)
}
