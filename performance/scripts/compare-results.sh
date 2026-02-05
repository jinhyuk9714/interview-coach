#!/bin/bash

# 성능 테스트 결과 비교 스크립트
# 사용법: ./compare-results.sh <baseline-file> <current-file>

set -e

BASELINE_FILE="$1"
CURRENT_FILE="$2"

# 색상 정의
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

if [ -z "$BASELINE_FILE" ] || [ -z "$CURRENT_FILE" ]; then
    echo "사용법: $0 <baseline-file> <current-file>"
    echo ""
    echo "예시: $0 results/baseline.json results/current.json"
    exit 1
fi

if [ ! -f "$BASELINE_FILE" ]; then
    echo -e "${RED}Baseline 파일을 찾을 수 없습니다: $BASELINE_FILE${NC}"
    exit 1
fi

if [ ! -f "$CURRENT_FILE" ]; then
    echo -e "${RED}Current 파일을 찾을 수 없습니다: $CURRENT_FILE${NC}"
    exit 1
fi

# jq 확인
if ! command -v jq &> /dev/null; then
    echo -e "${RED}jq가 설치되어 있지 않습니다.${NC}"
    exit 1
fi

echo -e "${BLUE}========================================${NC}"
echo -e "${BLUE}   성능 테스트 결과 비교${NC}"
echo -e "${BLUE}========================================${NC}"
echo ""
echo "Baseline: $BASELINE_FILE"
echo "Current:  $CURRENT_FILE"
echo ""

# 메트릭 추출 함수
extract_metric() {
    local file="$1"
    local path="$2"
    jq -r "$path // 0" "$file"
}

# 변화율 계산 함수
calc_change() {
    local baseline="$1"
    local current="$2"

    if (( $(echo "$baseline == 0" | bc -l) )); then
        echo "N/A"
        return
    fi

    local change=$(echo "scale=2; (($current - $baseline) / $baseline) * 100" | bc -l)
    echo "$change"
}

# 변화 표시 함수
show_change() {
    local change="$1"
    local metric_type="$2"  # "lower_better" or "higher_better"

    if [ "$change" = "N/A" ]; then
        echo "N/A"
        return
    fi

    local abs_change=$(echo "$change" | tr -d '-')
    local is_negative=$(echo "$change < 0" | bc -l)

    if [ "$metric_type" = "lower_better" ]; then
        if [ "$is_negative" = "1" ]; then
            echo -e "${GREEN}↓ ${abs_change}%${NC}"
        else
            echo -e "${RED}↑ ${change}%${NC}"
        fi
    else
        if [ "$is_negative" = "1" ]; then
            echo -e "${RED}↓ ${abs_change}%${NC}"
        else
            echo -e "${GREEN}↑ ${change}%${NC}"
        fi
    fi
}

# 메트릭 비교
echo "┌─────────────────────────────────────────────────────────────┐"
echo "│                     성능 메트릭 비교                          │"
echo "├─────────────────────┬───────────┬───────────┬───────────────┤"
echo "│       메트릭         │  Baseline │  Current  │     변화      │"
echo "├─────────────────────┼───────────┼───────────┼───────────────┤"

# 총 요청 수
baseline_reqs=$(extract_metric "$BASELINE_FILE" '.metrics.http_reqs.values.count')
current_reqs=$(extract_metric "$CURRENT_FILE" '.metrics.http_reqs.values.count')
change=$(calc_change "$baseline_reqs" "$current_reqs")
printf "│ %-19s │ %9s │ %9s │ %s\n" "총 요청 수" "$baseline_reqs" "$current_reqs" "$(show_change "$change" "higher_better")"

# 에러율
baseline_error=$(extract_metric "$BASELINE_FILE" '.metrics.http_req_failed.values.rate')
current_error=$(extract_metric "$CURRENT_FILE" '.metrics.http_req_failed.values.rate')
baseline_error_pct=$(printf "%.2f%%" $(echo "$baseline_error * 100" | bc -l))
current_error_pct=$(printf "%.2f%%" $(echo "$current_error * 100" | bc -l))
change=$(calc_change "$baseline_error" "$current_error")
printf "│ %-19s │ %9s │ %9s │ %s\n" "에러율" "$baseline_error_pct" "$current_error_pct" "$(show_change "$change" "lower_better")"

# 평균 응답 시간
baseline_avg=$(extract_metric "$BASELINE_FILE" '.metrics.http_req_duration.values.avg')
current_avg=$(extract_metric "$CURRENT_FILE" '.metrics.http_req_duration.values.avg')
baseline_avg_ms=$(printf "%.0fms" $baseline_avg)
current_avg_ms=$(printf "%.0fms" $current_avg)
change=$(calc_change "$baseline_avg" "$current_avg")
printf "│ %-19s │ %9s │ %9s │ %s\n" "평균 응답 시간" "$baseline_avg_ms" "$current_avg_ms" "$(show_change "$change" "lower_better")"

