package org.hrfilter.resume.api.batchrun

import io.swagger.v3.oas.annotations.Operation
import org.hrfilter.resume.api.batchrun.dto.EvaluationResultResponse
import org.hrfilter.resume.batchrun.BatchRunIdentity
import org.hrfilter.resume.batchrun.of
import org.hrfilter.resume.evaluation.EvaluationReaderService
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/batch-runs")
class BatchRunApiController(
    private val evaluationReaderService: EvaluationReaderService,
) {
    @GetMapping("/{batchRunId}/evaluations")
    @Operation(summary = "배치별 평가 결과 조회", operationId = "getEvaluationsByBatchRun")
    fun getEvaluations(
        @PathVariable batchRunId: Long,
    ): List<EvaluationResultResponse> =
        evaluationReaderService
            .getAllByBatch(batchRunIdentity = BatchRunIdentity.of(batchRunId = batchRunId))
            .map { EvaluationResultResponse.from(evaluationResult = it) }
}
