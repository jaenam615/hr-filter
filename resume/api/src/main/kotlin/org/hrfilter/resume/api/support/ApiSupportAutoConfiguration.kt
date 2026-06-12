package org.hrfilter.resume.api.support

import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.context.annotation.Import

@AutoConfiguration
@Import(
    GlobalExceptionHandler::class,
)
class ApiSupportAutoConfiguration
