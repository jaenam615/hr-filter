package org.hrfilter.resume.llm

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "hrfilter.llm.anthropic")
data class AnthropicLlmProperties(
    val apiKey: String = "",
    val baseUrl: String = "https://api.anthropic.com",
    val model: String = "claude-opus-4-8",
    val maxTokens: Int = 4096,
    val anthropicVersion: String = "2023-06-01",
)
