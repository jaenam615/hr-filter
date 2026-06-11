package org.hrfilter.resume.api.jobposting

import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.context.annotation.Import

@AutoConfiguration
@Import(
    JobPostingApiController::class,
)
class JobPostingApiAutoConfiguration
