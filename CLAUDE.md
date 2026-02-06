# CLAUDE.md

AI 면접 코치 프로젝트 개발 가이드

## 프로젝트 개요

JD(Job Description) 기반 면접 질문 생성 + AI 모의 면접 + 실시간 피드백 시스템

## 프로젝트 구조

```
interview-coach/
├── backend/                    # Spring Boot 마이크로서비스
│   ├── gateway/                # API Gateway (8080)
│   ├── user-service/           # 사용자 인증 (8081)
│   ├── question-service/       # JD 분석, 질문 생성 (8082)
│   ├── interview-service/      # 면접 세션 관리 (8083)
│   └── feedback-service/       # 피드백, 통계 (8084)
├── frontend/web/               # Next.js 14 App Router
├── infra/docker/               # Docker Compose 설정
├── performance/                # k6 성능 테스트
└── docs/                       # 프로젝트 문서
```

## 기술 스택

### Backend
- **Java 21** + **Spring Boot 3.2.0**
- **LangChain4j 0.35.0**: LLM 통합 프레임워크
- **JJWT 0.12.3**: JWT 인증
- **Spring WebFlux**: 비동기 처리, SSE 스트리밍

### Database
- **PostgreSQL**: 주 데이터베이스 (복합 인덱스, `pg_stat_statements` 활성화)
- **Redis**: 캐싱 (`@Cacheable` JD 목록/상세, TTL 5분), 세션 관리
- **ChromaDB**: 벡터 DB (RAG)

### Frontend
- **Next.js 14.2.35**: App Router 사용
- **TypeScript 5.x**
- **Tailwind CSS 3.4.16**: Neo-brutalist 디자인
- **Zustand 5.0.11**: 상태 관리
- **@tanstack/react-query**: 서버 상태 캐싱, API 중복 호출 방지 (전 페이지 표준화)
- **Axios 1.7.9**: HTTP 클라이언트 (토큰 갱신 Mutex 패턴)
- **sonner**: Toast 알림 (Neo-brutalist 스타일)
- **Vitest + Testing Library**: 프론트엔드 테스트 (33개)

### LLM
- **Claude API**: claude-sonnet-4-20250514 (Primary)
- **OpenAI API**: Fallback

### Observability
- **Prometheus**: 메트릭 수집 (JVM, Hibernate, HikariCP)
- **Grafana**: 대시보드 시각화
- **k6**: 부하 테스트 (검색, 동시성, Soak Test)

## 명령어

### Docker Compose (전체 스택 - 권장)

```bash
# 전체 서비스 한 번에 실행 (인프라 + 백엔드 5개 + 프론트엔드)
cd infra/docker
docker-compose up -d --build

# 로그 확인
docker-compose logs -f                    # 전체 로그
docker-compose logs -f question-service   # 특정 서비스 로그

# 서비스 중지
docker-compose down

# 볼륨까지 삭제 (DB 초기화)
docker-compose down -v
```

### 개발 모드 (개별 실행)

```bash
# 인프라만 실행 (PostgreSQL, Redis, ChromaDB)
cd infra/docker && docker-compose up -d postgres redis chromadb

# 백엔드 전체 빌드
cd backend && ./gradlew build

# 백엔드 테스트
cd backend && ./gradlew test

# 개별 서비스 실행
cd backend && ./gradlew :user-service:bootRun
cd backend && ./gradlew :question-service:bootRun
cd backend && ./gradlew :interview-service:bootRun
cd backend && ./gradlew :feedback-service:bootRun
cd backend && ./gradlew :gateway:bootRun

# 프론트엔드 실행
cd frontend/web && npm install
cd frontend/web && npm run dev
```

## 서비스 구조

| 서비스 | 포트 | 역할 |
|--------|------|------|
| gateway | 8080 | API Gateway, 라우팅, 인증 필터 |
| user-service | 8081 | 회원가입/로그인, JWT 발급/검증 |
| question-service | 8082 | JD 분석, 면접 질문 생성, RAG, Redis 캐싱 |
| interview-service | 8083 | 모의 면접 세션, Q&A 관리, 검색, 일시정지/재개 |
| feedback-service | 8084 | AI 피드백 생성, 통계 분석, SSE 스트리밍 |

## API 엔드포인트

### 인증 (user-service)
| Method | Endpoint | 설명 |
|--------|----------|------|
| POST | `/api/auth/signup` | 회원가입 |
| POST | `/api/auth/login` | 로그인, JWT 발급 |
| POST | `/api/auth/refresh` | 토큰 갱신 |
| GET | `/api/users/me` | 내 정보 조회 |

