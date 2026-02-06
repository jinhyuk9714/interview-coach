import http from 'k6/http';
import { check, sleep } from 'k6';
import { Rate, Trend, Counter, Gauge } from 'k6/metrics';
import { config, httpOptions } from '../lib/config.js';
import { login, getAuthHeaders } from '../lib/auth.js';

/**
 * [B-6, B-7] Soak Test (장시간 부하 테스트)
 *
 * 목적:
 *   - B-6: SseEmitter 메모리 누수 감지 (24h 힙 추이)
 *   - B-7: GC 안정성 검증 (Full GC 빈도, Pause 시간)
 *   - HikariCP 커넥션 풀 안정성 (B-8)
 *
 * 시나리오:
 *   1. 100 VU가 4시간 동안 지속적으로 API 호출
 *   2. 매 10분마다 JVM 메모리 메트릭 수집 (Actuator)
 *   3. SSE 피드백 요청도 간헐적으로 포함
 *
 * 핵심 관찰:
 *   - jvm.memory.used{area="heap"} 추이 (선형 증가 = 누수)
 *   - jvm.gc.pause 빈도 및 시간
 *   - hikaricp.connections.pending 급증 여부
 */

// 커스텀 메트릭
const apiDuration = new Trend('api_duration', true);
const sseDuration = new Trend('sse_duration', true);
const memoryMetric = new Gauge('jvm_heap_used_mb');
const gcPauseMetric = new Gauge('gc_pause_ms');
const errorRate = new Rate('errors');
const requestCount = new Counter('total_requests');

export const options = {
  scenarios: {
    steady_load: {
      executor: 'ramping-vus',
      startVUs: 0,
      stages: [
        { duration: '5m', target: 100 },  // 웜업
        { duration: '4h', target: 100 },   // 지속 부하
        { duration: '5m', target: 0 },     // 쿨다운
      ],
      exec: 'steadyLoad',
    },
    memory_monitor: {
      executor: 'constant-vus',
      vus: 1,
      duration: '4h10m',
      exec: 'memoryMonitor',
    },
  },

  thresholds: {
    api_duration: ['p(95)<1000', 'p(99)<2000'],
    errors: ['rate<0.05'],
  },
};

export function setup() {
  const loginResult = login();
  if (!loginResult.success) {
    console.error('Setup failed: Could not authenticate');
  }
  return {
    token: loginResult.token,
    startTime: Date.now(),
  };
}

// 메인 부하 시나리오
export function steadyLoad(data) {
  const headers = getAuthHeaders(data.token);

  // 랜덤하게 다양한 API 호출
  const scenario = Math.random();

  if (scenario < 0.3) {
    // 면접 목록 조회 (가장 빈번한 API)
    const res = http.get(
      `${config.services.interview}/api/v1/interviews`,
      { headers, tags: { name: 'list-interviews' } }
    );
    apiDuration.add(res.timings.duration);
    check(res, { 'list ok': (r) => r.status === 200 }) || errorRate.add(1);

  } else if (scenario < 0.5) {
    // 통계 조회
    const res = http.get(
      `${config.services.feedback}/api/v1/statistics`,
      { headers, tags: { name: 'get-statistics' } }
    );
    apiDuration.add(res.timings.duration);
    check(res, { 'stats ok': (r) => r.status === 200 }) || errorRate.add(1);

  } else if (scenario < 0.7) {
    // JD 목록 조회 (캐싱 효과 측정)
    const res = http.get(
      `${config.services.question}/api/v1/jd`,
      { headers, tags: { name: 'list-jd' } }
    );
    apiDuration.add(res.timings.duration);
    check(res, { 'jd ok': (r) => r.status === 200 }) || errorRate.add(1);

  } else if (scenario < 0.85) {
    // 검색 API
    const keywords = ['Java', 'Spring', 'React', 'SQL', 'Docker'];
    const keyword = keywords[Math.floor(Math.random() * keywords.length)];
    const res = http.get(
      `${config.services.interview}/api/v1/interviews/search?keyword=${keyword}`,
      { headers, tags: { name: 'search' } }
    );
    apiDuration.add(res.timings.duration);
    check(res, { 'search ok': (r) => r.status === 200 }) || errorRate.add(1);

  } else {
    // 통계 기록 (쓰기 부하)
    const payload = JSON.stringify({
      skillCategory: 'Java',
      isCorrect: Math.random() > 0.3,
      score: Math.floor(Math.random() * 100),
    });

    const res = http.post(
      `${config.services.feedback}/api/v1/statistics/record`,
      payload,
      { headers: { ...headers, ...httpOptions.headers }, tags: { name: 'record-stats' } }
    );
    apiDuration.add(res.timings.duration);
    check(res, { 'record ok': (r) => r.status === 200 }) || errorRate.add(1);
  }

  requestCount.add(1);
  sleep(Math.random() * 3 + 1);
}

// 메모리 모니터링 시나리오 (10분 간격)
export function memoryMonitor() {
  const services = [
    { name: 'interview', url: config.services.interview },
    { name: 'feedback', url: config.services.feedback },
    { name: 'question', url: config.services.question },
  ];

  for (const svc of services) {
    try {
      // JVM 힙 메모리 수집
      const memRes = http.get(
        `${svc.url}/actuator/metrics/jvm.memory.used?tag=area:heap`,
        { tags: { name: `${svc.name}-heap-memory` } }
      );

      if (memRes.status === 200) {
        const measurements = memRes.json('measurements');
        if (measurements && measurements.length > 0) {
          const heapMb = measurements[0].value / (1024 * 1024);
          memoryMetric.add(heapMb);
          console.log(`[${svc.name}] Heap: ${heapMb.toFixed(1)}MB`);
        }
      }

      // GC Pause 수집
      const gcRes = http.get(
        `${svc.url}/actuator/metrics/jvm.gc.pause`,
        { tags: { name: `${svc.name}-gc-pause` } }
      );

      if (gcRes.status === 200) {
        const measurements = gcRes.json('measurements');
        if (measurements) {
          const maxPause = measurements.find(m => m.statistic === 'MAX');
          if (maxPause) {
            gcPauseMetric.add(maxPause.value * 1000);
            console.log(`[${svc.name}] Max GC Pause: ${(maxPause.value * 1000).toFixed(1)}ms`);
          }
        }
      }
    } catch (e) {
      console.log(`Failed to collect metrics from ${svc.name}: ${e.message}`);
    }
  }

  sleep(600); // 10분 간격
}

export function teardown(data) {
  const elapsed = ((Date.now() - data.startTime) / 1000 / 60).toFixed(1);
  console.log(`=== Soak Test 완료 ===`);
  console.log(`실행 시간: ${elapsed}분`);
  console.log('Grafana에서 확인할 대시보드:');
  console.log('  - jvm.memory.used{area="heap"}: 선형 증가 = 누수');
  console.log('  - jvm.gc.pause: 스파이크 = GC 문제');
  console.log('  - hikaricp.connections.pending: 급증 = 풀 부족');
}
