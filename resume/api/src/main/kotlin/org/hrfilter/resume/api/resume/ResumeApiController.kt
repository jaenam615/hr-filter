package org.hrfilter.resume.api.resume

import io.swagger.v3.oas.annotations.Operation
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Positive
import org.hrfilter.resume.api.resume.dto.ResumeResponse
import org.hrfilter.resume.jobposting.JobPostingIdentity
import org.hrfilter.resume.jobposting.of
import org.hrfilter.resume.resume.ResumeIdentity
import org.hrfilter.resume.resume.ResumeReaderService
import org.hrfilter.resume.resume.ResumeRegistrationCommand
import org.hrfilter.resume.resume.ResumeRegistrationService
import org.hrfilter.resume.resume.of
import org.springframework.http.MediaType
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RequestPart
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.multipart.MultipartFile

@RestController
@RequestMapping("/api/v1/resumes")
@Validated
class ResumeApiController(
    private val resumeRegistrationService: ResumeRegistrationService,
    private val resumeReaderService: ResumeReaderService,
) {
    @PostMapping(consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    @Operation(summary = "이력서 업로드", operationId = "uploadResume")
    fun uploadResume(
        @RequestParam("jobPostingId") @Positive jobPostingId: Long,
        @RequestParam("applicantName") @NotBlank applicantName: String,
        @RequestParam("applicantEmail") @Email applicantEmail: String,
        @RequestPart("file") file: MultipartFile,
    ): ResumeResponse {
        val resume =
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
        return ResumeResponse.from(resume = resume)
    }

    @GetMapping("/{resumeId}")
    @Operation(summary = "이력서 단건 조회", operationId = "getResume")
    fun getResume(
        @PathVariable resumeId: Long,
    ): ResumeResponse =
        ResumeResponse.from(
            resume = resumeReaderService.get(resumeIdentity = ResumeIdentity.of(resumeId = resumeId)),
        )
}
