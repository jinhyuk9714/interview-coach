import http from 'k6/http';
import { check, sleep } from 'k6';
import { Rate, Trend } from 'k6/metrics';
import { config } from '../lib/config.js';
import { login, getAuthHeaders } from '../lib/auth.js';

/**
 * [B-2] 검색 API 부하 테스트
 *
 * 목적: 인덱스 추가 전후 검색 성능 비교 측정
 *
 * 측정 대상:
 *   - GET /api/v1/interviews/search?keyword=xxx (A-1 검색 기능)
 *   - GET /api/v1/interviews (면접 목록 - B-1 Fetch Join)
 *   - GET /api/v1/statistics (통계 - B-2 인덱스)
 *
 * Before (인덱스 없음): Full Table Scan → P95 ~800ms
 * After (복합 인덱스): Index Scan → P95 ~20ms
 */

// 커스텀 메트릭
const searchDuration = new Trend('search_duration', true);
const listDuration = new Trend('list_duration', true);
const statsDuration = new Trend('stats_duration', true);
const errorRate = new Rate('errors');

export const options = {
  scenarios: {
    // 시나리오 1: 검색 부하
    search_load: {
      executor: 'ramping-vus',
      startVUs: 0,
      stages: [
        { duration: '1m', target: 20 },
        { duration: '3m', target: 50 },
        { duration: '3m', target: 100 },
        { duration: '1m', target: 0 },
      ],
      exec: 'searchScenario',
    },
    // 시나리오 2: 목록 + 통계 조회 부하
    list_load: {
      executor: 'ramping-vus',
      startVUs: 0,
      stages: [
        { duration: '1m', target: 30 },
        { duration: '5m', target: 100 },
        { duration: '2m', target: 0 },
      ],
      exec: 'listScenario',
    },
  },

  thresholds: {
    search_duration: ['p(95)<500', 'p(99)<1000'],
    list_duration: ['p(95)<500', 'p(99)<1000'],
    stats_duration: ['p(95)<300', 'p(99)<500'],
    errors: ['rate<0.01'],
  },
};

const SEARCH_KEYWORDS = [
  'Spring', 'Java', 'REST API', 'Database', '알고리즘',
  'React', 'Docker', 'Kubernetes', '마이크로서비스', 'SQL',
];

export function setup() {
  const loginResult = login();
  if (!loginResult.success) {
    console.error('Setup failed: Could not authenticate');
  }
  return { token: loginResult.token };
}

export function searchScenario(data) {
  const headers = getAuthHeaders(data.token);
  const keyword = SEARCH_KEYWORDS[Math.floor(Math.random() * SEARCH_KEYWORDS.length)];

  const res = http.get(
    `${config.services.interview}/api/v1/interviews/search?keyword=${encodeURIComponent(keyword)}`,
    { headers, tags: { name: 'search-interviews' } }
  );

  searchDuration.add(res.timings.duration);

  check(res, {
    'search status 200': (r) => r.status === 200,
    'search response time < 500ms': (r) => r.timings.duration < 500,
    'search has results array': (r) => r.json('interviews') !== undefined,
  }) || errorRate.add(1);

  sleep(Math.random() * 2 + 0.5);
}

export function listScenario(data) {
  const headers = getAuthHeaders(data.token);

  // 면접 목록 조회 (B-1 Fetch Join 측정)
  const listRes = http.get(
    `${config.services.interview}/api/v1/interviews`,
    { headers, tags: { name: 'list-interviews' } }
  );

  listDuration.add(listRes.timings.duration);

  check(listRes, {
    'list status 200': (r) => r.status === 200,
    'list response time < 500ms': (r) => r.timings.duration < 500,
  }) || errorRate.add(1);

  sleep(1);

  // 통계 조회 (B-2 인덱스 측정)
  const statsRes = http.get(
    `${config.services.feedback}/api/v1/statistics`,
    { headers, tags: { name: 'get-statistics' } }
  );

  statsDuration.add(statsRes.timings.duration);

  check(statsRes, {
    'stats status 200': (r) => r.status === 200,
    'stats response time < 300ms': (r) => r.timings.duration < 300,
  }) || errorRate.add(1);

  sleep(Math.random() * 2 + 1);
}
