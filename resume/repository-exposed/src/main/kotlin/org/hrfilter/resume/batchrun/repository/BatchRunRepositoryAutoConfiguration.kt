package org.hrfilter.resume.batchrun.repository

import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.context.annotation.Bean

@AutoConfiguration
class BatchRunRepositoryAutoConfiguration {
    @Bean
    fun batchRunRepository(): BatchRunRepository = BatchRunExposedRepository()
}
