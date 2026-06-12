package org.hrfilter.resume.batchrun

import org.hrfilter.resume.AuditProps
import java.time.Instant

interface BatchRunProps {
    val status: BatchRunStatus
    val evaluatedCount: Int
    val passedCount: Int
    val holdCount: Int
    val rejectedCount: Int
    val failedCount: Int
    val startedAt: Instant
    val completedAt: Instant?

    // LLM 공급자(Anthropic) 배치 ID. 제출 후 수거 잡이 폴링에 사용.
    val providerBatchId: String?
}

interface BatchRunModel :
    BatchRunIdentity,
    BatchRunProps,
    AuditProps
