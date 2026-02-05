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
- **PostgreSQL**: 주 데이터베이스
- **Redis**: 캐싱, 세션 관리
- **ChromaDB**: 벡터 DB (RAG)

### Frontend
- **Next.js 14.2.35**: App Router 사용
- **TypeScript 5.x**
- **Tailwind CSS 3.4.16**: Neo-brutalist 디자인
- **Zustand 5.0.11**: 상태 관리
- **Axios 1.7.9**: HTTP 클라이언트

### LLM
- **Claude API**: claude-sonnet-4-20250514 (Primary)
- **OpenAI API**: Fallback

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
| question-service | 8082 | JD 분석, 면접 질문 생성, RAG |
| interview-service | 8083 | 모의 면접 세션, Q&A 관리 |
| feedback-service | 8084 | AI 피드백 생성, 통계 분석 |

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
| GET | `/api/jd` | JD 목록 조회 |
| GET | `/api/jd/{id}` | JD 상세 조회 |
| POST | `/api/jd/{id}/analyze` | JD 분석 (스킬 추출) |
| DELETE | `/api/jd/{id}` | JD 삭제 |

### 질문 생성 (question-service)
| Method | Endpoint | 설명 |
|--------|----------|------|
| POST | `/api/questions/generate` | AI 면접 질문 생성 (RAG 적용) |
| GET | `/api/questions` | 생성된 질문 목록 |
| GET | `/api/questions/similar` | 유사 질문 검색 (RAG) |
| GET | `/api/questions/jd/{jdId}/similar` | JD 기반 유사 질문 검색 |
| GET | `/api/questions/rag/status` | RAG 상태 확인 |

### 면접 (interview-service)
| Method | Endpoint | 설명 |
|--------|----------|------|
| POST | `/api/interviews/start` | 면접 세션 시작 |
| POST | `/api/interviews/{id}/answer` | 답변 제출 |
| POST | `/api/interviews/{id}/follow-up` | 꼬리 질문 추가 |
| POST | `/api/interviews/{id}/complete` | 면접 완료 |
| GET | `/api/interviews` | 면접 기록 목록 |
| GET | `/api/interviews/{id}` | 면접 상세 조회 |

### 피드백 (feedback-service)
| Method | Endpoint | 설명 |
|--------|----------|------|
| GET/POST | `/api/feedback/session/{id}/stream` | 피드백 + 꼬리 질문 조회 (SSE) |
| GET | `/api/statistics/me` | 내 통계 조회 |

### 꼬리 질문 (Follow-up Question)
- **생성 조건**: 답변 점수 < 85점, 깊이 < 2
- **피드백 응답에 포함**: `followUpQuestion`, `hasFollowUp` 필드
- **동작 흐름**: 답변 → 피드백 + 꼬리 질문 생성 → 꼬리 질문 답변 → 원본 다음 질문으로 복귀

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
user_statistics (id, user_id, total_interviews, avg_score, strengths, weaknesses)
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
- **동작 흐름**:
  1. 질문 생성 요청 시 ChromaDB에서 유사 질문 검색
  2. 유사 질문을 컨텍스트로 포함하여 LLM 호출 (중복 방지)
  3. 생성된 질문을 ChromaDB에 임베딩 저장
- **Graceful Degradation**: ChromaDB 연결 실패 시 기존 방식으로 동작

### 인증 흐름
1. 로그인 → JWT 발급 (Access + Refresh)
2. 요청 시 Authorization: Bearer {token}
3. Gateway에서 토큰 검증 후 라우팅

### 프론트엔드 상태 관리
- **Zustand stores**:
  - `authStore`: 인증 상태, 토큰 관리
  - `interviewStore`: 면접 세션 상태
- **Axios 인터셉터**: 자동 토큰 첨부, 401 처리

## 프론트엔드 구조

```
frontend/web/
├── src/
│   ├── app/              # Next.js App Router 페이지
│   │   ├── (auth)/       # 로그인/회원가입
│   │   ├── dashboard/    # 대시보드
│   │   ├── jd/           # JD 관리
│   │   ├── interview/    # 면접 진행
│   │   ├── history/      # 면접 기록
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
# 단위 테스트
cd backend && ./gradlew test

# 특정 서비스 테스트
cd backend && ./gradlew :question-service:test
```

### CI/CD
- **GitHub Actions**: `.github/workflows/`
- PR 시 자동 빌드 및 테스트

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

## 관련 문서

- [성능 테스트 가이드](docs/performance/README.md)
- [기준선 정의](docs/performance/BASELINE.md)
- [분석 가이드](docs/performance/ANALYSIS.md)
- [튜닝 기록](docs/performance/TUNING.md)
