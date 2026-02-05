# 성능 최적화 기록

## 개요

이 문서는 AI 면접 코치 시스템의 성능 최적화 과정과 결과를 기록합니다.
각 최적화는 Before/After 측정값과 함께 문서화됩니다.

## 최적화 기록

### [OPT-001] HikariCP 커넥션 풀 튜닝

**날짜:** YYYY-MM-DD
**담당자:** [이름]
**영향 범위:** 전체 서비스

#### 문제 상황
- Stress Test 300 VU에서 커넥션 고갈 에러 발생
- `HikariPool-1 - Connection is not available` 에러 15%

#### 변경 내용

**Before:**
```yaml
spring:
  datasource:
    hikari:
      maximum-pool-size: 10
      minimum-idle: 5
      connection-timeout: 30000
```

**After:**
```yaml
spring:
  datasource:
    hikari:
      maximum-pool-size: 30
      minimum-idle: 10
      connection-timeout: 20000
      max-lifetime: 1800000
      idle-timeout: 600000
```

#### 결과

| 메트릭 | Before | After | 개선율 |
|--------|--------|-------|--------|
| 에러율 (300 VU) | 15% | 3% | -80% |
| Connection Wait (P95) | 30s | 500ms | -98% |
| 처리량 | 150 RPS | 280 RPS | +87% |

#### 참고 자료
- [HikariCP 설정 가이드](https://github.com/brettwooldridge/HikariCP#configuration-knobs-baby)
- PostgreSQL `max_connections` 확인 필요

---

### [OPT-002] Redis 캐싱 적용 (질문 목록)

**날짜:** YYYY-MM-DD
**담당자:** [이름]
**영향 범위:** Question Service

#### 문제 상황
- 질문 목록 API가 매 요청마다 DB 조회
- P95 응답 시간 300ms

#### 변경 내용

**Before:**
```java
public Page<Question> getQuestions(Pageable pageable) {
    return questionRepository.findAll(pageable);
}
```

**After:**
```java
@Cacheable(value = "questions", key = "#pageable.pageNumber + '-' + #pageable.pageSize")
public Page<Question> getQuestions(Pageable pageable) {
    return questionRepository.findAll(pageable);
}
```

```yaml
spring:
  cache:
    type: redis
    redis:
      time-to-live: 300000  # 5분
```

#### 결과

| 메트릭 | Before | After | 개선율 |
|--------|--------|-------|--------|
| P95 응답 시간 | 300ms | 50ms | -83% |
| 처리량 | 500 RPS | 2000 RPS | +300% |
| DB 쿼리 수 | 1000/min | 200/min | -80% |

#### 캐시 히트율 모니터링
```promql
# Redis 캐시 히트율
redis_keyspace_hits_total / (redis_keyspace_hits_total + redis_keyspace_misses_total)
```

---

### [OPT-003] JD 분석 응답 스트리밍

**날짜:** YYYY-MM-DD
**담당자:** [이름]
**영향 범위:** Question Service

#### 문제 상황
- JD 분석 API 응답 대기 시간 30초 이상
- 사용자 경험 저하 (긴 로딩)

#### 변경 내용

**Before:**
```java
@PostMapping("/jd/analyze")
public ResponseEntity<JdAnalysisResult> analyzeJd(@RequestBody JdRequest request) {
    return ResponseEntity.ok(llmService.analyze(request));
}
```

**After:**
```java
@PostMapping("/jd/analyze")
public SseEmitter analyzeJdStream(@RequestBody JdRequest request) {
    SseEmitter emitter = new SseEmitter(120000L);
    llmService.analyzeWithStreaming(request, emitter);
    return emitter;
}
```

#### 결과

| 메트릭 | Before | After | 개선율 |
|--------|--------|-------|--------|
| 첫 응답 시간 | 30s | 3s | -90% |
| 체감 대기 시간 | 30s | 3s (점진적 표시) | -90% |
| 타임아웃 에러 | 10% | 2% | -80% |

---

### [OPT-004] 인덱스 최적화

**날짜:** YYYY-MM-DD
**담당자:** [이름]
**영향 범위:** Question Service, Feedback Service

#### 문제 상황
- 질문 검색 API 느림 (Full Table Scan)
- Feedback 통계 쿼리 지연

#### 변경 내용

```sql
-- Question 테이블
CREATE INDEX idx_questions_category ON questions(category);
CREATE INDEX idx_questions_skills ON questions USING gin(skills);
CREATE INDEX idx_questions_created_at ON questions(created_at DESC);

-- Feedback 테이블
CREATE INDEX idx_feedback_user_id ON feedback(user_id);
CREATE INDEX idx_feedback_created_at ON feedback(created_at DESC);
CREATE INDEX idx_feedback_user_category ON feedback(user_id, category);
```

#### 결과

| 쿼리 | Before | After | 개선율 |
|------|--------|-------|--------|
| 카테고리별 검색 | 500ms | 20ms | -96% |
| 스킬 기반 검색 | 800ms | 50ms | -94% |
| 사용자 통계 | 2s | 100ms | -95% |

---

### [OPT-005] GC 튜닝 (G1GC → ZGC)

**날짜:** YYYY-MM-DD
**담당자:** [이름]
**영향 범위:** 전체 서비스

#### 문제 상황
- Soak Test 중 GC Pause로 인한 지연 스파이크
- P99 응답 시간 불안정

#### 변경 내용

**Before (G1GC):**
```bash
java -Xmx512m -XX:+UseG1GC -jar app.jar
```

**After (ZGC):**
```bash
java -Xmx512m -XX:+UseZGC -XX:+ZGenerational -jar app.jar
```

#### 결과

| 메트릭 | Before | After | 개선율 |
|--------|--------|-------|--------|
| Max GC Pause | 200ms | 5ms | -97% |
| P99 응답 시간 | 800ms | 400ms | -50% |
| P99.9 응답 시간 | 2s | 500ms | -75% |

---

## 최적화 체크리스트

### 애플리케이션 레벨
- [ ] 캐싱 전략 (Redis, 로컬 캐시)
- [ ] 비동기 처리 (CompletableFuture, WebFlux)
- [ ] 배치 처리 (대량 데이터)
- [ ] 커넥션 풀 튜닝
- [ ] 스레드 풀 튜닝

### 데이터베이스 레벨
- [ ] 인덱스 최적화
- [ ] 쿼리 최적화 (N+1, 서브쿼리)
- [ ] 파티셔닝/샤딩
- [ ] Read Replica 분리
- [ ] 커넥션 풀러 (PgBouncer)

### JVM 레벨
- [ ] 힙 크기 조정
- [ ] GC 알고리즘 선택
- [ ] JIT 컴파일러 옵션

### 인프라 레벨
- [ ] 오토스케일링 설정
- [ ] 로드밸런서 튜닝
- [ ] CDN 적용
- [ ] 지역 분산

## 성능 개선 추이

```
처리량 (RPS)
800 ┤                                    ╭──────
700 ┤                              ╭─────╯
600 ┤                        ╭─────╯
500 ┤                  ╭─────╯
400 ┤            ╭─────╯
300 ┤      ╭─────╯
200 ┤╭─────╯
100 ┼╯
    └────────────────────────────────────────────
     초기   OPT-001  OPT-002  OPT-003  OPT-004
```

---

*마지막 업데이트: YYYY-MM-DD*