### JD 관리 (question-service)
| Method | Endpoint | 설명 |
|--------|----------|------|
| POST | `/api/jd` | JD 등록 |
| GET | `/api/jd` | JD 목록 조회 (Redis 캐싱) |
| GET | `/api/jd/{id}` | JD 상세 조회 (Redis 캐싱) |
| POST | `/api/jd/{id}/analyze` | JD 분석 (스킬 추출) |
| DELETE | `/api/jd/{id}` | JD 삭제 |

### 질문 생성 (question-service)
| Method | Endpoint | 설명 |
|--------|----------|------|
| POST | `/api/questions/generate` | AI 면접 질문 생성 (RAG, 취약 분야 우선 반영) |
| GET | `/api/questions` | 생성된 질문 목록 |
| GET | `/api/questions/similar` | 유사 질문 검색 (RAG) |
| GET | `/api/questions/jd/{jdId}/similar` | JD 기반 유사 질문 검색 |
| GET | `/api/questions/rag/status` | RAG 상태 확인 |

### 면접 (interview-service)
| Method | Endpoint | 설명 |
|--------|----------|------|
| POST | `/api/interviews` | 면접 세션 시작 |
| POST | `/api/interviews/{id}/answer` | 답변 제출 |
| POST | `/api/interviews/{id}/follow-up` | 꼬리 질문 추가 |
| POST | `/api/interviews/{id}/complete` | 면접 완료 |
| GET | `/api/interviews` | 면접 기록 목록 (Fetch Join) |
| GET | `/api/interviews/{id}` | 면접 상세 조회 (Fetch Join) |
| GET | `/api/interviews/search?keyword=xxx` | 면접 기록 검색 |
| PATCH | `/api/interviews/{id}/pause` | 면접 일시정지 |
| PATCH | `/api/interviews/{id}/resume` | 면접 재개 |

### 피드백 (feedback-service)
| Method | Endpoint | 설명 |
|--------|----------|------|
| GET/POST | `/api/feedback/session/{id}/stream` | 피드백 + 꼬리 질문 조회 (SSE) |
| GET | `/api/statistics` | 내 통계 조회 |
| POST | `/api/statistics/record` | 통계 기록 (비관적 락) |

### 꼬리 질문 (Follow-up Question)
- **생성 조건**: 답변 점수 < 85점, 깊이 < 2
- **피드백 응답에 포함**: `followUpQuestion`, `hasFollowUp` 필드
- **동작 흐름**: 답변 → 피드백 + 꼬리 질문 생성 → 꼬리 질문 답변 → 원본 다음 질문으로 복귀

### 면접 일시정지/재개
- **상태**: `IN_PROGRESS` → `PAUSED` → `IN_PROGRESS`
- **PAUSED 상태에서**: 답변 제출 불가, 재개 또는 완료만 가능
- **History 페이지**: PAUSED 상태 표시, 재개 링크 제공

### 면접 타이머
- 질문당 제한 시간 설정 (3분/5분 선택)
- 프론트엔드 전용: 카운트다운 UI + 시간 초과 경고

### 모범 답안 표시
- 피드백 후 "모범 답안 보기" 토글 버튼
- 기존 `idealAnswer` 필드 활용

### 취약 분야 우선 반영
- **취약 기준**: 70% 미만 점수 카테고리
- **동작 흐름**: JD 페이지 → 질문 생성 설정 모달 → "취약 분야 우선 반영" 체크 → LLM이 취약 분야에서 60% 비중으로 질문 생성
- **API 파라미터**: `weakCategories: [{ category: string, score: number }]`

### 일일 활동 기록
- **테이블**: `daily_activity` (user_id, activity_date, question_count, total_score)
- **시간대**: Asia/Seoul (KST) 기준
- **용도**: 주간 활동 그래프, 학습 통계

## 데이터베이스 스키마

### 주요 테이블
```sql
-- 사용자
users (id, email, password, name, created_at)

-- JD
job_descriptions (id, user_id, company, position, description, skills JSONB)

-- 면접 세션
interview_sessions (id, user_id, jd_id, status, started_at, completed_at)

-- 면접 Q&A
interview_qna (id, session_id, question, answer, feedback JSONB, sequence, parent_qna_id, follow_up_depth, is_follow_up)

-- 생성된 질문
generated_questions (id, jd_id, question, category, difficulty)

-- 사용자 통계
user_statistics (id, user_id, skill_category, total_questions, correct_answers, total_score)

-- 일일 활동 기록
daily_activity (id, user_id, activity_date, question_count, total_score, interview_count)
```

### 인덱스
```sql
CREATE INDEX idx_session_user_started ON interview_sessions(user_id, started_at DESC);
CREATE INDEX idx_qna_session_order ON interview_qna(session_id, question_order);
CREATE INDEX idx_stats_user_category ON user_statistics(user_id, skill_category);
CREATE INDEX idx_daily_user_date ON daily_activity(user_id, activity_date DESC);
CREATE INDEX idx_questions_jd_type ON generated_questions(jd_id, question_type);
CREATE INDEX idx_qna_question_text ON interview_qna USING gin(to_tsvector('simple', question_text));
CREATE INDEX idx_qna_answer_text ON interview_qna USING gin(to_tsvector('simple', answer_text));
```

