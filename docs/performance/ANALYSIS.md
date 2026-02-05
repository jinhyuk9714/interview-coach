# 성능 분석 가이드

## 개요

이 문서는 성능 테스트 결과를 분석하고 병목 지점을 식별하는 방법을 안내합니다.

## 분석 프로세스

```
┌─────────────┐     ┌─────────────┐     ┌─────────────┐     ┌─────────────┐
│  테스트 실행  │ ──▶ │  결과 수집   │ ──▶ │  병목 식별   │ ──▶ │  원인 분석   │
└─────────────┘     └─────────────┘     └─────────────┘     └─────────────┘
                                                                    │
                                                                    ▼
┌─────────────┐     ┌─────────────┐     ┌─────────────┐     ┌─────────────┐
│  문서화      │ ◀── │  검증       │ ◀── │  최적화     │ ◀── │  해결책 도출 │
└─────────────┘     └─────────────┘     └─────────────┘     └─────────────┘
```

## 1. 메트릭 수집

### k6 결과 분석

```bash
# 결과 요약 확인
jq '.metrics' results/load-test-summary.json

# 주요 메트릭 추출
jq '{
  total_requests: .metrics.http_reqs.values.count,
  error_rate: .metrics.http_req_failed.values.rate,
  avg_response: .metrics.http_req_duration.values.avg,
  p95_response: .metrics.http_req_duration.values["p(95)"],
  p99_response: .metrics.http_req_duration.values["p(99)"],
  throughput: .metrics.http_reqs.values.rate
}' results/load-test-summary.json
```

### Grafana 대시보드 확인

1. **k6 Load Testing Dashboard**
   - Request Rate 그래프에서 처리량 변화 확인
   - Response Time 그래프에서 지연 패턴 분석
   - Error Rate에서 에러 발생 시점 식별

2. **JVM Micrometer Dashboard**
   - Heap Memory 사용량 추이
   - GC Pause 발생 빈도
   - HikariCP Connection Pool 상태

## 2. 병목 식별

### 증상별 체크리스트

#### 높은 응답 지연 (P95 > 500ms)

- [ ] 데이터베이스 쿼리 시간
- [ ] 외부 API 호출 (LLM, 외부 서비스)
- [ ] 네트워크 지연
- [ ] 비효율적인 비즈니스 로직
- [ ] 직렬화/역직렬화 오버헤드

#### 높은 에러율 (> 1%)

- [ ] 커넥션 풀 고갈
- [ ] 타임아웃 설정
- [ ] 메모리 부족 (OOM)
- [ ] 외부 서비스 장애
- [ ] 동시성 이슈

#### 낮은 처리량

- [ ] CPU 병목
- [ ] I/O 블로킹
- [ ] 스레드 풀 크기
- [ ] 락 경합 (Lock Contention)
- [ ] 가비지 컬렉션

### 도구별 분석 방법

#### Prometheus 쿼리

```promql
# 서비스별 P95 응답 시간
histogram_quantile(0.95,
  sum(rate(http_server_requests_seconds_bucket[5m])) by (le, uri)
)

# 에러율 (5분 간격)
sum(rate(http_server_requests_seconds_count{status=~"5.."}[5m]))
/ sum(rate(http_server_requests_seconds_count[5m]))

# HikariCP 커넥션 사용률
hikaricp_connections_active / hikaricp_connections_max

# JVM 힙 사용률
jvm_memory_used_bytes{area="heap"} / jvm_memory_max_bytes{area="heap"}
```

#### APM 트레이스 분석

```bash
# Distributed Tracing (Jaeger/Zipkin 사용 시)
# 느린 트랜잭션 식별
# 서비스 간 호출 지연 확인
```

## 3. 원인 분석 템플릿

### 분석 기록 양식

```markdown
## 병목 분석 #[번호]

**날짜:** YYYY-MM-DD
**분석자:** [이름]
**테스트 유형:** [Load/Stress/Spike]

### 증상
- 발견된 문제점 설명
- 영향 받는 엔드포인트

### 측정값
| 메트릭 | 기대값 | 실제값 | 차이 |
|--------|--------|--------|------|
| P95 응답 시간 | 500ms | 1200ms | +140% |
| 에러율 | <1% | 5% | +400% |

### 원인 분석

#### 가설 1: [가설 설명]
- 근거: ...
- 검증 방법: ...
- 결과: [확인됨/기각됨]

#### 가설 2: [가설 설명]
- 근거: ...
- 검증 방법: ...
- 결과: [확인됨/기각됨]

### 근본 원인
[확인된 근본 원인 설명]

### 해결 방안
1. [단기 해결책]
2. [장기 해결책]

### 관련 이슈
- GitHub Issue: #XXX
```

