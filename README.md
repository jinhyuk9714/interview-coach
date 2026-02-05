# AI 면접 코치 (Interview Coach)

JD 기반 맞춤 질문 생성 + AI 모의 면접 + 실시간 피드백 시스템

## 프로젝트 소개

취업 준비생을 위한 AI 면접 코칭 서비스입니다. 채용공고(JD)를 분석하여 예상 질문을 생성하고, AI 면접관과 모의 면접을 진행하며, 답변에 대한 즉각적인 피드백을 제공합니다.

### 왜 만들었나?

- 면접 준비 시 "어떤 질문이 나올지" 예측하기 어려움
- 혼자 연습하면 객관적인 피드백을 받기 힘듦
- 기술 면접 질문에 대한 체계적인 학습 필요

## 주요 기능

| 기능 | 설명 |
|------|------|
| **JD 분석** | 채용공고 URL/텍스트 → 핵심 역량, 기술 스택 자동 추출 |
| **질문 생성** | JD + 직무 유형 기반 맞춤 면접 질문 생성 (LLM) |
| **모의 면접** | AI 면접관과 실시간 대화형 면접 진행 |
| **답변 피드백** | STAR 기법, 기술 정확도, 개선점 분석 (SSE 스트리밍) |
| **면접 기록** | 과거 면접 이력 조회, 상세 리포트 확인 |
| **학습 통계** | 취약 분야 추적, 카테고리별 정답률, 성장 추이 |

## 아키텍처

```mermaid
flowchart TB
    subgraph Client
        Web[Next.js Frontend]
    end

    subgraph Gateway
        GW[Spring Cloud Gateway :8080]
    end

    subgraph Services
        US[User Service :8081]
        QS[Question Service :8082]
        IS[Interview Service :8083]
        FS[Feedback Service :8084]
    end

    subgraph Data
        PG[(PostgreSQL :5432)]
        RD[(Redis :6379)]
        CH[(ChromaDB :8000)]
    end

    subgraph AI
        LC[LangChain4j]
        LLM[Claude API]
    end

    Web --> GW
    GW --> US & IS & QS & FS
    US & IS & FS --> PG
    IS --> RD
    QS --> CH
    QS --> LC
    LC --> LLM
```

## 기술 스택

### Backend
- Java 21
- Spring Boot 3.2
- Spring Security + JWT
- Spring Data JPA
- Spring Cloud Gateway
- LangChain4j

### Database
- PostgreSQL 16 - 메인 데이터
- Redis 7 - 세션, 캐싱
- ChromaDB - 벡터 임베딩 저장 (RAG)

### AI/LLM
- Claude API (claude-sonnet-4-20250514) - Primary
- OpenAI API (Fallback 구성 가능)
- AllMiniLmL6V2 - 로컬 임베딩 모델 (RAG)

### Frontend
- Next.js 14 (App Router)
- TypeScript
- Tailwind CSS
- Zustand (상태 관리)
- Framer Motion (애니메이션)

### Infra & DevOps
- Docker / Docker Compose
- GitHub Actions (CI/CD)
- k6 (성능 테스트)
- Prometheus + Grafana + InfluxDB (모니터링)

## 서비스 구조

| 서비스 | 포트 | 역할 |
|--------|------|------|
| gateway | 8080 | API Gateway, JWT 검증, 라우팅 |
| user-service | 8081 | 회원가입/로그인, JWT 발급 |
| question-service | 8082 | JD 분석, 질문 생성 (LLM) |
| interview-service | 8083 | 모의 면접 세션 관리 |
| feedback-service | 8084 | 답변 평가, SSE 피드백, 통계 |

## 프로젝트 구조

```
interview-coach/
├── backend/
│   ├── gateway/              # API Gateway
│   ├── user-service/         # 회원 관리, JWT 인증
│   ├── question-service/     # JD 분석, 질문 생성 (LangChain4j)
│   ├── interview-service/    # 면접 세션 관리
│   └── feedback-service/     # 답변 평가, SSE 스트리밍, 통계
├── frontend/web/
│   └── src/app/
│       ├── (auth)/           # 로그인, 회원가입
│       └── (main)/
│           ├── dashboard/    # 대시보드
│           ├── jd/           # JD 관리
│           ├── interview/    # 면접 진행
│           ├── history/      # 면접 기록
│           ├── statistics/   # 학습 통계
│           └── profile/      # 프로필
├── infra/
│   └── docker/               # Docker Compose 설정
├── performance/
│   ├── k6/                   # 성능 테스트 시나리오
│   ├── monitoring/           # Grafana, Prometheus, InfluxDB
│   └── scripts/              # 테스트 실행 스크립트
└── docs/
    └── performance/          # 성능 테스트 문서
```

