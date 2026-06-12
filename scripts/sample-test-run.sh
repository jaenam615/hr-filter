#!/usr/bin/env bash
#
# 샘플 JD + 이력서로 전체 평가 파이프라인을 한 번 돌려보는 테스트런 스크립트.
#
#   1) sample/jd/*.md   → 채용 공고 생성 (REST)
#   2) sample/resume/*  → 이력서 업로드 (REST, MinIO 저장)
#   3) 배치 서버가 평가할 때까지 폴링 → 결과 출력
#
# 사전 조건:
#   - docker compose up -d         (Postgres + MinIO)
#   - API 서버:   ./gradlew :application-api:bootRun
#   - 배치 서버:  set -a && source .env && set +a
#                 ./gradlew :application-batch:bootRun --args='--spring.profiles.active=local-dev'
#
# 사용법:  scripts/sample-test-run.sh [--reset]
#   --reset : 기존 resume/batch_run/evaluation_result/job_posting 데이터 초기화 후 실행
set -euo pipefail

BASE_URL="${BASE_URL:-http://localhost:8080}"
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
JD_FILE="$ROOT/sample/jd/product-engineer-backend.md"
RESUME_DIR="$ROOT/sample/resume"
PG=(docker exec hr-filter-postgres psql -U hrfilter -d hrfilter)
POLL_TIMEOUT_SECS="${POLL_TIMEOUT_SECS:-600}"

command -v jq >/dev/null || { echo "❌ jq가 필요합니다 (brew install jq)"; exit 1; }
curl -sf "$BASE_URL/actuator/health" >/dev/null || {
  echo "❌ API 서버($BASE_URL)가 안 떠있습니다 → ./gradlew :application-api:bootRun"; exit 1; }

if [[ "${1:-}" == "--reset" ]]; then
  echo "♻️  기존 데이터 초기화..."
  "${PG[@]}" -c "TRUNCATE evaluation_result, batch_run RESTART IDENTITY CASCADE; DELETE FROM resume; DELETE FROM job_posting;" >/dev/null
fi

# 1) JD → 채용 공고
title="$(head -1 "$JD_FILE" | tr -d '\r')"
payload="$(jq -n --arg t "$title" --rawfile d "$JD_FILE" \
  '{title: $t, description: $d, requirements: "JD 본문(description) 참조"}')"
jobPostingId="$(curl -sf -X POST "$BASE_URL/api/v1/job-postings" \
  -H 'Content-Type: application/json' -d "$payload" | jq -r '.jobPostingId')"
echo "✅ 채용 공고 #$jobPostingId 생성: $title"

# 2) 이력서 업로드
resumeIds=()
for pdf in "$RESUME_DIR"/*; do
  [[ -f "$pdf" ]] || continue
  fname="$(basename "$pdf")"
  applicant="${fname%%-*}"
  rid="$(curl -sf -X POST "$BASE_URL/api/v1/resumes" \
    -F "jobPostingId=$jobPostingId" \
    -F "applicantName=$applicant" \
    -F "applicantEmail=${applicant}@example.com" \
    -F "file=@$pdf" | jq -r '.resumeId')"
  resumeIds+=("$rid")
  echo "✅ 이력서 #$rid 업로드: $applicant ($fname)"
done

# 3) 배치 평가 대기
# (배치 서버는 web-application-type=none이라 HTTP 헬스체크가 없음 — 실행 여부는 안내만)
echo
echo "ℹ️  평가는 배치 서버(local-dev 프로필)가 실행 중일 때 진행됩니다. 안 떠있다면 다른 터미널에서:"
echo "      set -a && source .env && set +a"
echo "      ./gradlew :application-batch:bootRun --args='--spring.profiles.active=local-dev'"
echo "⏳ 평가 완료 대기 중 (최대 $((POLL_TIMEOUT_SECS / 60))분)..."
deadline=$(($(date +%s) + POLL_TIMEOUT_SECS))
for rid in "${resumeIds[@]}"; do
  while :; do
    status="$(curl -sf "$BASE_URL/api/v1/resumes/$rid" | jq -r '.status')"
    [[ "$status" == "EVALUATED" ]] && { echo "  resume #$rid → EVALUATED"; break; }
    if (($(date +%s) > deadline)); then echo "  ⏱️  타임아웃 (resume #$rid status=$status)"; break; fi
    sleep 10
  done
done

# 4) 결과 출력
echo
echo "=== 평가 결과 ==="
ids="$(IFS=,; echo "${resumeIds[*]}")"
"${PG[@]}" -x -c "SELECT r.applicant_name, e.verdict, e.score, e.breakdown, e.reasoning
  FROM evaluation_result e JOIN resume r ON r.resume_id = e.resume_id
  WHERE e.resume_id IN ($ids) ORDER BY e.resume_id;"
echo "🖥️  대시보드: $BASE_URL/dashboard"
