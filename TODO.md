# hr-filter TODO

직접 구현하면서 익히기 위한 핸드오프 문서. 헥사고날 + Build Recipe 패턴 적용.

---

## 출발점 (이미 완료된 것)

```
hr-filter/
├── settings.gradle.kts          ✅ 14개 모듈 include
├── build.gradle.kts             ✅ Build Recipe + type별 컨벤션 6개
├── gradle.properties            ✅ group=org.hrfilter, version=0.1.0-SNAPSHOT
├── gradle/libs.versions.toml    ✅ Spring Boot 3.4 / Exposed 0.57 / Tika 3 / AWS S3 / OkHttp 등
├── config/detekt/detekt.yml     ✅
│
└── resume/
    ├── model/                   ✅ Resume, JobPosting, BatchRun, EvaluationResult (+ Identity/Model/Props 분리)
    └── exception/               ✅ 4개 도메인 예외
```

루트 `build.gradle.kts`에 정의된 type 컨벤션:

| `type=` 값 | 자동 적용되는 것 |
|------------|----------------|
| `kotlin` (prefix) | Kotlin JVM + ktlint + detekt + JUnit5 + integrationTest 스위트 |
| `kotlin-lib` | 위 + Java Library |
| `kotlin-boot` | 위 + Spring Boot BOM + kotlin-spring |
| `kotlin-boot-mvc` | 위 + spring-boot-starter-web + springdoc |
| `kotlin-boot-exposed-repository` | 위 + Exposed starter + Postgres JDBC + Testcontainers |
| `kotlin-boot-application` | 위 + bootJar + actuator + liquibase |
| `kotlin-boot-mvc-application` | 위 둘 다 |

→ **모듈의 `build.gradle.kts`는 보통 `dependencies {}` 한 블록만** 들어갑니다. 플러그인/공통 의존성은 type이 자동 처리.

---

## 남은 모듈 (의존성 적은 순서)

### 1. `resume/infrastructure` — Out-Port 인터페이스
**type**: `kotlin` · **의존**: `model`

만들 인터페이스 (`src/main/kotlin/org/hrfilter/resume/...`):
- `repository/ResumeRepository.kt` — `findById`, `findNewlyUploadedSince`, `save`, `updateStatus`
- `repository/JobPostingRepository.kt` — `findById`
- `repository/BatchRunRepository.kt` — `save`, `findById`, `complete`
- `repository/EvaluationResultRepository.kt` — `save`, `findByResumeId`, `findByBatchRunId`
- `storage/ResumeStorage.kt` — `upload(s3Key, bytes)`, `download(s3Key): ByteArray`
- `parser/ResumeParser.kt` — `parse(bytes, mimeType): String`
- `llm/LlmEvaluator.kt` — `submitBatch(jobs): BatchHandle`, `pollResults(handle): List<LlmEvaluation>`
- `notifier/Notifier.kt` — `notify(summary: BatchSummary)` + 데이터 클래스 `BatchSummary`

샘플 참고: `corehr/infrastructure/.../EmployeeRepository.kt`

---

### 2. `resume/schema` — Liquibase 마이그레이션
**type**: `kotlin` · **의존**: 없음

만들 파일 (`src/main/resources/db/changelog/resume/`):
- `db.changelog-initial-schema.yaml` — sqlFile 참조
- `initial_schema.sql` — `resume`, `job_posting`, `batch_run`, `evaluation_result` 테이블 (Postgres 문법)

샘플 참고: `corehr/schema/src/main/resources/db/changelog/corehr/`

> 💡 샘플은 MySQL인데 우리는 Postgres → `DATETIME` 대신 `TIMESTAMP`, `BIGINT AUTO_INCREMENT` 대신 `BIGSERIAL` 사용.

---

### 3. `resume/service` — UseCase
**type**: `kotlin-boot` · **의존**: `model`, `infrastructure`, `exception`

만들 서비스 (인터페이스 + `internal class XxxImpl` 패턴):
- `BatchEvaluationService` — 배치 1회 실행 (신규 이력서 조회 → 파싱 → LLM Batch 제출 → 결과 저장 → 알림)
- `ResumeRegistrationService` — 업로드 받아 S3 저장 + DB row 생성
- `EvaluationLookUpService` — 결과 조회

각 서비스마다:
- `XxxService.kt` (인터페이스 + Impl)
- `XxxAutoConfiguration.kt` (`@AutoConfiguration` + `@Bean`)
- `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`에 FQN 추가

샘플 참고: `corehr/service/.../EmployeeLookUpService.kt` + `EmployeeAutoConfiguration.kt`

---