## 환경 변수

### Docker Compose (.env)
```bash
# infra/docker/.env 파일 생성
CLAUDE_API_KEY=sk-ant-xxxxx    # Claude API 키 (선택, 없으면 Mock 모드)
OPENAI_API_KEY=sk-xxxxx        # OpenAI API 키 (선택, Fallback용)
JWT_SECRET=your-256-bit-secret-key
```

### Backend (개발 모드 - application.yml)
```bash
# Database
DATABASE_URL=jdbc:postgresql://localhost:5432/interview_coach
DATABASE_USERNAME=postgres
DATABASE_PASSWORD=postgres

# JWT
JWT_SECRET=your-256-bit-secret-key
JWT_EXPIRATION=86400000

# LLM API
ANTHROPIC_API_KEY=sk-ant-xxxxx

# Redis
REDIS_HOST=localhost
REDIS_PORT=6379

# ChromaDB (RAG)
CHROMA_HOST=localhost
CHROMA_PORT=8000
```

### Frontend (.env.local)
```bash
NEXT_PUBLIC_API_URL=http://localhost:8080
```

## 아키텍처 패턴

### Clean Architecture
각 서비스는 레이어 분리:
```
service/
├── domain/          # 엔티티, 도메인 로직
├── application/     # 유스케이스, 서비스
├── infrastructure/  # JPA, 외부 API 클라이언트
└── presentation/    # REST Controller, DTO
```

### LLM 통합
- **LangChain4j** + **Claude API** 사용
- 프롬프트 템플릿: 시스템 프롬프트 + 사용자 입력
- 스트리밍: SSE를 통한 실시간 응답

### RAG 파이프라인
- **임베딩 모델**: AllMiniLmL6V2 (로컬, API 비용 없음)
- **벡터 저장소**: ChromaDB
- **배치 처리**: `embedAll()` API로 20개 질문 일괄 임베딩 (2.0s → 0.4s)
- **동작 흐름**:
  1. 질문 생성 요청 시 ChromaDB에서 유사 질문 검색
  2. 유사 질문을 컨텍스트로 포함하여 LLM 호출 (중복 방지)
  3. 생성된 질문을 ChromaDB에 배치 임베딩 저장
- **Graceful Degradation**: ChromaDB 연결 실패 시 기존 방식으로 동작

### 인증 흐름
1. 로그인 → JWT 발급 (Access + Refresh)
2. 요청 시 Authorization: Bearer {token}
3. Gateway에서 토큰 검증 후 라우팅

### 프론트엔드 상태 관리
- **Zustand stores**:
  - `authStore`: 인증 상태, 토큰 관리
  - `interviewStore`: 면접 세션 상태
- **React Query**: 서버 데이터 캐싱 (`staleTime` 설정, 중복 요청 제거)
- **Axios 인터셉터**: 자동 토큰 첨부, 401 처리 (Mutex 패턴으로 동시 갱신 방지)
- **Toast 알림**: sonner 라이브러리, 모든 CUD 액션에 성공/실패 피드백
- **스켈레톤 로딩**: 대시보드, 히스토리, 통계 페이지

## 프론트엔드 구조

```
frontend/web/
├── src/
│   ├── app/              # Next.js App Router 페이지
│   │   ├── (auth)/       # 로그인/회원가입
│   │   ├── dashboard/    # 대시보드 (React Query)
│   │   ├── jd/           # JD 관리
│   │   ├── interview/    # 면접 진행 (타이머, 모범답안, 일시정지)
│   │   ├── history/      # 면접 기록 (검색, 재개)
│   │   └── statistics/   # 통계
│   ├── components/       # 재사용 컴포넌트
│   ├── lib/              # API 클라이언트, 유틸리티
│   ├── stores/           # Zustand 스토어
│   └── types/            # TypeScript 타입 정의
```

### 디자인 시스템
- **Neo-brutalist** 스타일
- 굵은 테두리 (border-4 black)
- 그림자 효과 (shadow-[4px_4px_0_0_black])
- 원색 계열 색상

## 개발 워크플로우

### 전체 서비스 실행 순서

**Docker Compose (권장)**:
```bash
cd infra/docker && docker-compose up -d --build
# 접속: http://localhost:3000
```

**개발 모드**:
1. 인프라: `docker-compose up -d postgres redis chromadb`
2. 백엔드: 각 서비스 순차 실행 (user → question → interview → feedback → gateway)
3. 프론트엔드: `npm run dev`

### 테스트
```bash
# 백엔드 단위 테스트
cd backend && ./gradlew test

# 특정 서비스 테스트
cd backend && ./gradlew :question-service:test

# 프론트엔드 테스트
cd frontend/web && npm test        # watch 모드
cd frontend/web && npm run test:run # 단일 실행
```

