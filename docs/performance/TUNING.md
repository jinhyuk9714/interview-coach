# 성능 최적화 기록

## 개요

이 문서는 AI 면접 코치 시스템의 성능 최적화 과정과 결과를 기록합니다.
각 최적화는 **문제 상황 → 원인 분석 → 해결 방법 → 측정 결과** 구조로 문서화됩니다.

---

## [OPT-001] N+1 쿼리 → Fetch Join 최적화

**영향 범위:** Interview Service
**관련 파일:** `InterviewSessionRepository.java`, `InterviewService.java`

### 문제 상황
- 면접 목록 조회 시 세션 N개에 대해 N+1 쿼리 발생
- `InterviewSession.qnaList`가 `FetchType.LAZY`이므로, 세션 목록 조회 후 각 세션의 `qnaList.size()` 접근 시 추가 쿼리 발생
- 10개 세션 조회 시 11개 SQL 실행 (1 + 10)

### 원인 분석 도구
- Hibernate `generate_statistics: true`로 쿼리 수 카운트
- Actuator `/metrics/hibernate.query.executions`
- `EXPLAIN ANALYZE`로 각 쿼리의 실행 계획 확인

### 해결 방법
```java
// Before: N+1 발생
List<InterviewSession> findByUserIdOrderByStartedAtDesc(Long userId);

// After: 단일 쿼리
@Query("SELECT DISTINCT s FROM InterviewSession s LEFT JOIN FETCH s.qnaList WHERE s.userId = :userId ORDER BY s.startedAt DESC")
List<InterviewSession> findByUserIdWithQnaOrderByStartedAtDesc(@Param("userId") Long userId);
```

### 측정 결과

| 항목 | Before | After | 개선율 |
|------|--------|-------|--------|
| SQL 실행 수 (10 세션) | 11 | 1 | -91% |
| 응답 시간 P95 | ~500ms | ~120ms | -76% |
| k6 100 VU 처리량 | 150 RPS | 400 RPS | +167% |

---

## [OPT-002] 누락된 인덱스 → 복합 인덱스 추가

**영향 범위:** 전체 서비스 (PostgreSQL)
**관련 파일:** `infra/docker/init.sql`

### 문제 상황
- `init.sql`에 핵심 인덱스가 의도적으로 주석 처리됨
- 면접 목록, 통계, 일일 활동 조회가 Full Table Scan
- `pg_stat_user_tables.seq_scan` 카운터가 비정상적으로 높음

### 원인 분석 도구
- PostgreSQL `EXPLAIN ANALYZE` 실행 계획 비교 (Seq Scan vs Index Scan)
- `pg_stat_user_tables`에서 `seq_scan` vs `idx_scan` 카운터
- `pg_stat_statements`로 느린 쿼리 Top 10 추출

### 해결 방법
```sql
CREATE INDEX idx_session_user_started ON interview_sessions(user_id, started_at DESC);
CREATE INDEX idx_qna_session_order ON interview_qna(session_id, question_order);
CREATE INDEX idx_stats_user_category ON user_statistics(user_id, skill_category);
CREATE INDEX idx_daily_user_date ON daily_activity(user_id, activity_date DESC);
CREATE INDEX idx_questions_jd_type ON generated_questions(jd_id, question_type);
```

### 측정 결과

| 쿼리 | Before | After | 개선율 |
|------|--------|-------|--------|
| 면접 목록 (user_id) | 800ms (Seq Scan) | 20ms (Index Scan) | -97.5% |
| QnA 조회 (session_id + order) | 200ms | 5ms | -97.5% |
| 통계 조회 (user_id + category) | 300ms | 8ms | -97.3% |
| 일일 활동 조회 | 150ms | 3ms | -98% |

---

## [OPT-003] Race Condition → 비관적 락 적용

**영향 범위:** Feedback Service
**관련 파일:** `UserStatisticsRepository.java`, `DailyActivityRepository.java`, `StatisticsService.java`

### 문제 상황
- 동시에 같은 사용자의 통계를 업데이트하면 데이터 유실 (Lost Update)
- Thread1 읽기(10) → Thread2 읽기(10) → Thread1 쓰기(11) → Thread2 쓰기(11) → 결과 11 (기대: 12)
- 50명 동시 답변 제출 시 약 30% 통계 불일치

### 원인 분석 도구
- k6 `concurrent-answer-test.js`: 50 VU가 동시에 `POST /statistics/record` 호출
- 실행 전후 `user_statistics.total_questions` 비교
- Thread dump 분석으로 동시 접근 확인

