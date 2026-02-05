# CLAUDE.md

AI 면접 코치 프로젝트 개발 가이드

## 프로젝트 개요

JD 기반 면접 질문 생성 + AI 모의 면접 + 피드백 시스템

## 기술 스택

- Backend: Java 21, Spring Boot 3, LangChain4j
- Database: PostgreSQL, Redis, ChromaDB
- Frontend: Next.js 14, TypeScript, Tailwind CSS
- LLM: Claude API (Primary), OpenAI API (Fallback)

## 명령어

```bash
# 인프라 실행
cd infra/docker && docker-compose up -d

# 백엔드 빌드
cd backend && ./gradlew build

# 특정 서비스 실행
cd backend && ./gradlew :question-service:bootRun

# 프론트엔드 실행
cd frontend/web && npm run dev
```

## 서비스 구조

| 서비스 | 포트 | 역할 |
|--------|------|------|
| gateway | 8080 | API Gateway, 인증 |
| user-service | 8081 | 회원 관리, JWT |
| question-service | 8082 | JD 분석, 질문 생성, RAG |
| interview-service | 8083 | 모의 면접 세션 관리 |
| feedback-service | 8084 | 답변 평가, 통계 |

## 주요 패턴

- RAG: ChromaDB + LangChain4j로 관련 질문 검색
- SSE: 스트리밍 응답 (면접 피드백)
- JWT: 사용자 인증

## 개발 우선순위

1. question-service (JD 분석 + 질문 생성)
2. user-service (회원가입/로그인)
3. interview-service (모의 면접)
4. feedback-service (평가/통계)
5. gateway (통합)

## 성능 테스트

```bash
# 모니터링 스택 실행
cd performance/monitoring && docker-compose -f docker-compose.monitoring.yml up -d

# Smoke Test
./performance/scripts/run-smoke.sh

# Load Test (InfluxDB + Grafana)
./performance/scripts/run-load.sh -t load

# 결과 리포트 생성
./performance/scripts/generate-report.sh
```

**대시보드:**
- Grafana: http://localhost:3000 (admin/admin)
- Prometheus: http://localhost:9090
- InfluxDB: http://localhost:8086

**문서:**
- [성능 테스트 가이드](docs/performance/README.md)
- [기준선 정의](docs/performance/BASELINE.md)
- [분석 가이드](docs/performance/ANALYSIS.md)
- [튜닝 기록](docs/performance/TUNING.md)
