#!/bin/bash

# 성능 테스트 리포트 생성 스크립트
# 사용법: ./generate-report.sh <test-type> <summary-file>

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"
RESULTS_DIR="$PROJECT_ROOT/performance/results"
REPORT_DIR="$PROJECT_ROOT/performance-report"

TEST_TYPE="${1:-all}"
SUMMARY_FILE="${2:-}"
TIMESTAMP=$(date +%Y%m%d_%H%M%S)

# 색상 정의
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

# jq 설치 확인
if ! command -v jq &> /dev/null; then
    echo -e "${RED}jq가 설치되어 있지 않습니다.${NC}"
    echo "설치: brew install jq (macOS) 또는 apt-get install jq (Ubuntu)"
    exit 1
fi

mkdir -p "$REPORT_DIR"

echo -e "${BLUE}========================================${NC}"
echo -e "${BLUE}   성능 테스트 리포트 생성${NC}"
echo -e "${BLUE}========================================${NC}"
echo ""

# 리포트 헤더 생성
generate_header() {
    cat << EOF
# 성능 테스트 리포트

**생성일시:** $(date '+%Y-%m-%d %H:%M:%S')
**테스트 유형:** $TEST_TYPE
**환경:** $(uname -s) $(uname -m)

---

EOF
}

# 메트릭 요약 생성
generate_metrics_summary() {
    local file="$1"
    local test_name="$2"

    if [ ! -f "$file" ]; then
        echo "### $test_name"
        echo ""
        echo "결과 파일을 찾을 수 없습니다."
        echo ""
        return
    fi

    local total_requests=$(jq '.metrics.http_reqs.values.count // 0' "$file")
    local failed_rate=$(jq '.metrics.http_req_failed.values.rate // 0' "$file")
    local avg_duration=$(jq '.metrics.http_req_duration.values.avg // 0' "$file")
    local p95_duration=$(jq '.metrics.http_req_duration.values["p(95)"] // 0' "$file")
    local p99_duration=$(jq '.metrics.http_req_duration.values["p(99)"] // 0' "$file")
    local max_duration=$(jq '.metrics.http_req_duration.values.max // 0' "$file")
    local checks_rate=$(jq '.metrics.checks.values.rate // 0' "$file")

    # 상태 판단
    local status="✅ PASS"
    if (( $(echo "$failed_rate > 0.01" | bc -l) )); then
        status="❌ FAIL (에러율 초과)"
    elif (( $(echo "$p95_duration > 500" | bc -l) )); then
        status="⚠️ WARNING (P95 지연)"
    fi

    cat << EOF
### $test_name

**상태:** $status

| 메트릭 | 값 |
|--------|-----|
| 총 요청 수 | $(printf "%'d" $total_requests) |
| 에러율 | $(printf "%.2f%%" $(echo "$failed_rate * 100" | bc -l)) |
| 평균 응답 시간 | $(printf "%.2f ms" $avg_duration) |
| P95 응답 시간 | $(printf "%.2f ms" $p95_duration) |
| P99 응답 시간 | $(printf "%.2f ms" $p99_duration) |
| 최대 응답 시간 | $(printf "%.2f ms" $max_duration) |
| 체크 성공률 | $(printf "%.2f%%" $(echo "$checks_rate * 100" | bc -l)) |

EOF
}

# 임계값 분석 생성
generate_threshold_analysis() {
    local file="$1"

    if [ ! -f "$file" ]; then
        return
    fi

    echo "### 임계값 분석"
    echo ""
    echo "| 임계값 | 목표 | 실제 | 결과 |"
    echo "|--------|------|------|------|"

    # http_req_duration p95
    local p95=$(jq '.metrics.http_req_duration.values["p(95)"] // 0' "$file")
    local p95_target=500
    local p95_result="✅"
    if (( $(echo "$p95 > $p95_target" | bc -l) )); then
        p95_result="❌"
    fi
    echo "| P95 응답 시간 | < ${p95_target}ms | $(printf "%.2f ms" $p95) | $p95_result |"

    # http_req_failed
    local failed=$(jq '.metrics.http_req_failed.values.rate // 0' "$file")
    local failed_target=0.01
    local failed_result="✅"
    if (( $(echo "$failed > $failed_target" | bc -l) )); then
        failed_result="❌"
    fi
    echo "| 에러율 | < 1% | $(printf "%.2f%%" $(echo "$failed * 100" | bc -l)) | $failed_result |"

    # checks
    local checks=$(jq '.metrics.checks.values.rate // 0' "$file")
    local checks_target=0.95
    local checks_result="✅"
    if (( $(echo "$checks < $checks_target" | bc -l) )); then
        checks_result="❌"
    fi
    echo "| 체크 성공률 | > 95% | $(printf "%.2f%%" $(echo "$checks * 100" | bc -l)) | $checks_result |"

    echo ""
}

