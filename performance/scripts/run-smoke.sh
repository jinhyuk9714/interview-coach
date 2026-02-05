#!/bin/bash

# Smoke Test 실행 스크립트
# 사용법: ./run-smoke.sh [options]
#   -o, --output    출력 디렉토리 지정
#   -i, --influxdb  InfluxDB로 결과 전송
#   -h, --help      도움말 표시

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"
K6_DIR="$PROJECT_ROOT/performance/k6"
RESULTS_DIR="$PROJECT_ROOT/performance/results"

# 기본값
OUTPUT_DIR="$RESULTS_DIR"
USE_INFLUXDB=false
TIMESTAMP=$(date +%Y%m%d_%H%M%S)

# 환경 변수 기본값
export BASE_URL="${BASE_URL:-http://localhost:8080}"
export USER_SERVICE_URL="${USER_SERVICE_URL:-http://localhost:8081}"
export QUESTION_SERVICE_URL="${QUESTION_SERVICE_URL:-http://localhost:8082}"
export INTERVIEW_SERVICE_URL="${INTERVIEW_SERVICE_URL:-http://localhost:8083}"
export FEEDBACK_SERVICE_URL="${FEEDBACK_SERVICE_URL:-http://localhost:8084}"

# 색상 정의
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

print_help() {
    echo "Smoke Test 실행 스크립트"
    echo ""
    echo "사용법: $0 [options]"
    echo ""
    echo "옵션:"
    echo "  -o, --output DIR    출력 디렉토리 지정 (기본: $RESULTS_DIR)"
    echo "  -i, --influxdb      InfluxDB로 결과 전송"
    echo "  -h, --help          도움말 표시"
    echo ""
    echo "환경 변수:"
    echo "  BASE_URL            API Gateway URL (기본: http://localhost:8080)"
    echo "  K6_INFLUXDB_URL     InfluxDB URL (기본: http://localhost:8086)"
    echo "  K6_INFLUXDB_TOKEN   InfluxDB 인증 토큰"
}

# 인자 파싱
while [[ $# -gt 0 ]]; do
    case $1 in
        -o|--output)
            OUTPUT_DIR="$2"
            shift 2
            ;;
        -i|--influxdb)
            USE_INFLUXDB=true
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

# 출력 디렉토리 생성
mkdir -p "$OUTPUT_DIR"

echo -e "${GREEN}========================================${NC}"
echo -e "${GREEN}   Smoke Test 시작${NC}"
echo -e "${GREEN}========================================${NC}"
echo ""
echo "타겟 URL: $BASE_URL"
echo "결과 저장: $OUTPUT_DIR"
echo ""

# k6 설치 확인
if ! command -v k6 &> /dev/null; then
    echo -e "${RED}k6가 설치되어 있지 않습니다.${NC}"
    echo "설치 방법: https://k6.io/docs/getting-started/installation/"
    exit 1
fi

# 서비스 헬스체크
echo -e "${YELLOW}서비스 상태 확인 중...${NC}"
HEALTH_CHECK=$(curl -s -o /dev/null -w "%{http_code}" "$BASE_URL/actuator/health" 2>/dev/null || echo "000")

if [ "$HEALTH_CHECK" != "200" ]; then
    echo -e "${RED}경고: Gateway 헬스체크 실패 (HTTP $HEALTH_CHECK)${NC}"
    echo "서비스가 실행 중인지 확인하세요."
    read -p "계속 진행하시겠습니까? (y/N): " confirm
    if [ "$confirm" != "y" ] && [ "$confirm" != "Y" ]; then
        exit 1
    fi
else
    echo -e "${GREEN}Gateway 헬스체크 성공${NC}"
fi

# k6 실행 명령 구성
K6_CMD="k6 run"
K6_CMD="$K6_CMD --out json=$OUTPUT_DIR/smoke-test-${TIMESTAMP}.json"
K6_CMD="$K6_CMD --summary-export=$OUTPUT_DIR/smoke-test-summary-${TIMESTAMP}.json"

if [ "$USE_INFLUXDB" = true ]; then
    INFLUXDB_URL="${K6_INFLUXDB_URL:-http://localhost:8086}"
    K6_CMD="$K6_CMD --out influxdb=$INFLUXDB_URL/k6"
fi

K6_CMD="$K6_CMD $K6_DIR/scenarios/smoke-test.js"

# 테스트 실행
echo ""
echo -e "${YELLOW}Smoke Test 실행 중...${NC}"
echo ""

eval $K6_CMD

EXIT_CODE=$?

echo ""
if [ $EXIT_CODE -eq 0 ]; then
    echo -e "${GREEN}========================================${NC}"
    echo -e "${GREEN}   Smoke Test 완료 ✓${NC}"
    echo -e "${GREEN}========================================${NC}"
else
    echo -e "${RED}========================================${NC}"
    echo -e "${RED}   Smoke Test 실패 ✗${NC}"
    echo -e "${RED}========================================${NC}"
fi

echo ""
echo "결과 파일:"
echo "  - $OUTPUT_DIR/smoke-test-${TIMESTAMP}.json"
echo "  - $OUTPUT_DIR/smoke-test-summary-${TIMESTAMP}.json"

exit $EXIT_CODE
