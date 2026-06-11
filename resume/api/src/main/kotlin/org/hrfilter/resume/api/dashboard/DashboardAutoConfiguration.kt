package org.hrfilter.resume.api.dashboard

import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.context.annotation.Import

@AutoConfiguration
@Import(
    DashboardController::class,
)
class DashboardAutoConfiguration
