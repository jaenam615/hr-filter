# hr-filter 개발 일지

오랜만에 돌아왔을 때 "어디까지 했지" 빨리 회상하는 용도. 자세한 핸드오프는 `TODO.md`, 의사결정/패턴 근거는 `README.md`와 study repo (`../../study/hexagonal-module-sample`) 참조.

---

## 2026-05-21 — 출발

- 빈 Gradle 프로젝트 → study repo의 `corehr` 모듈 구성 미러링 결정
- `settings.gradle.kts`에 14개 서브모듈 등록 (model/exception/infrastructure/schema/service/repository-exposed/adapter-* 4개/api/batch/application-api/application-batch)
- 루트 `build.gradle.kts`에 Build Recipe 컨벤션 6개 (kotlin / -boot / -mvc / -exposed-repository / -application 조합)
- `gradle/libs.versions.toml` — Spring Boot 3.4, Exposed 0.57, Tika 3, aws-sdk-kotlin, okhttp, jakarta-mail
- `resume/model` 작성: Resume / JobPosting / BatchRun / EvaluationResult — 각 Identity/Model/데이터클래스 3단 분리 패턴 (study repo와 동일)
- `resume/exception` 4개 NotFoundException

**커밋**: `de86aa2` initial commit, `3bb6902` services

---

## 2026-05-22 ~ 23 — 도메인 로직

- `resume/infrastructure` — Out-Port 8개 작성 (Repository 4 + Storage + Parser + LLM + Notifier)
  - 모듈 이름은 "infrastructure"지만 실제로는 **port 인터페이스 모음**. 구현은 0줄. study repo 컨벤션 그대로.
- `resume/schema` — Liquibase `db.changelog-initial-schema.yaml` + Postgres DDL `initial_schema.sql`
  - study는 MySQL이지만 우리는 Postgres → `BIGSERIAL`, `TIMESTAMPTZ`, `JSONB`
- `resume/service` — 5개 서비스 + 각 `XxxAutoConfiguration`
  - `ResumeReaderService`, `ResumeRegistrationService`, `JobPostingReaderService`, `EvaluationReaderService`, `BatchEvaluationService`
  - `BatchEvaluationService`는 실제 로직 포함 (신규 이력서 조회 → 파싱 → LLM batch 제출 → polling → 결과 저장 → 알림)
- `resume/repository-exposed` — 4개 Table + 4개 ExposedRepository + AutoConfiguration
  - **학습 포인트**: JSONB 매핑 (`jsonb<EvaluationBreakdown>` + Jackson), `batchInsert`, 원자적 카운터 증가 (`SqlExpressionBuilder.plus + intLiteral(1)` — race-safe), `orderBy DESC + limit + offset` 페이징

**커밋**: `141050f`, `a36080b`, `10756d3` (서비스 시리즈), `d06d41c` exposed repositories

---

## 2026-06-08 — 외부 어댑터 골격

- `resume/adapter-parsing` — `TikaResumeParser` (body는 TODO)
- `resume/adapter-llm` — `AnthropicLlmEvaluator` (Anthropic Message Batches API 사용 예정, body TODO)
- `resume/adapter-storage-s3` — `S3ResumeStorage` (aws-sdk-kotlin coroutine, body TODO)
- `resume/adapter-notifier` — `SlackNotifier` / `TeamsNotifier` / `EmailNotifier` 3개 + `NotifierAutoConfiguration`에서 `@ConditionalOnProperty(hrfilter.notifier.channel)`로 분기
- 4개 모듈 모두 `type=kotlin-boot` + AutoConfiguration + `META-INF/spring/...imports`

**메모**: 시그니처만, 메서드 body는 전부 `TODO("...")`. 컴파일 통과가 마일스톤.

**부산물 (잡 정리)**: `settings.gradle.kts`의 더미 `include("resume:untitled")` + 중복 `include("resume:service")` 제거.

**커밋**: `90a1e3f` external adapter skeletons

---

## 2026-06-09 ~ 10 — 조립 (api + 진입점 2개)

### `resume/api` (실제 동작하는 컨트롤러)
- `ResumeApiController` — `POST /api/v1/resumes` (multipart), `GET /api/v1/resumes/{id}`
- `BatchRunApiController` — `GET /api/v1/batch-runs/{id}/evaluations`
- DTO 2개 + companion `from()` 매퍼
- AutoConfiguration은 `@Import(컨트롤러::class)` 패턴 (study repo와 동일)
- 매핑이 워낙 얇아서 TODO 없이 그대로 작동 — 단, service → adapter 단의 `TODO()`가 호출 시 throw

### `application-api` (API 서버 진입점)
- `type=kotlin-boot-mvc-application`
- `HrFilterApiApplication.kt` — `@SpringBootApplication` + UTC + Clock 빈
- `application.yml` — Postgres Testcontainers JDBC URL (`jdbc:tc:postgresql:15:///hrfilter`), port 8080, `local-dev` 프로필은 12346
- `db.changelog-master.yaml` — `:resume:schema`의 initial changelog include
- 의존성: schema + api + repository-exposed + adapter-* 4개

### `resume/batch` (스케줄러)
- `EvaluationBatchJob` — `@Scheduled(cron = "\${hrfilter.batch.cron}")` → `batchEvaluationService.evaluate()` 호출. `runCatching` + 로깅
- 패키지를 `org.hrfilter.resume.batchjob`으로 분리 — service 모듈의 `BatchAutoConfiguration` (FQN: `org.hrfilter.resume.batch.BatchAutoConfiguration`)과 충돌 회피
- `EvaluationBatchJobAutoConfiguration`에 `@EnableScheduling` 부착

### `application-batch` (배치 서버 진입점)
- `type=kotlin-boot-application` (mvc 없음)
- `application.yml` — `spring.main.web-application-type: none`, actuator만 12347 포트, `hrfilter.batch.cron: "0 0 8,14 * * *"` (8시/14시), `local-dev` 프로필은 매 분 (`0 * * * * *`)

**마일스톤**: settings.gradle.kts의 14개 모듈 **전부 컴파일 통과**.

**커밋**: `ce98827` controllers & api server entry point

---

## 다음 (TODO.md 기준)

1. 🅱️ **`./gradlew :application-api:bootRun` 첫 성공** — Docker 데몬 필요 (Testcontainers Postgres). Liquibase 마이그레이션 + Swagger UI `http://localhost:8080/swagger-ui.html` 확인이 첫 진짜 마일스톤
2. 🅲️ **어댑터 실제 로직 채우기** — 추천 순서: Tika (가장 단순) → Slack (ROI 높음, 배치 알림 즉시 확인) → S3 (coroutine) → Anthropic Batches (가장 복잡)
3. 통합 테스트 (`./gradlew check` — Testcontainers 사용)
