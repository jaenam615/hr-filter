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

## 2026-06-12 — 단위 테스트 · 대시보드 · 공고 관리 · 배치 견고성 · 입력 검증

### 단위 테스트 (Kotest `DescribeSpec` + MockK)
- 포트 전부 목으로 대체한 서비스 단위 테스트 우선(통합 최소화). Tika in-process 파싱, 알림 메시지 포매팅도.

### 대시보드 (Thymeleaf) + 채용 공고 관리
- `resume/api`에 `@Controller` 대시보드 — 평가 결과 표 + 업로드 폼 + 공고 등록 폼·드롭다운. `BatchRunReaderService`(최신 배치 조회) 추가.
- 공고 생성/목록 REST(`/api/v1/job-postings`) + `JobPostingRegistrationService`, `JobPostingRepository.save/findAll`. → DB 직접 insert 탈출.

### 배치 견고성 — 제출/수거 분리 (가장 큰 리팩터)
- **문제**: 기존 `evaluate()`가 제출 후 그 자리에서 블로킹 폴링(`while IN_PROGRESS { Thread.sleep(30s) }`). Batches는 완료 푸시(webhook)가 없고 SLA가 최대 24h → 스레드 장시간 점유 + 재시작 시 batchHandle 유실 → 배치 고아·중복 제출.
- **해결**: `BatchEvaluationService`를 **`BatchSubmissionService`(제출) + `BatchCollectionService`(수거)** 로 분리.
  - 제출 잡: RUNNING 있으면 skip(단일 in-flight 불변식) → 제출 → `batch_run.provider_batch_id` 영속 + 이력서 `SUBMITTED` → 즉시 리턴.
  - 수거 잡(5분 cron): RUNNING 1회 폴링 → COMPLETED 저장·알림 / FAILED·25h stale면 `SUBMITTED→UPLOADED` 롤백 후 FAILED. 논블로킹·재시작 안전.
- **스키마**: `batch_run.provider_batch_id` 컬럼(Liquibase addColumn), `ResumeStatus.SUBMITTED` 추가.
- 설계 근거(푸시리스 reconciliation·폴링 비용 오해 정정·고정 cron vs 동적 예약)는 `technical-challenges.md` §2.

### 입력 검증 + 전역 예외 핸들러
- `spring-boot-starter-validation` + DTO/파라미터 제약(`@NotBlank`/`@Email`/`@Positive`). 부팅 시 validation provider 경고 해소.
- `@RestControllerAdvice` 전역 핸들러: 도메인 `NotFoundException`→`404`, 검증 위반→`400 + fieldErrors`, 그 외→`500`(내부 메시지 비노출). 도메인은 HTTP 무지 — 번역은 경계에서.
- 도메인 예외 공통 베이스 `NotFoundException` 신설. `-java-parameters` 컴파일 플래그로 검증 메시지에 실제 인자명 노출.

### 실전 e2e 검증 + 샘플 스크립트
- **실 PDF 이력서 + JD로 전체 파이프라인 완주** — `sample/` 기반 `scripts/sample-test-run.sh`(공고 생성→업로드→평가 대기→결과). 결과: 남재희 PASS 80, 이력서 실내용 인용 + JD 갭 지적.
- **multipart 한도 상향**(기본 1MB → 10MB): 샘플 PDF가 1.05MB라 업로드 413 발생 → `spring.servlet.multipart` 설정.

**커밋**: (기능 단위로 분할, Claude 푸터 제외)

---

## 다음

1. **인증·인가** — 이력서=민감 PII. 방식 미정(폼+세션 / Basic / JWT). 우선순위 높음.
2. **통합 테스트** — S3는 MinIO Testcontainer, Anthropic은 MockWebServer로 batches 흐름.
3. **운영 보강** — 관측성(메트릭/구조화 로깅), 시크릿 스토어, 이력서 원문 열람 화면.
