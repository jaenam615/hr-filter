package org.hrfilter.resume.notifier

import org.hrfilter.resume.batchrun.BatchRunStatus

// Slack/Teams/Email 공통 메시지 본문. 평가 건수 + 통과·보류·탈락 요약 + 대시보드 링크.
internal fun BatchSummary.toMessageText(): String {
    val statusLabel =
        when (status) {
            BatchRunStatus.COMPLETED -> "✅ 배치 평가 완료"
            BatchRunStatus.FAILED -> "❌ 배치 평가 실패"
            BatchRunStatus.RUNNING -> "⏳ 배치 평가 진행 중"
        }
    return buildString {
        appendLine("$statusLabel (batchRunId=$batchRunId)")
        appendLine("평가: ${evaluatedCount}건")
        appendLine("통과 $passedCount · 보류 $holdCount · 탈락 $rejectedCount · 실패 $failedCount")
        if (dashboardUrl != null) {
            appendLine("대시보드: $dashboardUrl")
        }
    }.trimEnd()
}
