package org.hrfilter.resume.api.batchrun

import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.context.annotation.Import

@AutoConfiguration
@Import(
    BatchRunApiController::class,
)
class BatchRunApiAutoConfiguration
