package org.hrfilter.resume.jobposting

import org.hrfilter.resume.infrastructure.jobposting.repository.JobPostingRepository
import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.context.annotation.Bean

@AutoConfiguration
class JobPostingAutoConfiguration {
    @Bean
    fun jobPostingLookUpServiceImpl(jobPostingRepository: JobPostingRepository): JobPostingService =
        JobPostingServiceImpl(
            jobPostingRepository = jobPostingRepository,
        )
}
