package org.hrfilter.resume.storage

import aws.sdk.kotlin.runtime.auth.credentials.StaticCredentialsProvider
import aws.sdk.kotlin.services.s3.S3Client
import aws.smithy.kotlin.runtime.net.url.Url
import kotlinx.coroutines.runBlocking
import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean

@AutoConfiguration
@EnableConfigurationProperties(S3StorageProperties::class)
class ResumeStorageAutoConfiguration {
    @Bean(destroyMethod = "close")
    fun s3Client(properties: S3StorageProperties): S3Client =
        runBlocking {
            S3Client.fromEnvironment {
                region = properties.region
                credentialsProvider =
                    StaticCredentialsProvider {
                        accessKeyId = properties.accessKey
                        secretAccessKey = properties.secretKey
                    }
                if (properties.endpoint.isNotBlank()) {
                    endpointUrl = Url.parse(properties.endpoint)
                    // MinIO 등 S3 호환 스토리지는 path-style 접근 필요
                    forcePathStyle = true
                }
            }
        }

    @Bean
    fun s3ResumeStorage(
        s3: S3Client,
        properties: S3StorageProperties,
    ): ResumeStorage = S3ResumeStorage(s3, properties.bucket)
}
