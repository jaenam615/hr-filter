package org.hrfilter.resume.resume

enum class ResumeStatus {
    UPLOADED,

    // 배치에 제출돼 평가 대기 중 (재제출 방지용)
    SUBMITTED,
    PARSED,
    EVALUATED,
    FAILED,
}
