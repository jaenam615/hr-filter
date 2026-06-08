package org.hrfilter.resume.llm

internal class AnthropicLlmEvaluator : LlmEvaluator {
    override fun submitBatch(jobs: List<EvaluationJob>): BatchHandle {
        TODO("Anthropic Message Batches API에 jobs 제출 → providerBatchId 반환 (okhttp POST)")
    }

    override fun pollResults(handle: BatchHandle): BatchResults {
        TODO("providerBatchId로 결과 폴링 → BatchProcessingStatus + EvaluationJobResult 리스트 반환")
    }
}
