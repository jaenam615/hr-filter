package org.hrfilter.resume.notifier

import jakarta.mail.Message
import jakarta.mail.Session
import jakarta.mail.Transport
import jakarta.mail.internet.InternetAddress
import jakarta.mail.internet.MimeMessage
import java.util.Properties

internal class EmailNotifier(
    private val config: NotifierProperties.Email,
) : Notifier {
    override fun notify(summary: BatchSummary) {
        val session =
            Session.getInstance(smtpProperties(), PasswordAuthenticator(config.username, config.password))
        val message =
            MimeMessage(session).apply {
                setFrom(InternetAddress(config.from))
                setRecipients(
                    Message.RecipientType.TO,
                    config.to.map { InternetAddress(it) }.toTypedArray(),
                )
                subject = "[hr-filter] 배치 평가 결과 (batchRunId=${summary.batchRunId})"
                setText(summary.toMessageText(), "UTF-8")
            }
        Transport.send(message)
    }

    private fun smtpProperties(): Properties =
        Properties().apply {
            put("mail.smtp.host", config.host)
            put("mail.smtp.port", config.port.toString())
            put("mail.smtp.auth", "true")
            put("mail.smtp.starttls.enable", "true")
        }

    private class PasswordAuthenticator(
        private val username: String,
        private val password: String,
    ) : jakarta.mail.Authenticator() {
        override fun getPasswordAuthentication() = jakarta.mail.PasswordAuthentication(username, password)
    }
}
