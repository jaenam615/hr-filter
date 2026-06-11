package org.hrfilter.resume.llm

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import okhttp3.OkHttpClient
import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import java.time.Duration

@AutoConfiguration
@EnableConfigurationProperties(AnthropicLlmProperties::class)
class LlmEvaluatorAutoConfiguration {
    // OkHttpClient/ObjectMapper는 컨텍스트 빈으로 노출하지 않는다 (Spring MVC의 단일 ObjectMapper와 충돌).
    private val httpClient: OkHttpClient =
        OkHttpClient.Builder()
            .callTimeout(Duration.ofSeconds(60))
            .build()
    private val objectMapper: ObjectMapper = jacksonObjectMapper()

    @Bean
    fun anthropicLlmEvaluator(properties: AnthropicLlmProperties): LlmEvaluator =
        AnthropicLlmEvaluator(properties, httpClient, objectMapper)
}
