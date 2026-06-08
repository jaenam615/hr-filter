package org.hrfilter.resume.llm

import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.context.annotation.Bean

@AutoConfiguration
class LlmEvaluatorAutoConfiguration {
    @Bean
    fun anthropicLlmEvaluator(): LlmEvaluator = AnthropicLlmEvaluator()
}