### CI/CD
- **GitHub Actions**: `.github/workflows/`
- PR 시 자동 빌드 및 테스트

## 성능 최적화

### 적용된 최적화
| 항목 | 기법 | 효과 |
|------|------|------|
| N+1 쿼리 | `LEFT JOIN FETCH` | 11 SQL → 1 SQL |
| DB 인덱스 | 7개 복합/GIN 인덱스 | Full Scan → Index Scan |
| Race Condition | `@Lock(PESSIMISTIC_WRITE)` | 데이터 정합성 100% |
| Redis 캐싱 | `@Cacheable` (TTL 5분) | DB 쿼리 90% 감소 |
| SSE 스레드 풀 | 전용 `ThreadPoolTaskExecutor` (core:50, max:100) | 타임아웃 50% → 0% |
| SSE 메모리 누수 | TTL 기반 정리 + max 5,000 제한 | 힙 안정화 |
| JVM/GC | ZGC + 서비스별 힙 사이징 | GC Pause 2s → 5ms |
| HikariCP | 서비스별 풀 차등 할당 | Connection Wait 감소 |
| 프론트엔드 | React Query 캐싱 (전 페이지 표준화) | API 요청 70% 감소 |
| 임베딩 | `embedAll()` 배치 처리 | 2.0s → 0.4s |
| 입력 검증 | `@Validated` + `@Positive`/`@Size` | 잘못된 입력 컨트롤러 단 차단 |
| 토큰 갱신 | Mutex 패턴 (isRefreshing + failedQueue) | Race condition 제거 |
| UX 피드백 | sonner Toast + Skeleton 로딩 | 사용자 체감 품질 향상 |

### JVM 설정 (Dockerfile)
- **user/interview/gateway**: `-Xms128m -Xmx256m -XX:+UseZGC`
- **question/feedback**: `-Xms256m -Xmx512m -XX:+UseZGC`
- GC 로그: `-Xlog:gc*:file=/tmp/gc.log`

### HikariCP 풀 사이징
| 서비스 | max-pool | min-idle |
|--------|----------|----------|
| user-service | 10 | 3 |
| question-service | 15 | 5 |
| interview-service | 15 | 5 |
| feedback-service | 20 | 5 |
| gateway | 10 | 3 |

## 성능 테스트

```bash
# 모니터링 스택 실행
cd performance/monitoring && docker-compose -f docker-compose.monitoring.yml up -d

# Smoke Test
./performance/scripts/run-smoke.sh

# Load Test (InfluxDB + Grafana)
./performance/scripts/run-load.sh -t load

# k6 시나리오별 실행
k6 run performance/k6/scenarios/search-load-test.js       # 검색 API 부하
k6 run performance/k6/scenarios/concurrent-answer-test.js  # Race condition 검증
k6 run performance/k6/scenarios/soak-test.js               # 장시간 메모리/GC 테스트

# 결과 리포트 생성
./performance/scripts/generate-report.sh
```

**대시보드:**
- Grafana: http://localhost:3000 (admin/admin)
- Prometheus: http://localhost:9090
- InfluxDB: http://localhost:8086

## Kubernetes 배포

### 구조
```
infra/k8s/
├── base/           # 공통 매니페스트 (Deployment, Service, ConfigMap, Secret, Ingress)
├── overlays/
│   ├── dev/        # 리소스 축소, replicas=1
│   └── prod/       # HPA, replicas=2+, 리소스 확장
└── scripts/
    └── deploy-local.sh  # minikube 자동 배포
```

### 배포 명령어

```bash
# 로컬 (minikube)
./infra/k8s/scripts/deploy-local.sh

# Dev 환경
kubectl apply -k infra/k8s/overlays/dev

# Prod 환경
kubectl apply -k infra/k8s/overlays/prod

# 상태 확인
kubectl get pods -n interview-coach
```

### CI/CD 파이프라인

| 워크플로우 | 트리거 | 동작 |
|-----------|--------|------|
| `ci.yml` | PR, push to main | 백엔드 빌드/테스트 + 프론트엔드 린트/빌드 |
| `deploy.yml` | push to main | ghcr.io 이미지 빌드/푸시 + K8s 배포 |

### HPA (prod)
- gateway, question-service, feedback-service: CPU 70% → 2~5 replicas

### Network Policy
- gateway만 백엔드 서비스 접근 가능 (zero-trust)

## 관련 문서

- [K8s 배포 가이드](infra/k8s/README.md)
- [성능 테스트 가이드](docs/performance/README.md)
- [기준선 정의](docs/performance/BASELINE.md)
- [분석 가이드](docs/performance/ANALYSIS.md)
- [튜닝 기록](docs/performance/TUNING.md)
