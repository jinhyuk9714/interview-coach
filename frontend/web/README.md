# Interview Coach - Frontend

AI 면접 코치 프론트엔드 애플리케이션

## 기술 스택

- **Next.js 14** (App Router)
- **TypeScript 5.x**
- **Tailwind CSS** (Neo-brutalist 디자인)
- **Zustand** (클라이언트 상태 관리)
- **@tanstack/react-query** (서버 상태 캐싱)
- **Axios** (HTTP 클라이언트, 토큰 자동 갱신)

## 시작하기

```bash
# 의존성 설치
npm install

# 개발 서버 실행
npm run dev

# 프로덕션 빌드
npm run build
npm start
```

http://localhost:3000 접속

## 환경 변수

```bash
# .env.local
NEXT_PUBLIC_API_URL=http://localhost:8080
```

## 페이지 구조

| 경로 | 설명 |
|------|------|
| `/login` | 로그인 |
| `/signup` | 회원가입 |
| `/dashboard` | 대시보드 (React Query 캐싱) |
| `/jd` | JD 등록/관리, 질문 생성 |
| `/interview` | 모의 면접 진행 (타이머, 모범답안, 일시정지) |
| `/history` | 면접 기록 목록 (검색, 재개) |
| `/statistics` | 학습 통계 |
| `/profile` | 프로필 설정 |

## 주요 기능

- **JD 기반 질문 생성**: JD 등록 → 스킬 분석 → AI 면접 질문 생성 (취약 분야 우선 반영)
- **모의 면접**: 실시간 AI 피드백 (SSE 스트리밍), 꼬리 질문 자동 생성
- **면접 타이머**: 질문당 3분/5분 카운트다운
- **모범 답안**: 피드백 후 토글로 확인
- **일시정지/재개**: 면접 중단 후 나중에 이어서 진행
- **기록 검색**: 키워드로 면접 기록 검색 (디바운스 적용)
- **React Query**: 대시보드 API 캐싱, 중복 요청 방지

## 디자인 시스템

Neo-brutalist 스타일:
- 굵은 테두리 (`border-4 border-black`)
- 그림자 효과 (`shadow-[4px_4px_0_0_black]`)
- 원색 계열 색상