### 4. `resume/repository-exposed` — Postgres 어댑터
**type**: `kotlin-boot-exposed-repository` · **의존**: `infrastructure`, `model`

만들 것:
- `ResumeTable` (Exposed `Table` 또는 `LongIdTable`)
- `JobPostingTable`, `BatchRunTable`, `EvaluationResultTable`
- 각 `XxxExposedRepository` (Out-Port 구현체)
- `XxxRepositoryAutoConfiguration`
- `META-INF/spring/...AutoConfiguration.imports`

> 💡 Exposed Spring Boot starter가 `TransactionTemplate` 같은 걸 자동 설정해줍니다. `transaction { ... }` 블록 안에서 쿼리.

---

### 5~8. `resume/adapter-*` 4개
**type**: `kotlin-boot` · **의존**: `infrastructure`, `model`

| 모듈 | 구현할 Out-Port | 주 라이브러리 |
|-----|----------------|--------------|
| `adapter-parsing` | `ResumeParser` | `libs.tika.parsers.standard` |
| `adapter-llm` | `LlmEvaluator` | `libs.okhttp` + `libs.jackson.module.kotlin` (Anthropic Message Batches API HTTP 호출) |
| `adapter-storage-s3` | `ResumeStorage` | `libs.aws.s3` (coroutine 기반) |
| `adapter-notifier` | `Notifier` | `libs.okhttp` (Slack/Teams webhook) + `libs.jakarta.mail.api` / `libs.angus.mail` (Email) |

각 모듈 패턴:
- 구현 클래스 (예: `TikaResumeParser`, `S3ResumeStorage`)
- `XxxAutoConfiguration`
- `META-INF/spring/...AutoConfiguration.imports`

> 💡 `adapter-notifier`는 `SlackNotifier`, `TeamsNotifier`, `EmailNotifier` 셋 다 만들고 설정으로 선택 (`hrfilter.notifier.channel=slack|teams|email`). `@ConditionalOnProperty`로 분기.

---

### 9. `resume/api` — REST 컨트롤러
**type**: `kotlin-boot-mvc` · **의존**: `service`, `model`

엔드포인트:
- `POST /api/v1/resumes` — 업로드 (multipart)
- `GET /api/v1/resumes/{id}` — 단건 조회
- `GET /api/v1/batch-runs/{id}/evaluations` — 배치 결과 조회

구조:
- `ResumeApiController` + `ResumeApiAutoConfiguration` (`@Import`)
- `BatchRunApiController` + `BatchRunApiAutoConfiguration`
- `dto/...` (Request/Response)
- `META-INF/spring/...AutoConfiguration.imports`

샘플 참고: `corehr/api/.../EmployeeApiController.kt`

---

### 10. `resume/batch` — 배치 잡
**type**: `kotlin-boot` · **의존**: `service`, `model`

- `EvaluationBatchJob` — `@Scheduled(cron = "0 0 8,14 * * *")` (오전 8시, 오후 2시) → `BatchEvaluationService.runOnce()` 호출
- `BatchAutoConfiguration` (`@EnableScheduling` + `@Import(EvaluationBatchJob::class)`)
- `META-INF/spring/...AutoConfiguration.imports`

> 💡 cron 시간은 `application.yml`로 빼는 게 운영 친화적: `@Scheduled(cron = "\${hrfilter.batch.cron}")`

---

### 11. `application-api` — API 서버 진입점
**type**: `kotlin-boot-mvc-application` · **의존**: `api`, `repository-exposed`, `adapter-*` 전부, `schema`

만들 것:
- `HrFilterApiApplication.kt` — `@SpringBootApplication` + `main()`
- `src/main/resources/application.yml` — datasource, liquibase, actuator 등
- `src/main/resources/db/changelog/db.changelog-master.yaml` — `:resume:schema` 의 changelog include

샘플 참고: `application-api/src/main/kotlin/.../SampleApplication.kt` + `application.yml`

> 💡 `@ComponentScan` 안 씁니다. AutoConfiguration이 의존하는 모듈에서 자동 발견됩니다.

---

### 12. `application-batch` — 배치 서버 진입점
**type**: `kotlin-boot-application` · **의존**: `batch`, `repository-exposed`, `adapter-*` 전부, `schema`

`application-api`와 거의 동일. 차이:
- `mvc` 빠짐 (REST 서버 아님)
- `batch` 모듈 의존 (api 대신)
- `application.yml`에서 server port를 다르게 (예: 12347) — actuator만 노출

---

## 막힐 때 참고 포인트

