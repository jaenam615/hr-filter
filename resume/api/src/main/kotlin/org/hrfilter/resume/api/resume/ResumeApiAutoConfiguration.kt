package org.hrfilter.resume.api.resume

import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.context.annotation.Import

@AutoConfiguration
@Import(
    ResumeApiController::class,
)
class ResumeApiAutoConfiguration
