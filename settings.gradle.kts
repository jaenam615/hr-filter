rootProject.name = "hr-filter"

// 도메인: 이력서 평가
include(":resume:model")
include(":resume:exception")
include(":resume:infrastructure")
include(":resume:schema")
include(":resume:service")

include(":resume:repository-exposed")
include(":resume:adapter-parsing")
include(":resume:adapter-llm")
include(":resume:adapter-storage-s3")
include(":resume:adapter-notifier")

include(":resume:api")
include(":resume:batch")

// 진입점: API 서버 / 배치 서버 (배포 단위 분리)
include(":application-api")
include(":application-batch")

pluginManagement {
    buildscript {
        repositories {
            gradlePluginPortal()
        }
    }
    repositories {
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositories {
        mavenCentral()
    }
}

include("resume:untitled")

include("resume:service")