## 4. 일반적인 병목 패턴

### 데이터베이스 병목

**증상:**
- HikariCP pending connections 증가
- Connection acquisition time 증가
- Query execution time 증가

**분석 방법:**
```sql
-- PostgreSQL 느린 쿼리 확인
SELECT query, calls, mean_time, total_time
FROM pg_stat_statements
ORDER BY mean_time DESC
LIMIT 10;

-- 인덱스 사용 확인
EXPLAIN ANALYZE SELECT ...;
```

**해결 방안:**
- 쿼리 최적화 (인덱스 추가)
- 커넥션 풀 크기 조정
- 읽기 전용 쿼리 분리 (Read Replica)
- 캐싱 적용

### LLM API 병목

**증상:**
- JD 분석 API 응답 시간 30초 이상
- 타임아웃 에러 증가
- 동시 요청 시 지연 급증

**분석 방법:**
```bash
# API 호출 시간 측정
curl -w "@curl-format.txt" -o /dev/null -s "http://localhost:8082/api/v1/jd/analyze"

# curl-format.txt:
#   time_total:  %{time_total}\n
#   time_connect:  %{time_connect}\n
```

**해결 방안:**
- 요청 큐잉 및 배치 처리
- 응답 스트리밍 (SSE)
- 캐싱 (동일 JD 재분석 방지)
- Fallback LLM 설정

### 메모리 병목

**증상:**
- GC Pause 빈번 발생
- Heap 사용량 지속 증가
- OOM 에러

**분석 방법:**
```bash
# 힙 덤프
jmap -dump:format=b,file=heap.hprof <pid>

# GC 로그 분석
java -Xlog:gc*:file=gc.log:time,uptime,level,tags ...
```

**해결 방안:**
- 힙 크기 조정 (-Xmx, -Xms)
- GC 알고리즘 선택 (G1, ZGC)
- 메모리 누수 수정
- 대용량 객체 처리 개선

### Redis 캐시 병목

**증상:**
- 캐시 히트율 낮음
- Redis 메모리 부족
- 연결 타임아웃

**분석 방법:**
```bash
# Redis 상태 확인
redis-cli INFO stats
redis-cli INFO memory

# 느린 명령 확인
redis-cli SLOWLOG GET 10
```

**해결 방안:**
- TTL 조정
- 캐시 키 전략 개선
- 메모리 정책 설정 (maxmemory-policy)
- Redis Cluster 구성

## 5. 분석 결과 기록

### 예시: DB 커넥션 풀 병목

```markdown
## 병목 분석 #001

**날짜:** 2024-01-15
**분석자:** 홍길동
**테스트 유형:** Stress Test (300 VU)

### 증상
- 300 VU 도달 시 에러율 급증 (15%)
- `HikariPool-1 - Connection is not available` 에러 발생

### 측정값
| 메트릭 | 기대값 | 실제값 | 차이 |
|--------|--------|--------|------|
| 에러율 | <10% | 15% | +50% |
| Connection Wait | <1s | 30s | +2900% |

### 원인 분석

#### 가설 1: 커넥션 풀 크기 부족
- 근거: HikariCP max-pool-size=10, pending connections=50
- 검증 방법: pool 크기를 30으로 증가 후 재테스트
- 결과: ✅ 확인됨 - 에러율 3%로 감소

### 근본 원인
기본 HikariCP 설정(10개)이 300 동시 사용자 처리에 부족

### 해결 방안
1. [단기] maximum-pool-size를 30으로 증가
2. [장기] PostgreSQL max_connections 조정, PgBouncer 도입 검토

### 관련 이슈
- GitHub Issue: #123
- PR: #125
```

## 6. 권장 도구

| 도구 | 용도 |
|------|------|
| VisualVM | JVM 프로파일링 |
| async-profiler | CPU/메모리 프로파일링 |
| pgBadger | PostgreSQL 로그 분석 |
| RedisInsight | Redis 모니터링 |
| Wireshark | 네트워크 분석 |

---

*다음 문서: [TUNING.md](./TUNING.md)*
