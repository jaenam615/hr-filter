package org.hrfilter.resume.batchrun

import org.hrfilter.resume.batchrun.repository.BatchRunRepository
import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.context.annotation.Bean

@AutoConfiguration
class BatchRunReaderAutoConfiguration {
    @Bean
    fun batchRunReaderService(batchRunRepository: BatchRunRepository): BatchRunReaderService =
        BatchRunReaderServiceImpl(batchRunRepository = batchRunRepository)
}
