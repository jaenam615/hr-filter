package org.hrfilter.resume.notifier

internal class SlackNotifier : Notifier {
    override fun notify(summary: BatchSummary) {
        TODO("Slack incoming webhook POST (okhttp). 평가 건수/통과·보류·탈락 요약 + dashboardUrl 링크 포함")
    }
}
