package org.hrfilter.resume.parser

import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.context.annotation.Bean

@AutoConfiguration
class ResumeParserAutoConfiguration {
    @Bean
    fun tikaResumeParser(): ResumeParser = TikaResumeParser()
}
