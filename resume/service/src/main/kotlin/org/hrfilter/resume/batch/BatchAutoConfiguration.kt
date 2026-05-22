package org.hrfilter.resume.batch

import org.hrfilter.resume.infrastructure.batchrun.repository.BatchRunRepository
import org.hrfilter.resume.infrastructure.evaluation.repository.EvaluationResultRepository
import org.hrfilter.resume.infrastructure.jobposting.repository.JobPostingRepository
import org.hrfilter.resume.infrastructure.llm.LlmEvaluator
import org.hrfilter.resume.infrastructure.notifier.Notifier
import org.hrfilter.resume.infrastructure.parser.ResumeParser
import org.hrfilter.resume.infrastructure.resume.repository.ResumeRepository
import org.hrfilter.resume.infrastructure.storage.ResumeStorage
import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.context.annotation.Bean

@AutoConfiguration
class BatchAutoConfiguration {
    @Bean
    fun batchEvaluationServiceImpl(
        batchRunRepository: BatchRunRepository,
        resumeRepository: ResumeRepository,
        jobPostingRepository: JobPostingRepository,
        evaluationResultRepository: EvaluationResultRepository,
        storage: ResumeStorage,
        resumeParser: ResumeParser,
        llmEvaluator: LlmEvaluator,
        notifier: Notifier,
    ): BatchEvaluationService =
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
}
