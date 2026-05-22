package org.hrfilter.resume.evaluation.repository

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import org.hrfilter.resume.batchrun.repository.BatchRunTable
import org.hrfilter.resume.evaluation.EvaluationBreakdown
import org.hrfilter.resume.evaluation.EvaluationVerdict
import org.hrfilter.resume.resume.repository.ResumeTable
import org.jetbrains.exposed.dao.id.LongIdTable
import org.jetbrains.exposed.sql.javatime.timestamp
import org.jetbrains.exposed.sql.json.jsonb

private val objectMapper = jacksonObjectMapper()

internal object EvaluationResultTable :
    LongIdTable(name = "evaluation_result", columnName = "evaluation_result_id") {
    val resumeId = reference("resume_id", ResumeTable)
    val batchRunId = reference("batch_run_id", BatchRunTable)
    val verdict = enumerationByName("verdict", 20, EvaluationVerdict::class)
    val score = integer("score")
    val breakdown = jsonb<EvaluationBreakdown>(
        name = "breakdown",
        serialize = { objectMapper.writeValueAsString(it) },
        deserialize = { objectMapper.readValue(it) },
    )
    val reasoning = text("reasoning")
    val evaluatedAt = timestamp("evaluated_at")
    val createdAt = timestamp("created_at")
    val updatedAt = timestamp("updated_at")
}
