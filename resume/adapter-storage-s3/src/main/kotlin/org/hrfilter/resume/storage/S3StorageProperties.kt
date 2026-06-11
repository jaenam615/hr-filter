package org.hrfilter.resume.storage

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "hrfilter.storage.s3")
data class S3StorageProperties(
    // MinIO 등 S3 호환 엔드포인트. 실제 AWS S3면 비워두면 됨.
    val endpoint: String = "",
    val accessKey: String = "",
    val secretKey: String = "",
    val region: String = "us-east-1",
    val bucket: String = "",
)