# 권장 사항 생성
generate_recommendations() {
    local file="$1"

    if [ ! -f "$file" ]; then
        return
    fi

    local p95=$(jq '.metrics.http_req_duration.values["p(95)"] // 0' "$file")
    local failed=$(jq '.metrics.http_req_failed.values.rate // 0' "$file")

    echo "### 권장 사항"
    echo ""

    if (( $(echo "$p95 > 1000" | bc -l) )); then
        echo "- 🔴 **높은 응답 지연**: P95 응답 시간이 1초를 초과합니다."
        echo "  - 데이터베이스 쿼리 최적화 검토"
        echo "  - 캐싱 전략 개선"
        echo "  - N+1 쿼리 확인"
        echo ""
    elif (( $(echo "$p95 > 500" | bc -l) )); then
        echo "- 🟡 **응답 지연 주의**: P95 응답 시간이 500ms를 초과합니다."
        echo "  - 느린 엔드포인트 프로파일링 권장"
        echo ""
    fi

    if (( $(echo "$failed > 0.05" | bc -l) )); then
        echo "- 🔴 **높은 에러율**: 에러율이 5%를 초과합니다."
        echo "  - 에러 로그 분석 필요"
        echo "  - 서비스 리소스 확인"
        echo "  - 외부 의존성 상태 확인"
        echo ""
    elif (( $(echo "$failed > 0.01" | bc -l) )); then
        echo "- 🟡 **에러율 주의**: 에러율이 1%를 초과합니다."
        echo "  - 에러 유형 분석 권장"
        echo ""
    fi

    if (( $(echo "$p95 <= 500" | bc -l) )) && (( $(echo "$failed <= 0.01" | bc -l) )); then
        echo "- 🟢 **양호한 상태**: 모든 메트릭이 목표 범위 내에 있습니다."
        echo ""
    fi
}

# 메인 리포트 생성
REPORT_FILE="$REPORT_DIR/performance-report-${TIMESTAMP}.md"

{
    generate_header

    if [ "$TEST_TYPE" = "all" ] && [ -d "$SUMMARY_FILE" ]; then
        # 여러 테스트 결과 처리
        for result_dir in "$SUMMARY_FILE"/*; do
            if [ -d "$result_dir" ]; then
                dir_name=$(basename "$result_dir")
                for summary in "$result_dir"/*-summary-*.json; do
                    if [ -f "$summary" ]; then
                        generate_metrics_summary "$summary" "$dir_name"
                        generate_threshold_analysis "$summary"
                    fi
                done
            fi
        done
    elif [ -f "$SUMMARY_FILE" ]; then
        # 단일 테스트 결과 처리
        generate_metrics_summary "$SUMMARY_FILE" "$TEST_TYPE Test"
        generate_threshold_analysis "$SUMMARY_FILE"
        generate_recommendations "$SUMMARY_FILE"
    else
        # 최신 결과 파일 찾기
        latest_summary=$(ls -t "$RESULTS_DIR"/*-summary-*.json 2>/dev/null | head -1)
        if [ -f "$latest_summary" ]; then
            generate_metrics_summary "$latest_summary" "Latest Test"
            generate_threshold_analysis "$latest_summary"
            generate_recommendations "$latest_summary"
        else
            echo "결과 파일을 찾을 수 없습니다."
        fi
    fi

    cat << EOF
---

## 다음 단계

1. **Grafana 대시보드** 확인: http://localhost:3000
2. **상세 분석** 문서: docs/performance/ANALYSIS.md
3. **튜닝 기록**: docs/performance/TUNING.md

---

*이 리포트는 자동 생성되었습니다.*
EOF

} > "$REPORT_FILE"

echo -e "${GREEN}리포트가 생성되었습니다: $REPORT_FILE${NC}"

# HTML 버전도 생성 (pandoc이 있는 경우)
if command -v pandoc &> /dev/null; then
    HTML_FILE="$REPORT_DIR/performance-report-${TIMESTAMP}.html"
    pandoc "$REPORT_FILE" -o "$HTML_FILE" --standalone --metadata title="Performance Test Report"
    echo -e "${GREEN}HTML 리포트: $HTML_FILE${NC}"
fi