# P95 응답 시간
baseline_p95=$(extract_metric "$BASELINE_FILE" '.metrics.http_req_duration.values["p(95)"]')
current_p95=$(extract_metric "$CURRENT_FILE" '.metrics.http_req_duration.values["p(95)"]')
baseline_p95_ms=$(printf "%.0fms" $baseline_p95)
current_p95_ms=$(printf "%.0fms" $current_p95)
change=$(calc_change "$baseline_p95" "$current_p95")
printf "│ %-19s │ %9s │ %9s │ %s\n" "P95 응답 시간" "$baseline_p95_ms" "$current_p95_ms" "$(show_change "$change" "lower_better")"

# P99 응답 시간
baseline_p99=$(extract_metric "$BASELINE_FILE" '.metrics.http_req_duration.values["p(99)"]')
current_p99=$(extract_metric "$CURRENT_FILE" '.metrics.http_req_duration.values["p(99)"]')
baseline_p99_ms=$(printf "%.0fms" $baseline_p99)
current_p99_ms=$(printf "%.0fms" $current_p99)
change=$(calc_change "$baseline_p99" "$current_p99")
printf "│ %-19s │ %9s │ %9s │ %s\n" "P99 응답 시간" "$baseline_p99_ms" "$current_p99_ms" "$(show_change "$change" "lower_better")"

# 처리량 (RPS)
baseline_rps=$(extract_metric "$BASELINE_FILE" '.metrics.http_reqs.values.rate')
current_rps=$(extract_metric "$CURRENT_FILE" '.metrics.http_reqs.values.rate')
baseline_rps_fmt=$(printf "%.1f" $baseline_rps)
current_rps_fmt=$(printf "%.1f" $current_rps)
change=$(calc_change "$baseline_rps" "$current_rps")
printf "│ %-19s │ %9s │ %9s │ %s\n" "처리량 (RPS)" "$baseline_rps_fmt" "$current_rps_fmt" "$(show_change "$change" "higher_better")"

echo "└─────────────────────┴───────────┴───────────┴───────────────┘"

echo ""

# 회귀 감지
echo -e "${BLUE}회귀 분석:${NC}"
echo ""

regression_found=false

# P95 20% 이상 증가 시 회귀
p95_change=$(calc_change "$baseline_p95" "$current_p95")
if [ "$p95_change" != "N/A" ] && (( $(echo "$p95_change > 20" | bc -l) )); then
    echo -e "${RED}⚠️  P95 응답 시간이 20% 이상 증가했습니다 (${p95_change}%)${NC}"
    regression_found=true
fi

# 에러율 1% 이상 증가 시 회귀
error_diff=$(echo "$current_error - $baseline_error" | bc -l)
if (( $(echo "$error_diff > 0.01" | bc -l) )); then
    echo -e "${RED}⚠️  에러율이 1% 이상 증가했습니다${NC}"
    regression_found=true
fi

# 처리량 10% 이상 감소 시 회귀
rps_change=$(calc_change "$baseline_rps" "$current_rps")
if [ "$rps_change" != "N/A" ] && (( $(echo "$rps_change < -10" | bc -l) )); then
    echo -e "${RED}⚠️  처리량이 10% 이상 감소했습니다 (${rps_change}%)${NC}"
    regression_found=true
fi

if [ "$regression_found" = false ]; then
    echo -e "${GREEN}✅ 성능 회귀가 감지되지 않았습니다.${NC}"
fi

echo ""

# 개선 사항
echo -e "${BLUE}개선 사항:${NC}"
echo ""

improvement_found=false

if [ "$p95_change" != "N/A" ] && (( $(echo "$p95_change < -10" | bc -l) )); then
    abs_change=$(echo "$p95_change" | tr -d '-')
    echo -e "${GREEN}✨ P95 응답 시간이 ${abs_change}% 개선되었습니다${NC}"
    improvement_found=true
fi

if [ "$rps_change" != "N/A" ] && (( $(echo "$rps_change > 10" | bc -l) )); then
    echo -e "${GREEN}✨ 처리량이 ${rps_change}% 증가했습니다${NC}"
    improvement_found=true
fi

if [ "$improvement_found" = false ]; then
    echo "특별한 개선 사항이 없습니다."
fi

# Exit code (회귀 발견 시 실패)
if [ "$regression_found" = true ]; then
    exit 1
fi

exit 0
