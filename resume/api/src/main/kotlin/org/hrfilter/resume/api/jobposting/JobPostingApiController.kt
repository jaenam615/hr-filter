package org.hrfilter.resume.api.jobposting

import io.swagger.v3.oas.annotations.Operation
import org.hrfilter.resume.api.jobposting.dto.JobPostingCreateRequest
import org.hrfilter.resume.api.jobposting.dto.JobPostingResponse
import org.hrfilter.resume.jobposting.JobPostingReaderService
import org.hrfilter.resume.jobposting.JobPostingRegistrationCommand
import org.hrfilter.resume.jobposting.JobPostingRegistrationService
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/job-postings")
class JobPostingApiController(
    private val jobPostingRegistrationService: JobPostingRegistrationService,
    private val jobPostingReaderService: JobPostingReaderService,
) {
    @PostMapping
    @Operation(summary = "채용 공고 등록", operationId = "createJobPosting")
    fun create(
        @RequestBody request: JobPostingCreateRequest,
    ): JobPostingResponse =
        JobPostingResponse.from(
            jobPosting =
                jobPostingRegistrationService.register(
                    command =
                        JobPostingRegistrationCommand(
                            title = request.title,
                            description = request.description,
                            requirements = request.requirements,
                        ),
                ),
        )

    @GetMapping
    @Operation(summary = "채용 공고 목록", operationId = "listJobPostings")
    fun list(): List<JobPostingResponse> = jobPostingReaderService.getAll().map { JobPostingResponse.from(jobPosting = it) }
}
