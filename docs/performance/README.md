# 성능 테스트 가이드

AI 면접 코치 프로젝트의 성능/부하 테스트 실행 및 분석 가이드입니다.

## 개요

이 프로젝트는 [k6](https://k6.io/)를 사용하여 성능 테스트를 수행하고, Grafana/InfluxDB로 결과를 시각화합니다.

### 테스트 유형

| 테스트 | 목적 | VU | 시간 |
|--------|------|-----|------|
| Smoke | 기본 동작 확인 | 1-5 | 1분 |
| Load | 일반 운영 부하 | 50-100 | 16분 |
| Stress | 시스템 한계점 파악 | 100-500 | 22분 |
| Spike | 급격한 부하 변화 대응 | 10-500 | 8분 |
| Soak | 장시간 안정성 | 100 | 4시간+ |

## 빠른 시작

### 1. 사전 요구사항

```bash
# k6 설치 (macOS)
brew install k6



# k6 설치 (Ubuntu/Debian)
sudo gpg -k
sudo gpg --no-default-keyring --keyring /usr/share/keyrings/k6-archive-keyring.gpg \
  --keyserver hkp://keyserver.ubuntu.com:80 --recv-keys C5AD17C747E3415A3642D57D77C6C491D6AC1D69
echo "deb [signed-by=/usr/share/keyrings/k6-archive-keyring.gpg] https://dl.k6.io/deb stable main" \
  | sudo tee /etc/apt/sources.list.d/k6.list
sudo apt-get update && sudo apt-get install k6

# jq 설치 (리포트 생성용)
brew install jq  # macOS
sudo apt-get install jq  # Ubuntu
```

### 2. 모니터링 스택 실행

```bash
cd performance/monitoring
docker-compose -f docker-compose.monitoring.yml up -d

# Grafana 접속: http://localhost:3000 (admin/admin)
# InfluxDB 접속: http://localhost:8086
# Prometheus 접속: http://localhost:9090
```

### 3. 테스트 실행

```bash
# Smoke Test (빠른 검증)
./performance/scripts/run-smoke.sh

# Load Test (부하 테스트)
./performance/scripts/run-load.sh -t load

# Stress Test (스트레스 테스트)
./performance/scripts/run-load.sh -t stress

# Spike Test (스파이크 테스트)
./performance/scripts/run-load.sh -t spike
```

### 4. 결과 확인

```bash
# 리포트 생성
./performance/scripts/generate-report.sh

# 결과 비교
./performance/scripts/compare-results.sh results/baseline.json results/current.json

# Grafana 대시보드 확인
open http://localhost:3000/d/k6-load-testing
```

## 디렉토리 구조

```
performance/
├── k6/
│   ├── scenarios/          # 시나리오별 테스트
│   │   ├── smoke-test.js
│   │   ├── load-test.js
│   │   ├── stress-test.js
│   │   └── spike-test.js
│   ├── services/           # 서비스별 테스트
│   │   ├── user-service.js
│   │   ├── question-service.js
│   │   └── feedback-service.js
│   ├── lib/                # 공통 유틸리티
│   │   ├── config.js
│   │   ├── auth.js
│   │   └── sse-helper.js
│   ├── data/               # 테스트 데이터
│   │   ├── users.json
│   │   └── job-descriptions.json
│   └── thresholds.json     # 성능 기준선
├── monitoring/
│   ├── docker-compose.monitoring.yml
│   ├── grafana/
│   │   └── provisioning/
│   ├── prometheus/
│   │   ├── prometheus.yml
│   │   └── alerts.yml
│   └── influxdb/
├── results/                # 테스트 결과 저장
└── scripts/                # 자동화 스크립트
    ├── run-smoke.sh
    ├── run-load.sh
    ├── generate-report.sh
    └── compare-results.sh
```

## 서비스별 테스트 가이드

### User Service

```bash
k6 run performance/k6/services/user-service.js
```

테스트 항목:
- 회원가입 성능
- 로그인/JWT 발급
- 토큰 갱신
- 프로필 조회/수정

### Question Service

```bash
k6 run performance/k6/services/question-service.js
```

테스트 항목:
- JD 분석 (LLM 호출)
- RAG 기반 질문 검색
- 질문 CRUD

> **주의:** LLM 호출은 30초 이상 소요될 수 있습니다.

### Feedback Service

```bash
k6 run performance/k6/services/feedback-service.js
```

테스트 항목:
- SSE 스트리밍 피드백
- 피드백 조회
- 통계 API

## 환경 변수

| 변수명 | 설명 | 기본값 |
|--------|------|--------|
| `BASE_URL` | API Gateway URL | `http://localhost:8080` |
| `USER_SERVICE_URL` | User Service URL | `http://localhost:8081` |
| `QUESTION_SERVICE_URL` | Question Service URL | `http://localhost:8082` |
| `INTERVIEW_SERVICE_URL` | Interview Service URL | `http://localhost:8083` |
| `FEEDBACK_SERVICE_URL` | Feedback Service URL | `http://localhost:8084` |
| `K6_INFLUXDB_URL` | InfluxDB URL | `http://localhost:8086` |
| `K6_INFLUXDB_TOKEN` | InfluxDB 토큰 | - |

## 성능 임계값 (SLO)

| 메트릭 | 목표 | 설명 |
|--------|------|------|
| P95 응답 시간 | < 500ms | 일반 API |
| P95 응답 시간 | < 30s | LLM 호출 |
| 에러율 | < 1% | 전체 요청 |
| 가용성 | > 99.9% | 서비스 가용성 |

## Grafana 대시보드

### k6 Load Testing Dashboard

![k6 Dashboard](../../docs/images/k6-dashboard.png)

- Request Rate (RPS)
- Response Time (P50/P95/P99)
- Error Rate
- Active VUs

### JVM Micrometer Dashboard

- HTTP 메트릭
- JVM 힙 메모리
- GC 일시정지
- HikariCP 커넥션 풀
- 스레드 수

## CI/CD 통합

PR 생성 시 자동으로 Smoke Test가 실행됩니다.

```yaml
# .github/workflows/performance-test.yml
on:
  pull_request:
    branches: [main, develop]
```

수동 실행:
```bash
gh workflow run performance-test.yml -f test_type=load
```

## 문제 해결

### k6 설치 오류

```bash
# macOS에서 arm64 관련 오류
arch -x86_64 brew install k6
```

### InfluxDB 연결 오류

```bash
# 토큰 확인
docker exec interview-coach-influxdb influx auth list

# 버킷 확인
docker exec interview-coach-influxdb influx bucket list
```

### 서비스 연결 오류

```bash
# 서비스 상태 확인
curl http://localhost:8080/actuator/health
curl http://localhost:8081/actuator/health
curl http://localhost:8082/actuator/health
```

## 관련 문서

- [BASELINE.md](./BASELINE.md) - 기준선 측정 결과
- [ANALYSIS.md](./ANALYSIS.md) - 병목 분석 가이드
- [TUNING.md](./TUNING.md) - 최적화 기록
