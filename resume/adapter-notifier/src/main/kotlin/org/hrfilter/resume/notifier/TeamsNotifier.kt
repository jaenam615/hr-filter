package org.hrfilter.resume.notifier

import com.fasterxml.jackson.databind.ObjectMapper
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

internal class TeamsNotifier(
    private val webhookUrl: String,
    private val httpClient: OkHttpClient,
    private val objectMapper: ObjectMapper,
) : Notifier {
    override fun notify(summary: BatchSummary) {
        // Teams incoming webhook은 MessageCard(legacy) 형식을 받음. text 한 줄이면 충분.
        val payload =
            objectMapper.writeValueAsString(
                mapOf(
                    "@type" to "MessageCard",
                    "@context" to "https://schema.org/extensions",
                    "text" to summary.toMessageText(),
                ),
            )
        val request =
            Request.Builder()
                .url(webhookUrl)
                .post(payload.toRequestBody(JSON))
                .build()
        httpClient.newCall(request).execute().use { response ->
            check(response.isSuccessful) {
                "Teams webhook 실패: ${response.code} ${response.body?.string()}"
            }
        }
    }

    private companion object {
        val JSON = "application/json; charset=utf-8".toMediaType()
    }
}
