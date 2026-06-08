package org.hrfilter.resume.notifier

internal class EmailNotifier : Notifier {
    override fun notify(summary: BatchSummary) {
        TODO("SMTP로 이메일 발송 (jakarta.mail + angus.mail). 수신자 리스트는 설정에서 주입")
    }
}
