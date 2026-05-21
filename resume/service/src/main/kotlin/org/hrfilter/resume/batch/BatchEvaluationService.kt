package org.hrfilter.resume.batch

import org.hrfilter.resume.infrastructure.batchrun.repository.BatchRunRepository
import org.hrfilter.resume.infrastructure.evaluation.repository.EvaluationResultRepository
import org.hrfilter.resume.infrastructure.jobposting.repository.JobPostingRepository
import org.hrfilter.resume.infrastructure.llm.LlmEvaluator
import org.hrfilter.resume.infrastructure.parser.ResumeParser
import org.hrfilter.resume.infrastructure.resume.repository.ResumeRepository
import org.hrfilter.resume.infrastructure.storage.ResumeStorage
import org.hrfilter.resume.resume.Resume
import org.hrfilter.resume.resume.ResumeStatus

interface BatchEvaluationService {
    fun evaluate()
}

internal class BatchEvaluationServiceImpl(
    private val batchRunRepository: BatchRunRepository,
    private val resumeRepository: ResumeRepository,
    private val jobPostingRepository: JobPostingRepository,
    private val evaluationResultRepository: EvaluationResultRepository,
    private val storage: ResumeStorage,
    private val resumeParser: ResumeParser,
    private val llmEvaluator: LlmEvaluator,
) : BatchEvaluationService {
    override fun evaluate() {
        // 1. 미평가(UPLOADED) 이력서 모음 — status가 진실의 원천. 시간 윈도우 X.
        val resumes: List<Resume> = resumeRepository.findAllByStatus(
            status = ResumeStatus.UPLOADED,
        )

        // TODO: 2. BatchRun 시작 (status=RUNNING)
        // TODO: 3. 각 resume → storage.download → parser.parse → EvaluationJob 생성
        // TODO: 4. JobPosting 정보 합쳐서 LLM에 submitBatch
        // TODO: 5. 결과 폴링
        // TODO: 6. EvaluationResult 저장 + BatchRun 카운트 ++ + Resume status=EVALUATED
        // TODO: 7. BatchRun complete + Notifier (Notifier 의존성 추가 필요)
    }
}
