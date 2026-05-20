package org.hrfilter.resume.resume

import org.hrfilter.resume.infrastructure.resume.repository.ResumeRepository
import org.hrfilter.resume.infrastructure.storage.ResumeStorage
import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.context.annotation.Bean

@AutoConfiguration
class ResumeAutoConfiguration {
    @Bean
    fun resumeReaderServiceImpl(resumeRepository: ResumeRepository): ResumeReaderService =
        ResumeReaderServiceImpl(
            resumeRepository = resumeRepository,
        )

    @Bean
    fun resumeRegistrationServiceImpl(
        resumeRepository: ResumeRepository,
        resumeStorage: ResumeStorage,
    ): ResumeRegistrationService =
        ResumeRegistrationServiceImpl(
            resumeRepository = resumeRepository,
            resumeStorage = resumeStorage,
        )
}
