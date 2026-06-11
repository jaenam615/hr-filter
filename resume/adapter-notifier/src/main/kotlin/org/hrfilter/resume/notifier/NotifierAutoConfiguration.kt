package org.hrfilter.resume.notifier

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import okhttp3.OkHttpClient
import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean

@AutoConfiguration
@EnableConfigurationProperties(NotifierProperties::class)
class NotifierAutoConfiguration {
    // OkHttpClient/ObjectMapper는 컨텍스트 빈으로 노출하지 않는다 — Spring MVC가 단일 ObjectMapper를
    // 기대하므로 충돌. 어댑터 내부에서만 쓰는 인스턴스라 private 생성.
    private val httpClient = OkHttpClient()
    private val objectMapper: ObjectMapper = jacksonObjectMapper()

    @Bean
    @ConditionalOnProperty(name = ["hrfilter.notifier.channel"], havingValue = "slack")
    fun slackNotifier(properties: NotifierProperties): Notifier = SlackNotifier(properties.slack.webhookUrl, httpClient, objectMapper)

    @Bean
    @ConditionalOnProperty(name = ["hrfilter.notifier.channel"], havingValue = "teams")
    fun teamsNotifier(properties: NotifierProperties): Notifier = TeamsNotifier(properties.teams.webhookUrl, httpClient, objectMapper)

    @Bean
    @ConditionalOnProperty(name = ["hrfilter.notifier.channel"], havingValue = "email")
    fun emailNotifier(properties: NotifierProperties): Notifier = EmailNotifier(properties.email)
}
