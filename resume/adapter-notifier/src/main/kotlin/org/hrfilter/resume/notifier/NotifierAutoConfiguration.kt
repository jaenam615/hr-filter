package org.hrfilter.resume.notifier

import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean

@AutoConfiguration
class NotifierAutoConfiguration {
    @Bean
    @ConditionalOnProperty(name = ["hrfilter.notifier.channel"], havingValue = "slack")
    fun slackNotifier(): Notifier = SlackNotifier()

    @Bean
    @ConditionalOnProperty(name = ["hrfilter.notifier.channel"], havingValue = "teams")
    fun teamsNotifier(): Notifier = TeamsNotifier()

    @Bean
    @ConditionalOnProperty(name = ["hrfilter.notifier.channel"], havingValue = "email")
    fun emailNotifier(): Notifier = EmailNotifier()
}
