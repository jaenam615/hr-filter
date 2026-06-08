package org.hrfilter.resume.storage

internal class S3ResumeStorage : ResumeStorage {
    override fun upload(request: ResumeUploadRequest): String {
        TODO("S3에 content 업로드 → objectKey 반환 (aws-sdk-kotlin S3Client.putObject, coroutine 기반)")
    }

    override fun download(objectKey: String): ByteArray? {
        TODO("objectKey로 S3 객체 다운로드 → ByteArray 반환, 없으면 null")
    }
}