## 시작하기

### 요구사항

- Java 21+
- Docker & Docker Compose
- (선택) Claude API Key

### 환경 변수

```bash
# .env (선택 - 없으면 Mock 모드로 동작)
CLAUDE_API_KEY=your-api-key
```

### 실행 (Docker Compose - 권장)

```bash
# 전체 스택 한 번에 실행 (인프라 + 백엔드 + 프론트엔드)
cd infra/docker
cp .env.example .env  # API 키 설정 (선택)
docker-compose up -d --build

# 접속
# - 프론트엔드: http://localhost:3000
# - API Gateway: http://localhost:8080
```

### 실행 (개발 모드)

```bash
# 1. 인프라만 실행 (PostgreSQL, Redis, ChromaDB)
cd infra/docker && docker-compose up -d postgres redis chromadb

# 2. 백엔드 빌드 및 실행
cd backend && ./gradlew build

./gradlew :gateway:bootRun           # 포트 8080
./gradlew :user-service:bootRun      # 포트 8081
./gradlew :question-service:bootRun  # 포트 8082
./gradlew :interview-service:bootRun # 포트 8083
./gradlew :feedback-service:bootRun  # 포트 8084

# 3. 프론트엔드 실행
cd frontend/web && npm install && npm run dev  # 포트 3000
```

### 테스트

```bash
# 백엔드 단위 테스트
cd backend && ./gradlew test

# 특정 서비스 테스트
./gradlew :question-service:test
./gradlew :feedback-service:test
./gradlew :interview-service:test
```

## API 명세

### 인증 API (user-service)

```http
# 회원가입
POST /api/v1/auth/signup
Content-Type: application/json

{
  "email": "user@example.com",
  "password": "password123",
  "nickname": "닉네임"
}

# 로그인
POST /api/v1/auth/login
Content-Type: application/json

{
  "email": "user@example.com",
  "password": "password123"
}

# Response
{
  "accessToken": "eyJhbGc...",
  "refreshToken": "eyJhbGc...",
  "tokenType": "Bearer",
  "expiresIn": 3600
}
```

### JD 분석 API (question-service)

```http
# JD 등록
POST /api/v1/jd
X-User-Id: 1
Content-Type: application/json

{
  "companyName": "카카오",
  "position": "백엔드 개발자",
  "originalText": "Java, Spring Boot 경력 3년 이상..."
}

# JD 분석
POST /api/v1/jd/{id}/analyze

# Response
{
  "jdId": 1,
  "skills": ["Java", "Spring Boot", "JPA"],
  "requirements": ["3년 이상 경력", "MSA 경험"],
  "summary": "백엔드 개발자 포지션..."
}

# 질문 생성
POST /api/v1/questions/generate
X-User-Id: 1
Content-Type: application/json

{
  "jdId": 1,
  "questionType": "mixed",
  "count": 5,
  "difficulty": 3
}
```

### 면접 API (interview-service)

```http
# 면접 시작
POST /api/v1/interviews
X-User-Id: 1
Content-Type: application/json

{
  "jdId": 1,
  "questionIds": [1, 2, 3],
  "interviewType": "PRACTICE"
}

# 답변 제출
POST /api/v1/interviews/{id}/answer
Content-Type: application/json

{
  "qnaId": 1,
  "answer": "저는 SAGA 패턴을 사용해서..."
}

# 면접 완료
POST /api/v1/interviews/{id}/complete
```

### 피드백 API (feedback-service)

```http
# SSE 피드백 스트림
GET /api/v1/feedback/session/{sessionId}/stream

# 통계 조회
GET /api/v1/statistics
X-User-Id: 1

# Response
{
  "userId": 1,
  "totalQuestions": 50,
  "totalCorrect": 35,
  "overallCorrectRate": 70.0,
  "byCategory": [
    {"category": "Java", "totalQuestions": 20, "correctRate": 80.0}
  ]
}
```

