import http from 'k6/http';
import { check, sleep, group } from 'k6';
import { Rate, Counter, Trend } from 'k6/metrics';
import { config, httpOptions } from '../lib/config.js';
import { login, getAuthHeaders } from '../lib/auth.js';

/**
 * Stress Test
 *
 * 목적: 시스템의 한계점 파악
 * - 점진적으로 극단적인 부하 증가 (100 → 500 VU)
 * - 시스템이 어디서 실패하는지 확인
 * - 복구 능력 테스트
 */

export const options = {
  stages: [
    { duration: '2m', target: 100 },   // 시작
    { duration: '5m', target: 200 },   // 증가
    { duration: '5m', target: 300 },   // 고부하
    { duration: '5m', target: 500 },   // 극한 부하
    { duration: '5m', target: 0 },     // 복구
  ],

  thresholds: {
    // Stress test에서는 임계값을 완화
    http_req_duration: ['p(95)<2000', 'p(99)<5000'],
    http_req_failed: ['rate<0.1'],  // 10% 에러 허용
    checks: ['rate>0.8'],
  },

  tags: {
    testType: 'stress',
  },
};

// 커스텀 메트릭
const errorRate = new Rate('errors');
const timeoutCount = new Counter('timeouts');
const responseTime = new Trend('response_time_trend');
const concurrentUsers = new Counter('concurrent_users');

export function setup() {
  const loginResult = login();

  return {
    token: loginResult.token,
    startTime: new Date().toISOString(),
  };
}

export default function (data) {
  concurrentUsers.add(1);

  const headers = data.token ? getAuthHeaders(data.token) : httpOptions.headers;

  // 다양한 API 엔드포인트에 부하 분산
  const endpoints = [
    { weight: 30, fn: () => healthCheck() },
    { weight: 25, fn: () => getQuestions(headers) },
    { weight: 20, fn: () => searchQuestions(headers) },
    { weight: 15, fn: () => getUserProfile(headers) },
    { weight: 10, fn: () => heavyOperation(headers) },
  ];

  // 가중치 기반 랜덤 선택
  const random = Math.random() * 100;
  let cumulative = 0;

  for (const endpoint of endpoints) {
    cumulative += endpoint.weight;
    if (random < cumulative) {
      endpoint.fn();
      break;
    }
  }

  // 짧은 간격으로 요청 (스트레스 테스트)
  sleep(Math.random() * 2);
}

function healthCheck() {
  const res = http.get(`${config.services.gateway}/actuator/health`, {
    tags: { name: 'health-check' },
    timeout: '10s',
  });

  responseTime.add(res.timings.duration);

  check(res, {
    'health check ok': (r) => r.status === 200,
  }) || errorRate.add(1);

  if (res.timings.duration > 10000) {
    timeoutCount.add(1);
  }
}

function getQuestions(headers) {
  const page = Math.floor(Math.random() * 10);

  const res = http.get(
    `${config.services.question}/api/v1/questions?page=${page}&size=50`,
    {
      headers,
      tags: { name: 'get-questions' },
      timeout: '30s',
    }
  );

  responseTime.add(res.timings.duration);

  check(res, {
    'questions ok': (r) => r.status === 200 || r.status === 204,
  }) || errorRate.add(1);
}

function searchQuestions(headers) {
  const keywords = ['java', 'spring', 'kubernetes', 'react', 'python', 'aws', 'docker'];
  const keyword = keywords[Math.floor(Math.random() * keywords.length)];

  const res = http.get(
    `${config.services.question}/api/v1/questions/search?keyword=${keyword}&page=0&size=20`,
    {
      headers,
      tags: { name: 'search-questions' },
      timeout: '30s',
    }
  );

  responseTime.add(res.timings.duration);

  check(res, {
    'search ok': (r) => r.status === 200 || r.status === 204,
  }) || errorRate.add(1);
}

function getUserProfile(headers) {
  const res = http.get(`${config.services.user}/api/v1/users/me`, {
    headers,
    tags: { name: 'get-profile' },
    timeout: '10s',
  });

  responseTime.add(res.timings.duration);

  check(res, {
    'profile ok': (r) => r.status === 200,
  }) || errorRate.add(1);
}

function heavyOperation(headers) {
  // RAG 검색 (벡터 DB 쿼리)
  const res = http.get(
    `${config.services.question}/api/v1/questions/similar?skills=java,spring,microservices&limit=10`,
    {
      headers,
      tags: { name: 'rag-search' },
      timeout: '60s',
    }
  );

  responseTime.add(res.timings.duration);

  check(res, {
    'rag search ok': (r) => r.status === 200 || r.status === 204,
    'rag search response time': (r) => r.timings.duration < 5000,
  }) || errorRate.add(1);
}

export function teardown(data) {
  console.log(`Stress test completed. Started at: ${data.startTime}`);
  console.log('Check metrics for system breaking point analysis.');
}
