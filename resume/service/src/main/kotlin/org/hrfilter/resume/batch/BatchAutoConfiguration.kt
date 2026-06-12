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
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean

@AutoConfiguration
@EnableConfigurationProperties(BatchCollectionProperties::class)
class BatchAutoConfiguration {
    @Bean
    fun batchSubmissionService(
        batchRunRepository: BatchRunRepository,
        resumeRepository: ResumeRepository,
        jobPostingRepository: JobPostingRepository,
        storage: ResumeStorage,
        resumeParser: ResumeParser,
        llmEvaluator: LlmEvaluator,
    ): BatchSubmissionService =
        BatchSubmissionServiceImpl(
            batchRunRepository = batchRunRepository,
            resumeRepository = resumeRepository,
            jobPostingRepository = jobPostingRepository,
            storage = storage,
            resumeParser = resumeParser,
            llmEvaluator = llmEvaluator,
        )

    @Bean
    fun batchCollectionService(
        batchRunRepository: BatchRunRepository,
        resumeRepository: ResumeRepository,
        evaluationResultRepository: EvaluationResultRepository,
        llmEvaluator: LlmEvaluator,
        notifier: Notifier,
        collectionProperties: BatchCollectionProperties,
    ): BatchCollectionService =
        BatchCollectionServiceImpl(
            batchRunRepository = batchRunRepository,
            resumeRepository = resumeRepository,
            evaluationResultRepository = evaluationResultRepository,
            llmEvaluator = llmEvaluator,
            notifier = notifier,
            maxAgeHours = collectionProperties.maxAgeHours,
        )
}
