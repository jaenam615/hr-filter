package org.hrfilter.resume.notifier

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.NestedConfigurationProperty

@ConfigurationProperties(prefix = "hrfilter.notifier")
data class NotifierProperties(
    // slack | teams | email
    val channel: String = "slack",
    @NestedConfigurationProperty val slack: Slack = Slack(),
    @NestedConfigurationProperty val teams: Teams = Teams(),
    @NestedConfigurationProperty val email: Email = Email(),
) {
    data class Slack(
        val webhookUrl: String = "",
    )

    data class Teams(
        val webhookUrl: String = "",
    )

    data class Email(
        val host: String = "",
        val port: Int = 587,
        val username: String = "",
        val password: String = "",
        val from: String = "",
        // 콤마로 구분된 수신자 목록
        val to: List<String> = emptyList(),
    )
}
