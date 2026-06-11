package org.hrfilter.resume.api.dashboard

import org.hrfilter.resume.batchrun.BatchRunIdentity
import org.hrfilter.resume.batchrun.BatchRunReaderService
import org.hrfilter.resume.batchrun.of
import org.hrfilter.resume.evaluation.EvaluationReaderService
import org.hrfilter.resume.jobposting.JobPostingIdentity
import org.hrfilter.resume.jobposting.JobPostingReaderService
import org.hrfilter.resume.jobposting.JobPostingRegistrationCommand
import org.hrfilter.resume.jobposting.JobPostingRegistrationService
import org.hrfilter.resume.jobposting.of
import org.hrfilter.resume.resume.ResumeIdentity
import org.hrfilter.resume.resume.ResumeReaderService
import org.hrfilter.resume.resume.ResumeRegistrationCommand
import org.hrfilter.resume.resume.ResumeRegistrationService
import org.hrfilter.resume.resume.of
import org.springframework.http.MediaType
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RequestPart
import org.springframework.web.multipart.MultipartFile

@Controller
@RequestMapping("/dashboard")
class DashboardController(
    private val batchRunReaderService: BatchRunReaderService,
    private val evaluationReaderService: EvaluationReaderService,
    private val resumeReaderService: ResumeReaderService,
    private val resumeRegistrationService: ResumeRegistrationService,
    private val jobPostingReaderService: JobPostingReaderService,
    private val jobPostingRegistrationService: JobPostingRegistrationService,
) {
    @GetMapping
    fun dashboard(model: Model): String {
        val batchRuns = batchRunReaderService.getLatest(limit = 10)
        val latestRun = batchRuns.firstOrNull()
        val rows =
            latestRun
                ?.let { run ->
                    evaluationReaderService
                        .getAllByBatch(batchRunIdentity = BatchRunIdentity.of(batchRunId = run.batchRunId))
                        .map { ev ->
                            val applicantName =
                                runCatching {
                                    resumeReaderService.get(resumeIdentity = ResumeIdentity.of(resumeId = ev.resumeId))
                                }.getOrNull()?.applicantName ?: "resume #${ev.resumeId}"
                            EvaluationRow(
                                applicantName = applicantName,
                                verdict = ev.verdict.name,
                                score = ev.score,
                                reasoning = ev.reasoning,
                                evaluatedAt = ev.evaluatedAt.toString(),
                            )
                        }
                }.orEmpty()

        model.addAttribute("jobPostings", jobPostingReaderService.getAll())
        model.addAttribute("batchRuns", batchRuns)
        model.addAttribute("latestRun", latestRun)
        model.addAttribute("rows", rows)
        return "dashboard"
    }

    @PostMapping("/job-postings")
    fun createJobPosting(
        @RequestParam("title") title: String,
        @RequestParam("description") description: String,
        @RequestParam("requirements") requirements: String,
    ): String {
        jobPostingRegistrationService.register(
            command =
                JobPostingRegistrationCommand(
                    title = title,
                    description = description,
                    requirements = requirements,
                ),
        )
        return "redirect:/dashboard"
    }

    @PostMapping("/resumes")
    fun upload(
        @RequestParam("jobPostingId") jobPostingId: Long,
        @RequestParam("applicantName") applicantName: String,
        @RequestParam("applicantEmail") applicantEmail: String,
        @RequestPart("file") file: MultipartFile,
    ): String {
        resumeRegistrationService.register(
            command =
                ResumeRegistrationCommand(
                    jobPostingIdentity = JobPostingIdentity.of(jobPostingId = jobPostingId),
                    applicantName = applicantName,
                    applicantEmail = applicantEmail,
                    content = file.bytes,
                    mimeType = file.contentType ?: MediaType.APPLICATION_OCTET_STREAM_VALUE,
                    fileExtension = file.originalFilename?.substringAfterLast('.', "") ?: "",
                ),
        )
        return "redirect:/dashboard"
    }
}

data class EvaluationRow(
    val applicantName: String,
    val verdict: String,
    val score: Int,
    val reasoning: String,
    val evaluatedAt: String,
)