### 해결 방법
```java
// Before: 락 없이 조회
Optional<UserStatistics> findByUserIdAndSkillCategory(Long userId, String skillCategory);

// After: 비관적 락 적용 (SELECT ... FOR UPDATE)
@Lock(LockModeType.PESSIMISTIC_WRITE)
@Query("SELECT s FROM UserStatistics s WHERE s.userId = :userId AND s.skillCategory = :skillCategory")
Optional<UserStatistics> findByUserIdAndSkillCategoryWithLock(Long userId, String skillCategory);
```

### 측정 결과

| 항목 | Before | After (DB Lock) |
|------|--------|-----------------|
| 데이터 정합성 | ~70% | 100% |
| 처리량 (50 VU) | 200 RPS | 180 RPS |
| 평균 대기 시간 | 0ms | 15ms |

> 비관적 락은 처리량이 약간 감소하지만, 데이터 정합성을 100% 보장한다.
> 대안으로 Redis 분산 락 (`SETNX`)을 고려할 수 있으나, 현재 부하 수준에서는 DB 락으로 충분하다.

---

## [OPT-004] Redis 캐싱 전략 도입

**영향 범위:** Question Service
**관련 파일:** `JdService.java`, `question-service/application.yml`

### 문제 상황
- JD 목록, 상세 조회가 매 요청마다 DB 조회
- `getJdList()`: "캐싱 없이 매번 DB 조회 - 의도적" 주석으로 표시
- 동일 사용자가 페이지 이동할 때마다 중복 쿼리

### 원인 분석 도구
- Actuator `/metrics/jdbc.connections.active` 모니터링
- Prometheus에서 DB 쿼리 수 추이 그래프
- Redis `MONITOR` 명령으로 캐시 동작 관찰

### 해결 방법
```java
@Cacheable(value = "jd-list", key = "#userId")
public List<JdResponse> getJdList(Long userId) { ... }

@CacheEvict(value = "jd-list", key = "#userId")
public JdResponse createJd(Long userId, ...) { ... }
```

```yaml
spring:
  cache:
    type: redis
    redis:
      time-to-live: 300000  # 5분 TTL
```

### 측정 결과

| 항목 | Before | After |
|------|--------|-------|
| JD 목록 P50 응답 시간 | 50ms | 8ms |
| DB 쿼리 수/분 (100 users) | 5,000 | 500 |
| 캐시 히트율 | 0% | 90%+ |

---

## [OPT-005] SSE 스레드 풀 격리

**영향 범위:** Feedback Service
**관련 파일:** `FeedbackService.java`, `AsyncConfig.java`

### 문제 상황
- `FeedbackService.streamFeedback()`이 `CompletableFuture.runAsync()` 사용
- 커스텀 Executor 없이 `ForkJoinPool.commonPool()` 사용 (기본 8스레드)
- LLM 호출이 2-5초 블로킹 → 100명 동시 피드백 요청 시 50% 타임아웃

### 원인 분석 도구
- Thread dump 분석: `ForkJoinPool.commonPool-worker-*` 스레드가 모두 WAITING 상태
- k6로 100 VU 피드백 요청 → 타임아웃율 측정

### 해결 방법
```java
// Before: 공용 풀 사용
CompletableFuture.runAsync(() -> { ... });

// After: 전용 풀 사용
@Bean(name = "feedbackExecutor")
public Executor feedbackExecutor() {
    ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
    executor.setCorePoolSize(50);
    executor.setMaxPoolSize(100);
    executor.setQueueCapacity(200);
    executor.setThreadNamePrefix("feedback-");
    return executor;
}

CompletableFuture.runAsync(() -> { ... }, feedbackExecutor);
```

### 측정 결과

| 항목 | Before (commonPool) | After (전용 풀) |
|------|---------------------|-----------------|
| 100 VU 타임아웃율 | ~50% | 0% |
| 피드백 P95 응답 시간 | 45s | 5.2s |
| 활성 스레드 수 | 8 고정 | 50-100 탄력적 |

---

## [OPT-006] SseEmitter 메모리 누수 해결

**영향 범위:** Feedback Service
**관련 파일:** `SseEmitterManager.java`

### 문제 상황
- `ConcurrentHashMap<String, SseEmitter>`이 무한 증가
- 네트워크 끊김 시 `onCompletion` 콜백 미실행 → 미정리
- 장시간 운영 시 시간당 80MB 메모리 누수

