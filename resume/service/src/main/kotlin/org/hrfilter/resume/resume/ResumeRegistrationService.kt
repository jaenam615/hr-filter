package org.hrfilter.resume.resume

import org.hrfilter.resume.infrastructure.resume.repository.ResumeRepository
import org.hrfilter.resume.infrastructure.storage.ResumeStorage
import org.hrfilter.resume.infrastructure.storage.ResumeUploadRequest
import org.hrfilter.resume.jobposting.JobPostingIdentity
import java.time.Instant
import java.util.UUID

interface ResumeRegistrationService {
    fun register(command: ResumeRegistrationCommand): Resume
}

class ResumeRegistrationCommand(
    val jobPostingIdentity: JobPostingIdentity,
    val applicantName: String,
    val applicantEmail: String,
    val content: ByteArray,
    val mimeType: String,
    val fileExtension: String,
)

internal class ResumeRegistrationServiceImpl(
    private val resumeRepository: ResumeRepository,
    private val resumeStorage: ResumeStorage,
) : ResumeRegistrationService {
    override fun register(command: ResumeRegistrationCommand): Resume {
        val now = Instant.now()
        val objectKey = resumeStorage.upload(
            request = ResumeUploadRequest(
                jobPostingId = command.jobPostingIdentity.jobPostingId,
                uploadedAt = now,
                applicantIdentifier = UUID.randomUUID().toString(),
                content = command.content,
                mimeType = command.mimeType,
                fileExtension = command.fileExtension,
            ),
        )
        return resumeRepository.save(
            resume = Resume(
                resumeId = 0L,
                jobPostingId = command.jobPostingIdentity.jobPostingId,
                applicantName = command.applicantName,
                applicantEmail = command.applicantEmail,
                objectKey = objectKey,
                mimeType = command.mimeType,
                status = ResumeStatus.UPLOADED,
                createdAt = now,
                updatedAt = now,
            ),
        )
    }
}
