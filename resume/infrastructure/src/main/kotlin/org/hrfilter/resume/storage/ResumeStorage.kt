package org.hrfilter.resume.storage

import java.time.Instant

interface ResumeStorage {
    fun upload(request: ResumeUploadRequest): String

    fun download(objectKey: String): ByteArray?
}

class ResumeUploadRequest(
    val jobPostingId: Long,
    val uploadedAt: Instant,
    val applicantIdentifier: String,
    val content: ByteArray,
    val mimeType: String,
    val fileExtension: String,
)
