package org.hrfilter.resume.evaluation

import org.hrfilter.resume.AuditProps
import org.hrfilter.resume.batchrun.BatchRunIdentity
import org.hrfilter.resume.resume.ResumeIdentity
import java.time.Instant

interface EvaluationResultProps {
    val verdict: EvaluationVerdict
    val score: Int
    val breakdown: EvaluationBreakdown
    val reasoning: String
    val evaluatedAt: Instant
}

interface EvaluationResultModel :
    EvaluationResultIdentity,
    ResumeIdentity,
    BatchRunIdentity,
    EvaluationResultProps,
    AuditProps
