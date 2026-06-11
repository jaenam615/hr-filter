package org.hrfilter.resume.notifier

import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import org.hrfilter.resume.batchrun.BatchRunStatus

class BatchSummaryMessageTest : DescribeSpec({
    fun summary(
        status: BatchRunStatus = BatchRunStatus.COMPLETED,
        dashboardUrl: String? = null,
    ) = BatchSummary(
        batchRunId = 7L,
        status = status,
        evaluatedCount = 5,
        passedCount = 2,
        holdCount = 1,
        rejectedCount = 2,
        failedCount = 0,
        dashboardUrl = dashboardUrl,
    )

    describe("toMessageText") {
        it("완료 요약은 배치 ID·건수·통과/보류/탈락을 포함한다") {
            val text = summary().toMessageText()

            text shouldContain "batchRunId=7"
            text shouldContain "평가: 5건"
            text shouldContain "통과 2"
            text shouldContain "보류 1"
            text shouldContain "탈락 2"
            text shouldContain "완료"
        }

        it("dashboardUrl이 있으면 링크를 포함한다") {
            summary(dashboardUrl = "https://dash.example.com/7").toMessageText() shouldContain
                "https://dash.example.com/7"
        }

        it("dashboardUrl이 없으면 링크 줄을 넣지 않는다") {
            summary(dashboardUrl = null).toMessageText() shouldNotContain "대시보드:"
        }

        it("실패 상태는 실패 라벨을 쓴다") {
            summary(status = BatchRunStatus.FAILED).toMessageText() shouldContain "실패"
        }
    }
})
