package org.hrfilter.resume.storage

import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.context.annotation.Bean

@AutoConfiguration
class ResumeStorageAutoConfiguration {
    @Bean
    fun s3ResumeStorage(): ResumeStorage = S3ResumeStorage()
}