### 패턴 1:1 참고
- **모듈 구조**: `/Users/jaeheenam/Dev/study/hexagonal-module-sample/corehr/{model,service,infrastructure,api,application-api,...}` 그대로 미러링
- **Identity / Model / 데이터클래스 3단**: `corehr/model/.../employee/Employee*.kt`
- **AutoConfiguration**: `corehr/service/.../EmployeeAutoConfiguration.kt`
- **AutoConfiguration.imports** 파일 형식: `corehr/service/src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`
- **컨트롤러**: `corehr/api/.../EmployeeApiController.kt`
- **앱 진입점**: `application-api/src/main/kotlin/.../SampleApplication.kt`

### 자주 헷갈리는 것

**Q. 모듈 build.gradle.kts가 비어있어도 되나?**
A. 네. `dependencies {}` 한 블록만 보통 필요. 플러그인/공통 deps는 type이 자동 적용. 의존성 없는 모듈은 `dependencies {}` 자체가 빈 블록.

**Q. `@Service` / `@Component` / `@Repository` 안 쓰나?**
A. 안 씁니다. 모든 빈은 `@AutoConfiguration` 클래스의 `@Bean` 메서드로 명시적 등록. 이게 이 패턴의 핵심 규율. `@ComponentScan` 의존 → 각 모듈을 명시적 조립으로 전환한 것.

**Q. AutoConfiguration이 왜 안 잡히지?**
A. `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports` 파일에 FQN 한 줄 추가 빠뜨림. 모듈마다 이 파일이 있어야 함.

**Q. 도메인 간 직접 참조?**
A. 금지. (지금은 도메인이 `resume` 하나라 무관하지만 미래에 `candidate` 같은 도메인 추가 시) → `infrastructure`에 Out-Port 인터페이스 정의 → 받는 쪽 `application-*`에서 어댑터로 주입.

**Q. `api` 모듈에서 `repository-exposed`로 직접 갈 수 있나?**
A. 금지. 반드시 `service` 경유. 컴파일타임에 Gradle 의존성 그래프로 강제됨.

### Build Recipe 동작 (기억해두면 디버깅 쉬움)
- 각 모듈 `gradle.properties`의 `type=` 값을 읽음
- 루트 `build.gradle.kts`의 `configureByTypePrefix("kotlin") { ... }` → `type` 값이 "kotlin"으로 **시작**하는 모듈에 블록 적용
- `configureByTypeHaving("boot", "mvc") { ... }` → `type` 값에 두 단어 **모두 포함**된 모듈에 적용 (하이픈으로 토큰화)
- 매칭 안 되면 그냥 빈 모듈

### 빌드 검증 명령
```bash
./gradlew :resume:model:compileKotlin       # 특정 모듈만
./gradlew compileKotlin                      # 전체 컴파일
./gradlew :application-api:bootRun           # API 서버 기동
./gradlew :application-batch:bootRun         # 배치 서버 기동
./gradlew ktlintCheck                        # 코드 스타일
./gradlew check                              # 전체 검증 (unit + integration)
```

### 추천 진행 순서
1. `resume/infrastructure` 만들고 → `./gradlew :resume:infrastructure:compileKotlin` ✓
2. `resume/schema` (DDL은 일단 한 테이블 정도만)
3. `resume/service` — 인터페이스 + Impl + AutoConfiguration 1세트 먼저, 컴파일만 성공시키고 로직은 TODO 주석
4. `resume/repository-exposed` 한 Repository만 먼저
5. 어댑터 4개는 **시그니처만** 먼저 (TODO 메서드) → 컴파일 통과
6. `api`, `batch` 시그니처만
7. `application-api` 진입점 만들고 `./gradlew :application-api:bootRun` 시도 — 컨텍스트 로드 성공이 첫 마일스톤
8. 그 다음 실제 로직 채워나가기

> 💡 **컴파일 통과 → 컨텍스트 로드 → 실제 로직** 순서. 한 번에 진짜 구현까지 가려고 하면 디버깅 지옥. 골격부터.

---

## 도메인 의사결정 메모 (잊지 말 것)

- **배치 시간 2회**: 오전 8시 + 오후 2시 (조정 가능)
- **LLM은 Anthropic Message Batches API 사용** — 50% 할인 + 24시간 내 처리. 동기 호출 ❌
- **벡터 DB**: MVP 제외. 비용/사전필터링 니즈 생기면 추가
- **EDA(큐/이벤트 버스)**: 도입 트리거 — ATS 연동, 평가 결과 컨슈머 3개+, 실시간 알림, 지원자 상태 UX
- **알림 채널**: Slack/Teams/Email 셋 다 구현하되 설정으로 선택. 메시지엔 평가 건수/통과·보류·탈락 요약 + 대시보드 링크
- **이미지/스캔 PDF**: 스코프 밖. 텍스트 PDF/DOCX/HWP만 (Tika로 처리)
