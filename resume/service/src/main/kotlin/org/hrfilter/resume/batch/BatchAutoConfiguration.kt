package org.hrfilter.resume.batch

import org.hrfilter.resume.batchrun.repository.BatchRunRepository
import org.hrfilter.resume.evaluation.repository.EvaluationResultRepository
import org.hrfilter.resume.jobposting.repository.JobPostingRepository
import org.hrfilter.resume.llm.LlmEvaluator
import org.hrfilter.resume.notifier.Notifier
import org.hrfilter.resume.parser.ResumeParser
import org.hrfilter.resume.resume.repository.ResumeRepository
import org.hrfilter.resume.storage.ResumeStorage
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