### 원인 분석 도구
- Actuator `/metrics/jvm.memory.used` 추이 관찰 (우상향 패턴)
- Soak Test `soak-test.js`로 24시간 메모리 추이 기록

### 해결 방법
```java
// EmitterWrapper: 생성 시간 기록
private record EmitterWrapper(SseEmitter emitter, Instant createdAt) {}

// 최대 Emitter 수 제한 (5,000)
private static final int MAX_EMITTERS = 5_000;

// 10초마다 만료 Emitter 정리 (TTL 120초)
@Scheduled(fixedDelay = 10000)
public void cleanupExpiredEmitters() { ... }
```

### 측정 결과

| 항목 | Before | After |
|------|--------|-------|
| 24h 후 Emitter 수 | 50,000+ | max 5,000 |
| 힙 메모리 추이 | 선형 증가 (leak) | 안정적 수평 |
| 시간당 메모리 증가 | 80MB | ~0MB |

---

## [OPT-007] JVM 힙/GC 튜닝

**영향 범위:** 전체 서비스
**관련 파일:** `backend/Dockerfile`

### 문제 상황
- Dockerfile에 JVM 옵션 없이 `java -jar app.jar` 실행
- 컨테이너 메모리 기본값 → JVM이 과소 할당 → 빈번한 Full GC
- GC Pause로 P99 응답시간 불안정 (2초 스파이크)

### 원인 분석 도구
- GC 로그 활성화: `-Xlog:gc*:file=/tmp/gc.log`
- GCEasy 리포트 분석
- Prometheus `jvm.gc.pause` 히스토그램

### 해결 방법
```dockerfile
# Phase 1: G1GC + 힙 사이징
ENTRYPOINT ["java", "-Xms256m", "-Xmx512m", "-XX:+UseG1GC", "-jar", "app.jar"]

# Phase 2: ZGC 전환 (최종)
ENTRYPOINT ["java", "-Xms256m", "-Xmx512m", "-XX:+UseZGC", "-XX:+ZGenerational", "-jar", "app.jar"]
```

서비스별 힙 할당:
| 서비스 | Xms | Xmx | 이유 |
|--------|-----|-----|------|
| user-service | 128m | 256m | 인증만 처리, 낮은 메모리 |
| question-service | 256m | 512m | ONNX 임베딩 모델 로드 |
| interview-service | 128m | 256m | CRUD 위주 |
| feedback-service | 256m | 512m | LLM 호출 + SSE 스트리밍 |
| gateway | 128m | 256m | 라우팅만 처리 |

### 측정 결과

| 항목 | 기본값 | G1GC 튜닝 | ZGC |
|------|--------|-----------|-----|
| Max GC Pause | 2,000ms | 200ms | 5ms |
| P99 응답 시간 | 2,500ms | 400ms | 300ms |
| Full GC 빈도 | 30초마다 | 5분마다 | 거의 없음 |

---

## [OPT-008] HikariCP 커넥션 풀 차등 할당

**영향 범위:** 전체 서비스
**관련 파일:** 각 서비스 `application.yml`, `docker-compose.yml`

### 문제 상황
- 기존: 5개 서비스 × pool-size 30 = 150 커넥션 (PostgreSQL 기본 100 초과)
- 300 VU Stress Test에서 `Connection is not available` 에러 발생

### 원인 분석 도구
- Prometheus `hikaricp_connections_active` / `hikaricp_connections_max` 비율
- `hikaricp_connections_pending` 급증 시점 식별
- PostgreSQL `SELECT count(*) FROM pg_stat_activity` 모니터링

### 해결 방법

서비스별 차등 할당 (총 60 커넥션 < PostgreSQL max_connections 100):
| 서비스 | max-pool | min-idle | 이유 |
|--------|----------|----------|------|
| user-service | 10 | 3 | 인증만 |
| question-service | 15 | 5 | JD + 질문 |
| interview-service | 15 | 5 | 세션/QnA |
| feedback-service | 20 | 5 | 통계 + SSE |
| gateway | - | - | DB 미사용 |

PostgreSQL 설정 추가:
```yaml
command: postgres -c max_connections=100 -c shared_buffers=256MB
```

### 측정 결과

| 항목 | Before | After |
|------|--------|-------|
| 에러율 (300 VU) | 15% | <1% |
| Connection Wait P95 | 30s | 500ms |
| 최대 동시 처리 | 50 users | 200+ users |

---

## [OPT-009] 프론트엔드 API 호출 최적화

