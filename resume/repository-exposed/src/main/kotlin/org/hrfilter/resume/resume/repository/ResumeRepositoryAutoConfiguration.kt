package org.hrfilter.resume.resume.repository

import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.context.annotation.Bean

@AutoConfiguration
class ResumeRepositoryAutoConfiguration {
    @Bean
    fun resumeRepository(): ResumeRepository = ResumeExposedRepository()
}
