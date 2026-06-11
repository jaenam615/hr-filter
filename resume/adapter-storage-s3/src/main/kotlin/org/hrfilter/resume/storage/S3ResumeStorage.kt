package org.hrfilter.resume.storage

import aws.sdk.kotlin.services.s3.S3Client
import aws.sdk.kotlin.services.s3.model.GetObjectRequest
import aws.sdk.kotlin.services.s3.model.NoSuchKey
import aws.sdk.kotlin.services.s3.model.PutObjectRequest
import aws.smithy.kotlin.runtime.content.ByteStream
import aws.smithy.kotlin.runtime.content.toByteArray
import kotlinx.coroutines.runBlocking

internal class S3ResumeStorage(
    private val s3: S3Client,
    private val bucket: String,
) : ResumeStorage {
    // ResumeStorage 포트는 블로킹 시그니처 → aws-sdk-kotlin 코루틴 호출을 runBlocking으로 브리지.
    override fun upload(request: ResumeUploadRequest): String =
        runBlocking {
            val objectKey = buildObjectKey(request)
            s3.putObject(
                PutObjectRequest {
                    bucket = this@S3ResumeStorage.bucket
                    key = objectKey
                    body = ByteStream.fromBytes(request.content)
                    contentType = request.mimeType
                },
            )
            objectKey
        }

    override fun download(objectKey: String): ByteArray? =
        runBlocking {
            try {
                s3.getObject(
                    GetObjectRequest {
                        bucket = this@S3ResumeStorage.bucket
                        key = objectKey
                    },
                ) { response -> response.body?.toByteArray() }
            } catch (e: NoSuchKey) {
                null
            }
        }

    // resumes/{jobPostingId}/{업로드시각millis}-{지원자식별자}.{확장자}
    private fun buildObjectKey(request: ResumeUploadRequest): String {
        val safeIdentifier = request.applicantIdentifier.replace(Regex("[^A-Za-z0-9._-]"), "_")
        return "resumes/${request.jobPostingId}/${request.uploadedAt.toEpochMilli()}-$safeIdentifier.${request.fileExtension}"
    }
}
