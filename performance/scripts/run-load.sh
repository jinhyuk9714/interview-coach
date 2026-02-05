#!/bin/bash

# Load Test 실행 스크립트
# 사용법: ./run-load.sh [options]

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"
K6_DIR="$PROJECT_ROOT/performance/k6"
RESULTS_DIR="$PROJECT_ROOT/performance/results"

# 기본값
OUTPUT_DIR="$RESULTS_DIR"
USE_INFLUXDB=true
TIMESTAMP=$(date +%Y%m%d_%H%M%S)
TEST_TYPE="load"

# 환경 변수 기본값
export BASE_URL="${BASE_URL:-http://localhost:8080}"
export USER_SERVICE_URL="${USER_SERVICE_URL:-http://localhost:8081}"
export QUESTION_SERVICE_URL="${QUESTION_SERVICE_URL:-http://localhost:8082}"
export INTERVIEW_SERVICE_URL="${INTERVIEW_SERVICE_URL:-http://localhost:8083}"
export FEEDBACK_SERVICE_URL="${FEEDBACK_SERVICE_URL:-http://localhost:8084}"
export K6_INFLUXDB_URL="${K6_INFLUXDB_URL:-http://localhost:8086}"
export K6_INFLUXDB_TOKEN="${K6_INFLUXDB_TOKEN:-my-super-secret-auth-token}"
export K6_INFLUXDB_ORG="${K6_INFLUXDB_ORG:-interview-coach}"
export K6_INFLUXDB_BUCKET="${K6_INFLUXDB_BUCKET:-k6}"

# 색상 정의
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

print_help() {
    echo "Load/Stress/Spike Test 실행 스크립트"
    echo ""
    echo "사용법: $0 [options]"
    echo ""
    echo "옵션:"
    echo "  -t, --type TYPE     테스트 유형 (load, stress, spike) (기본: load)"
    echo "  -o, --output DIR    출력 디렉토리 지정"
    echo "  --no-influxdb       InfluxDB 출력 비활성화"
    echo "  -h, --help          도움말 표시"
}

# 인자 파싱
while [[ $# -gt 0 ]]; do
    case $1 in
        -t|--type)
            TEST_TYPE="$2"
            shift 2
            ;;
        -o|--output)
            OUTPUT_DIR="$2"
            shift 2
            ;;
        --no-influxdb)
            USE_INFLUXDB=false
            shift
            ;;
        -h|--help)
            print_help
            exit 0
            ;;
        *)
            echo -e "${RED}알 수 없는 옵션: $1${NC}"
            print_help
            exit 1
            ;;
    esac
done

# 테스트 파일 확인
TEST_FILE="$K6_DIR/scenarios/${TEST_TYPE}-test.js"
if [ ! -f "$TEST_FILE" ]; then
    echo -e "${RED}테스트 파일을 찾을 수 없습니다: $TEST_FILE${NC}"
    exit 1
fi

mkdir -p "$OUTPUT_DIR"

echo -e "${BLUE}========================================${NC}"
echo -e "${BLUE}   ${TEST_TYPE^^} Test 시작${NC}"
echo -e "${BLUE}========================================${NC}"
echo ""
echo "테스트 유형: $TEST_TYPE"
echo "타겟 URL: $BASE_URL"
echo "InfluxDB: $K6_INFLUXDB_URL"
echo "결과 저장: $OUTPUT_DIR"
echo ""

# k6 설치 확인
if ! command -v k6 &> /dev/null; then
    echo -e "${RED}k6가 설치되어 있지 않습니다.${NC}"
    exit 1
fi

# 서비스 헬스체크
echo -e "${YELLOW}서비스 상태 확인 중...${NC}"
services=("$BASE_URL" "$USER_SERVICE_URL" "$QUESTION_SERVICE_URL" "$INTERVIEW_SERVICE_URL" "$FEEDBACK_SERVICE_URL")
service_names=("Gateway" "User Service" "Question Service" "Interview Service" "Feedback Service")

