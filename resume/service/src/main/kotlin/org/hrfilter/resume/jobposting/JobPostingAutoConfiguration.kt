package org.hrfilter.resume.jobposting

import org.hrfilter.resume.jobposting.repository.JobPostingRepository
import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.context.annotation.Bean

@AutoConfiguration
class JobPostingAutoConfiguration {
    @Bean
    fun jobPostingReaderServiceImpl(jobPostingRepository: JobPostingRepository): JobPostingReaderService =
        JobPostingReaderServiceImpl(
            jobPostingRepository = jobPostingRepository,
        )

    @Bean
    fun jobPostingRegistrationServiceImpl(jobPostingRepository: JobPostingRepository): JobPostingRegistrationService =
        JobPostingRegistrationServiceImpl(
            jobPostingRepository = jobPostingRepository,
        )
}
