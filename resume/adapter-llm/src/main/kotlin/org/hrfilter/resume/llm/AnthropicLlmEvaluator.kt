package org.hrfilter.resume.llm

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.hrfilter.resume.evaluation.EvaluationBreakdown
import org.hrfilter.resume.evaluation.EvaluationVerdict

/**
 * Anthropic Message Batches API 어댑터.
 *
 * - submitBatch: POST /v1/messages/batches  → providerBatchId 반환
 * - pollResults: GET  /v1/messages/batches/{id}        → processing_status 확인
 *                GET  /v1/messages/batches/{id}/results → JSONL 결과 파싱 (ended일 때)
 *
 * 각 요청은 output_config.format(json_schema)로 평가 결과 JSON 구조를 강제한다.
 */
internal class AnthropicLlmEvaluator(
    private val properties: AnthropicLlmProperties,
    private val httpClient: OkHttpClient,
    private val objectMapper: ObjectMapper,
) : LlmEvaluator {
    override fun submitBatch(jobs: List<EvaluationJob>): BatchHandle {
        val body = objectMapper.writeValueAsString(mapOf("requests" to jobs.map(::toBatchRequest)))
        val request =
            authedRequest("${properties.baseUrl}/v1/messages/batches")
                .post(body.toRequestBody(JSON))
                .build()
        val node = execute(request)
        val batchId = node.get("id")?.asText() ?: error("배치 응답에 id 없음: $node")
        return BatchHandle(providerBatchId = batchId)
    }

    override fun pollResults(handle: BatchHandle): BatchResults {
        val statusRequest =
            authedRequest("${properties.baseUrl}/v1/messages/batches/${handle.providerBatchId}")
                .get()
                .build()
        val statusNode = execute(statusRequest)
        return when (val status = statusNode.get("processing_status")?.asText()) {
            "in_progress" -> BatchResults(BatchProcessingStatus.IN_PROGRESS, emptyList())
            "ended" -> BatchResults(BatchProcessingStatus.COMPLETED, fetchResults(handle))
            "canceling", "canceled" -> BatchResults(BatchProcessingStatus.FAILED, emptyList())
            else -> error("알 수 없는 배치 상태: $status")
        }
    }

    private fun fetchResults(handle: BatchHandle): List<EvaluationJobResult> {
        val request =
            authedRequest("${properties.baseUrl}/v1/messages/batches/${handle.providerBatchId}/results")
                .get()
                .build()
        httpClient.newCall(request).execute().use { response ->
            check(response.isSuccessful) { "결과 조회 실패: ${response.code}" }
            val jsonl = response.body?.string().orEmpty()
            return jsonl.lineSequence()
                .filter { it.isNotBlank() }
                .mapNotNull { parseResultLine(it) }
                .toList()
        }
    }

    // JSONL 한 줄 = {"custom_id": "...", "result": {"type": "succeeded", "message": {...}}}
    private fun parseResultLine(line: String): EvaluationJobResult? {
        val node = objectMapper.readTree(line)
        val result = node.get("result")
        if (result?.get("type")?.asText() != "succeeded") return null

        val resumeId = node.get("custom_id").asText().substringAfter(CUSTOM_ID_PREFIX).toLong()
        val text =
            result.get("message").get("content")
                .first { it.get("type").asText() == "text" }
                .get("text").asText()
        val eval = objectMapper.readTree(text)
        return EvaluationJobResult(
            resumeId = resumeId,
            verdict = EvaluationVerdict.valueOf(eval.get("verdict").asText()),
            score = eval.get("score").asInt(),
            breakdown =
                eval.get("breakdown").let {
                    EvaluationBreakdown(
                        education = it.get("education").asInt(),
                        experience = it.get("experience").asInt(),
                        skills = it.get("skills").asInt(),
                        fit = it.get("fit").asInt(),
                    )
                },
            reasoning = eval.get("reasoning").asText(),
        )
    }

    private fun toBatchRequest(job: EvaluationJob): Map<String, Any> =
        mapOf(
            "custom_id" to "$CUSTOM_ID_PREFIX${job.resumeId}",
            "params" to
                mapOf(
                    "model" to properties.model,
                    "max_tokens" to properties.maxTokens,
                    "system" to SYSTEM_PROMPT,
                    "messages" to
                        listOf(
                            mapOf("role" to "user", "content" to userContent(job)),
                        ),
                    "output_config" to mapOf("format" to mapOf("type" to "json_schema", "schema" to OUTPUT_SCHEMA)),
                ),
        )

    private fun userContent(job: EvaluationJob): String =
        """
        [채용 공고]
        제목: ${job.jobPostingTitle}
        설명: ${job.jobPostingDescription}
        요구사항: ${job.jobPostingRequirements}

        [지원자 이력서]
        ${job.resumeText}
        """.trimIndent()

    private fun authedRequest(url: String): Request.Builder =
        Request.Builder()
            .url(url)
            .header("x-api-key", properties.apiKey)
            .header("anthropic-version", properties.anthropicVersion)
            .header("content-type", "application/json")

    private fun execute(request: Request): JsonNode =
        httpClient.newCall(request).execute().use { response ->
            val bodyText = response.body?.string().orEmpty()
            check(response.isSuccessful) { "Anthropic API 실패: ${response.code} $bodyText" }
            objectMapper.readTree(bodyText)
        }

    private companion object {
        val JSON = "application/json; charset=utf-8".toMediaType()
        const val CUSTOM_ID_PREFIX = "resume-"

        val SYSTEM_PROMPT =
            """
            당신은 채용 서류 심사관입니다. 주어진 채용 공고와 지원자 이력서를 비교해 평가하세요.
            - verdict: PASS(통과) / HOLD(보류) / REJECT(탈락) 중 하나
            - score: 0~100 종합 점수
            - breakdown: education/experience/skills/fit 각 0~100 세부 점수
            - reasoning: 평가 근거를 한국어로 간결히
            반드시 지정된 JSON 스키마에 맞춰 응답하세요.
            """.trimIndent()

        val OUTPUT_SCHEMA: Map<String, Any> =
            mapOf(
                "type" to "object",
                "properties" to
                    mapOf(
                        "verdict" to mapOf("type" to "string", "enum" to listOf("PASS", "HOLD", "REJECT")),
                        "score" to mapOf("type" to "integer"),
                        "breakdown" to
                            mapOf(
                                "type" to "object",
                                "properties" to
                                    mapOf(
                                        "education" to mapOf("type" to "integer"),
                                        "experience" to mapOf("type" to "integer"),
                                        "skills" to mapOf("type" to "integer"),
                                        "fit" to mapOf("type" to "integer"),
                                    ),
                                "required" to listOf("education", "experience", "skills", "fit"),
                                "additionalProperties" to false,
                            ),
                        "reasoning" to mapOf("type" to "string"),
                    ),
                "required" to listOf("verdict", "score", "breakdown", "reasoning"),
                "additionalProperties" to false,
            )
    }
}
