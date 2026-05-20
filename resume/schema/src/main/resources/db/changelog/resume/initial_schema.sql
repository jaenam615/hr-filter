-- =============================================================================
-- job_posting : 채용 공고 (루트 엔티티)
-- =============================================================================
CREATE TABLE job_posting (
    job_posting_id BIGSERIAL PRIMARY KEY,
    title          VARCHAR(200) NOT NULL,
    description    TEXT         NOT NULL,
    requirements   TEXT         NOT NULL,
    created_at     TIMESTAMPTZ  NOT NULL,
    updated_at     TIMESTAMPTZ  NOT NULL
);


-- =============================================================================
-- resume : 이력서 (job_posting 참조)
-- =============================================================================
CREATE TABLE resume (
    resume_id        BIGSERIAL PRIMARY KEY,
    job_posting_id   BIGINT       NOT NULL REFERENCES job_posting (job_posting_id),
    applicant_name   VARCHAR(100) NOT NULL,
    applicant_email  VARCHAR(255) NOT NULL,
    object_key       VARCHAR(500) NOT NULL,
    mime_type        VARCHAR(100) NOT NULL,
    status           VARCHAR(20)  NOT NULL,
    created_at       TIMESTAMPTZ  NOT NULL,
    updated_at       TIMESTAMPTZ  NOT NULL
);

-- 배치 잡 핵심 쿼리: findAllByStatusAndCreatedBefore
CREATE INDEX idx_resume_status_created_at ON resume (status, created_at);

-- 공고별 조회 (미래 대비)
CREATE INDEX idx_resume_job_posting_id ON resume (job_posting_id);


-- =============================================================================
-- batch_run : 배치 실행 기록
-- =============================================================================
CREATE TABLE batch_run (
    batch_run_id     BIGSERIAL PRIMARY KEY,
    status           VARCHAR(20) NOT NULL,
    evaluated_count  INT         NOT NULL DEFAULT 0,
    passed_count     INT         NOT NULL DEFAULT 0,
    hold_count       INT         NOT NULL DEFAULT 0,
    rejected_count   INT         NOT NULL DEFAULT 0,
    failed_count     INT         NOT NULL DEFAULT 0,
    started_at       TIMESTAMPTZ NOT NULL,
    completed_at     TIMESTAMPTZ NULL,
    created_at       TIMESTAMPTZ NOT NULL,
    updated_at       TIMESTAMPTZ NOT NULL
);

-- 대시보드 히스토리: findLatestN
CREATE INDEX idx_batch_run_started_at_desc ON batch_run (started_at DESC);


-- =============================================================================
-- evaluation_result : 평가 결과 (resume + batch_run 참조)
-- =============================================================================
CREATE TABLE evaluation_result (
    evaluation_result_id BIGSERIAL PRIMARY KEY,
    resume_id            BIGINT      NOT NULL REFERENCES resume (resume_id),
    batch_run_id         BIGINT      NOT NULL REFERENCES batch_run (batch_run_id),
    verdict              VARCHAR(20) NOT NULL,
    score                INT         NOT NULL,
    breakdown            JSONB       NOT NULL,
    reasoning            TEXT        NOT NULL,
    evaluated_at         TIMESTAMPTZ NOT NULL,
    created_at           TIMESTAMPTZ NOT NULL,
    updated_at           TIMESTAMPTZ NOT NULL
);

-- 이력서별 평가 히스토리
CREATE INDEX idx_evaluation_result_resume_id ON evaluation_result (resume_id);

-- 배치별 결과 (대시보드)
CREATE INDEX idx_evaluation_result_batch_run_id ON evaluation_result (batch_run_id);
