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

## 2026-06-11 — bootRun 첫 성공 + 어댑터 4종 실제 로직

### 로컬 스택 전환 (Testcontainers → docker-compose)
- `docker-compose.yml` — Postgres 15 + MinIO(S3 호환) + minio-init(버킷 자동 생성). 헬스체크 포함
- 두 진입점 `application.yml`을 Testcontainers JDBC URL → 실제 docker-compose 접속으로 변경. `application-*/build.gradle.kts`에서 testcontainers deps 제거
- **삽질**: bootRun이 `UnsupportedClassVersionError` (class 65.0 vs 61.0) — toolchain 미선언이라 Gradle 데몬이 잡은 JVM(homebrew openjdk@17)으로 fork. 캐시 클래스는 21로 컴파일됨 → 충돌
  - 해결: 루트 `build.gradle.kts`의 `JavaPluginExtension`에 `toolchain { languageVersion = 21 }` 추가 → 컴파일·실행 JVM 통일
  - `~/.zshrc`에 `jdk()` 버전 스위처 함수 추가 (수동 전환용, 기본 JDK는 그대로 17)

### 🅱️ bootRun 첫 성공 (마일스톤)
- `./gradlew :application-api:bootRun` → Liquibase 마이그레이션 정상, Tomcat 8080, 1.3초 기동
- `/actuator/health` UP, `/swagger-ui/index.html` 200, `/v3/api-docs` 200 확인

### 🅲️ 어댑터 실제 로직 (4종 전부 `TODO()` 제거)
- **Tika** (`TikaResumeParser`) — `AutoDetectParser` + `BodyContentHandler(-1)`(100KB 쓰기 제한 해제) + `Content-Type` 힌트. **빌드 픽스**: `tika-parsers-standard-package`는 파서 구현체만, 코어 API(`AutoDetectParser`/`BodyContentHandler`/`Metadata`)는 `tika-core`에 → `libs.tika.core` 의존성 추가
- **Slack/Teams** — okhttp 웹훅 POST. Slack은 `{text}`, Teams는 MessageCard. 메시지 본문은 `BatchSummary.toMessageText()` 공통 함수
- **Email** — jakarta.mail SMTP (`MimeMessage` + STARTTLS + `Authenticator`)
- **Anthropic** (`AnthropicLlmEvaluator`) — Message Batches API 직접 HTTP 호출 (SDK 미사용, 프로젝트 컨벤션대로 okhttp+jackson)
  - `submitBatch`: `POST /v1/messages/batches`, 요청마다 `output_config.format`(json_schema)로 평가 JSON 구조 강제 (verdict/score/breakdown/reasoning)
  - `pollResults`: `GET .../{id}` → `processing_status`(in_progress/ended/canceling) 매핑 → ended면 `GET .../{id}/results` JSONL 파싱
  - 모델 `claude-opus-4-8`, `custom_id="resume-{id}"`로 결과↔이력서 매핑
- **신규 패턴**: 어댑터 설정을 `@ConfigurationProperties` + `@EnableConfigurationProperties`로 바인딩 (NotifierProperties/S3StorageProperties/AnthropicLlmProperties). 프로젝트 첫 도입
  - **삽질**: notifier/llm에서 `ObjectMapper`를 `@Bean`으로 노출 → Spring MVC가 단일 ObjectMapper 기대하는데 2개 발견되어 부팅 실패. okhttp client/ObjectMapper는 어댑터 내부 `private`로 강등

### 검증
- **S3 end-to-end (MinIO)**: job_posting 시드 → `POST /api/v1/resumes` 멀티파트 업로드 → MinIO 버킷에 객체 생성 확인(92B), `object_key` DB 저장, `GET` 조회까지 통과
- Tika는 컴파일+API 정합성만 (실파일 파싱은 배치에서). Slack/Teams/Email/Anthropic은 컴파일+빈 와이어링 확인 (웹훅 URL/API 키 없어 실호출 미검증)
- `compileKotlin` 전체 통과, 어댑터 4종 `ktlintCheck` 통과

---

## 다음

1. **통합 테스트** — `kotlin` 타입의 integrationTest 스위트 활용. S3는 MinIO Testcontainer로 실검증 가능. Anthropic은 mock 웹서버(okhttp MockWebServer)로 batches 흐름 검증 권장 (실 API는 키+최대 24h 소요라 CI 부적합)
2. **배치 실행 경로 검증** — `application-batch` bootRun + `ANTHROPIC_API_KEY` 세팅 후 소량 이력서로 `evaluate()` 1회 실제 돌려보기. local-dev 프로필 cron 매분
3. **운영 보강** — Bean Validation provider 추가(DTO 검증 활성화), Slack 웹훅 URL 등 시크릿 주입 경로 정리(env → 배포 시 secret store)