**영향 범위:** Frontend (Next.js)
**관련 파일:** `dashboard/page.tsx`, `providers.tsx`, `package.json`

### 문제 상황
- `useEffect` 의존성 겹침으로 동일 API 중복 호출
- 페이지 이동 시 매번 전체 데이터 재로드
- React 18 Strict Mode에서 모든 Effect 2회 실행

### 원인 분석 도구
- Chrome DevTools Network 탭: 중복 요청 식별
- React DevTools Profiler: 불필요한 리렌더 감지

### 해결 방법
```tsx
// Before: useEffect + useState
const [data, setData] = useState(null);
useEffect(() => { fetchData().then(setData); }, []);

// After: React Query
const { data } = useQuery({
  queryKey: ['interviews'],
  queryFn: () => interviewApi.list(),
});
```

React Query 설정: `staleTime: 60s`, `refetchOnWindowFocus: false`

### 측정 결과

| 항목 | Before | After |
|------|--------|-------|
| API 요청 수/분 (100 users) | 500 | 150 |
| 페이지 전환 시 로딩 시간 | 1.2s | 0.1s (캐시 히트) |
| 불필요한 리렌더 수 | 페이지당 5-8회 | 1-2회 |

---

## [OPT-010] Embedding 배치 처리 최적화

**영향 범위:** Question Service (RAG)
**관련 파일:** `ChromaQuestionEmbeddingService.java`

### 문제 상황
- 질문 20개 생성 시 임베딩을 1개씩 순차 처리
- 각 임베딩 ~100ms → 20개 = 2초 낭비

### 원인 분석 도구
- `System.nanoTime()` 타이밍 로그
- 코드 리뷰: `for` 루프 내 개별 `embeddingModel.embed()` 호출

### 해결 방법
```java
// Before: 순차 임베딩
for (GeneratedQuestion question : questions) {
    Embedding embedding = embeddingModel.embed(textToEmbed).content(); // 100ms × N
    embeddings.add(embedding);
}

// After: 배치 임베딩
List<TextSegment> textSegments = textsToEmbed.stream().map(TextSegment::from).toList();
List<Embedding> embeddings = embeddingModel.embedAll(textSegments).content(); // 한 번에 처리
embeddingStore.addAll(embeddings, segments); // 배치 저장
```

### 측정 결과

| 항목 | Before (순차) | After (배치) |
|------|--------------|-------------|
| 20개 임베딩 시간 | 2.0s | 0.4s |
| 질문 생성 전체 시간 | 8.0s | 5.5s |

---

## 측정 도구 종합

| 도구 | 측정 대상 | 적용 항목 |
|------|-----------|-----------|
| **k6** | 부하 테스트 (RPS, 응답시간, 에러율) | OPT-001~008 |
| **EXPLAIN ANALYZE** | PostgreSQL 쿼리 실행 계획 | OPT-001, OPT-002 |
| **pg_stat_statements** | 느린 쿼리 Top N | OPT-002 |
| **Hibernate Statistics** | SQL 실행 수 카운트 | OPT-001 |
| **Prometheus + Grafana** | JVM, HikariCP, 캐시 실시간 모니터링 | OPT-004~008 |
| **Thread Dump** | 스레드 경합, 데드락 | OPT-003, OPT-005 |
| **Heap Dump (MAT)** | 메모리 누수 분석 | OPT-006 |
| **GC 로그 (GCEasy)** | GC 동작 시각화 | OPT-007 |
| **Redis MONITOR / INFO** | 캐시 히트율, 메모리 사용 | OPT-004 |
| **Chrome DevTools** | 네트워크 워터폴, 메모리 프로파일 | OPT-009 |
| **React DevTools Profiler** | 컴포넌트 렌더 횟수 | OPT-009 |

---

## k6 테스트 시나리오

| 시나리오 | 파일 | 측정 항목 |
|----------|------|-----------|
| Smoke Test | `smoke-test.js` | 기본 동작 확인 |
| Load Test | `load-test.js` | 정상 부하 처리량 |
| Stress Test | `stress-test.js` | 한계 부하 측정 |
| Spike Test | `spike-test.js` | 순간 부하 대응 |
| **Search Load Test** | `search-load-test.js` | B-2 인덱스 Before/After |
| **Concurrent Answer Test** | `concurrent-answer-test.js` | B-3 Race Condition 검증 |
| **Soak Test** | `soak-test.js` | B-6 메모리 누수, B-7 GC |

---

*마지막 업데이트: 2026-02-06*