all_healthy=true
for i in "${!services[@]}"; do
    HEALTH=$(curl -s -o /dev/null -w "%{http_code}" "${services[$i]}/actuator/health" 2>/dev/null || echo "000")
    if [ "$HEALTH" = "200" ]; then
        echo -e "  ${GREEN}✓${NC} ${service_names[$i]}"
    else
        echo -e "  ${RED}✗${NC} ${service_names[$i]} (HTTP $HEALTH)"
        all_healthy=false
    fi
done

if [ "$all_healthy" = false ]; then
    echo ""
    echo -e "${YELLOW}경고: 일부 서비스가 응답하지 않습니다.${NC}"
    read -p "계속 진행하시겠습니까? (y/N): " confirm
    if [ "$confirm" != "y" ] && [ "$confirm" != "Y" ]; then
        exit 1
    fi
fi

# InfluxDB 연결 확인
if [ "$USE_INFLUXDB" = true ]; then
    echo ""
    echo -e "${YELLOW}InfluxDB 연결 확인 중...${NC}"
    INFLUX_HEALTH=$(curl -s -o /dev/null -w "%{http_code}" "$K6_INFLUXDB_URL/health" 2>/dev/null || echo "000")
    if [ "$INFLUX_HEALTH" = "200" ]; then
        echo -e "  ${GREEN}✓${NC} InfluxDB 연결 성공"
    else
        echo -e "  ${YELLOW}!${NC} InfluxDB 연결 실패 (결과 저장은 JSON으로만 진행)"
        USE_INFLUXDB=false
    fi
fi

# k6 실행 명령 구성
K6_CMD="k6 run"
K6_CMD="$K6_CMD --out json=$OUTPUT_DIR/${TEST_TYPE}-test-${TIMESTAMP}.json"
K6_CMD="$K6_CMD --summary-export=$OUTPUT_DIR/${TEST_TYPE}-test-summary-${TIMESTAMP}.json"

if [ "$USE_INFLUXDB" = true ]; then
    K6_CMD="$K6_CMD --out influxdb=$K6_INFLUXDB_URL/k6"
fi

K6_CMD="$K6_CMD $TEST_FILE"

# 테스트 실행
echo ""
echo -e "${YELLOW}${TEST_TYPE^^} Test 실행 중...${NC}"
echo -e "${YELLOW}예상 소요 시간: $(get_estimated_time $TEST_TYPE)${NC}"
echo ""

# 테스트 시작 시간 기록
START_TIME=$(date +%s)

eval $K6_CMD

EXIT_CODE=$?

# 테스트 종료 시간 계산
END_TIME=$(date +%s)
DURATION=$((END_TIME - START_TIME))
DURATION_MIN=$((DURATION / 60))
DURATION_SEC=$((DURATION % 60))

echo ""
if [ $EXIT_CODE -eq 0 ]; then
    echo -e "${GREEN}========================================${NC}"
    echo -e "${GREEN}   ${TEST_TYPE^^} Test 완료 ✓${NC}"
    echo -e "${GREEN}========================================${NC}"
else
    echo -e "${RED}========================================${NC}"
    echo -e "${RED}   ${TEST_TYPE^^} Test 실패 ✗${NC}"
    echo -e "${RED}========================================${NC}"
fi

echo ""
echo "실행 시간: ${DURATION_MIN}분 ${DURATION_SEC}초"
echo ""
echo "결과 파일:"
echo "  - $OUTPUT_DIR/${TEST_TYPE}-test-${TIMESTAMP}.json"
echo "  - $OUTPUT_DIR/${TEST_TYPE}-test-summary-${TIMESTAMP}.json"

if [ "$USE_INFLUXDB" = true ]; then
    echo ""
    echo -e "${BLUE}Grafana 대시보드에서 결과를 확인하세요:${NC}"
    echo "  http://localhost:3000/d/k6-load-testing"
fi

exit $EXIT_CODE

# 예상 시간 함수
get_estimated_time() {
    case $1 in
        load)
            echo "약 16분"
            ;;
        stress)
            echo "약 22분"
            ;;
        spike)
            echo "약 8분"
            ;;
        *)
            echo "알 수 없음"
            ;;
    esac
}
