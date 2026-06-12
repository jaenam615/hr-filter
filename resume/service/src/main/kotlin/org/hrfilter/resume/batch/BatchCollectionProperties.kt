package org.hrfilter.resume.batch

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "hrfilter.batch.collect")
data class BatchCollectionProperties(
    // 이 시간이 지나도 IN_PROGRESS면 stuck으로 보고 FAILED 처리(이력서는 UPLOADED로 되돌려 재시도).
    // Anthropic 배치 SLA(최대 24h)보다 약간 여유.
    val maxAgeHours: Long = 25,
)
