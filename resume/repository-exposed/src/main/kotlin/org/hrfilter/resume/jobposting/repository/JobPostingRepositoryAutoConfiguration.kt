package org.hrfilter.resume.jobposting.repository

import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.context.annotation.Bean

@AutoConfiguration
class JobPostingRepositoryAutoConfiguration {
    @Bean
    fun jobPostingRepository(): JobPostingRepository = JobPostingExposedRepository()
}