## 성능 테스트

```bash
# 모니터링 스택 실행
cd performance/monitoring
docker-compose -f docker-compose.monitoring.yml up -d

# Smoke Test (기본 동작 확인)
./performance/scripts/run-smoke.sh

# Load Test (부하 테스트)
./performance/scripts/run-load.sh -t load

# 결과 리포트 생성
./performance/scripts/generate-report.sh
```

**대시보드:**
- Grafana: http://localhost:3000 (admin/admin)
- Prometheus: http://localhost:9090
- InfluxDB: http://localhost:8086

## 데이터 모델

```mermaid
erDiagram
    users ||--o{ job_descriptions : creates
    users ||--o{ interview_sessions : has
    job_descriptions ||--o{ generated_questions : has
    job_descriptions ||--o{ interview_sessions : based_on
    interview_sessions ||--o{ interview_qna : contains
    users ||--o{ user_statistics : tracks

    users {
        bigint id PK
        varchar email UK
        varchar password_hash
        varchar nickname
        varchar target_position
        int experience_years
    }

    job_descriptions {
        bigint id PK
        bigint user_id FK
        varchar company_name
        varchar position
        text original_text
        text[] parsed_skills
        text[] parsed_requirements
    }

    generated_questions {
        bigint id PK
        bigint jd_id FK
        varchar question_type
        varchar skill_category
        text question_text
        text hint
        int difficulty
    }

    interview_sessions {
        bigint id PK
        bigint user_id FK
        bigint jd_id FK
        varchar interview_type
        varchar status
        timestamp started_at
        timestamp completed_at
    }

    interview_qna {
        bigint id PK
        bigint session_id FK
        int question_order
        text question_text
        text answer_text
        int score
        text feedback
    }

    user_statistics {
        bigint id PK
        bigint user_id FK
        varchar skill_category
        int total_questions
        int total_correct
        decimal correct_rate
    }
```

## 개발 로드맵

- [x] 프로젝트 설계
- [x] **Phase 1: MVP 백엔드**
  - [x] 회원가입/로그인 (JWT)
  - [x] JD 텍스트 분석 → 질문 생성 (LLM)
  - [x] 모의 면접 세션 관리
  - [x] SSE 피드백 스트리밍
  - [x] 학습 통계
  - [x] API Gateway
  - [x] 성능 테스트 인프라 (k6, Grafana)
- [x] **Phase 2: 프론트엔드**
  - [x] Next.js 14 UI 구현
  - [x] 면접 진행 화면 (실시간 피드백)
  - [x] 통계 대시보드 (카테고리별 점수, 성장 추이)
  - [x] 면접 기록 페이지 (히스토리, 상세 리포트)
  - [x] 프로필 페이지
- [x] **Phase 3: 테스트 & 안정화**
  - [x] 단위 테스트 (JUnit 5, Mockito)
  - [x] SSE 스트리밍 안정화
  - [x] 버그 수정 및 UX 개선
- [x] **Phase 4: 고도화**
  - [x] RAG 파이프라인 (ChromaDB + LangChain4j)
  - [x] Docker Compose 전체 스택 (한 번에 실행)
  - [ ] 꼬리 질문 기능
  - [ ] 취약점 분석 & 추천
- [ ] **Phase 5: 배포**
  - [ ] Kubernetes 배포
  - [ ] CI/CD 파이프라인 완성

## 기술적 도전

| 도전 | 해결 방법 |
|------|----------|
| JD에서 핵심 정보 추출 | LLM 프롬프트 엔지니어링 + JSON 구조화 출력 |
| 실시간 피드백 | SSE (Server-Sent Events) 스트리밍 |
| 마이크로서비스 인증 | Gateway에서 JWT 검증 → X-User-Id 헤더 전달 |
| LLM 비용 최적화 | Mock 모드 지원, 캐싱 (추후 Redis) |
| 성능 측정 | k6 + InfluxDB + Grafana 대시보드 |
| 질문 중복 방지 | RAG (ChromaDB + 로컬 임베딩 모델) |
| 컨테이너 시간대 | UTC 날짜 파싱 유틸리티로 KST 변환 |
| 새로고침 로그아웃 | Zustand hydration 상태 추적 |

## 라이선스

MIT License
