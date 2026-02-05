import http from 'k6/http';
import { check, sleep } from 'k6';
import { Rate } from 'k6/metrics';
import { config, httpOptions, thresholds } from '../lib/config.js';
import { login, getAuthHeaders } from '../lib/auth.js';

/**
 * Smoke Test
 *
 * 목적: 시스템의 기본 동작 확인
 * - 최소한의 부하 (1-5 VU)
 * - 짧은 시간 (1분)
 * - 기본 기능 검증
 */

export const options = {
  vus: 1,
  duration: '1m',

  thresholds: {
    http_req_duration: ['p(95)<500'],
    http_req_failed: ['rate<0.01'],
    checks: ['rate>0.99'],
  },

  tags: {
    testType: 'smoke',
  },
};

// 에러율 커스텀 메트릭
const errorRate = new Rate('errors');

export function setup() {
  // 테스트 시작 전 로그인
  const loginResult = login();

  if (!loginResult.success) {
    console.error('Setup failed: Could not authenticate');
  }

  return {
    token: loginResult.token,
    startTime: new Date().toISOString(),
  };
}

export default function (data) {
  const headers = getAuthHeaders(data.token);

  // 1. Health Check
  const healthRes = http.get(`${config.services.gateway}/actuator/health`, {
    tags: { name: 'health-check' },
  });

  check(healthRes, {
    'health check status 200': (r) => r.status === 200,
    'health check response time < 200ms': (r) => r.timings.duration < 200,
  }) || errorRate.add(1);

  sleep(1);

  // 2. 사용자 프로필 조회
  const profileRes = http.get(`${config.services.user}/api/v1/users/me`, {
    headers,
    tags: { name: 'get-profile' },
  });

  check(profileRes, {
    'profile status 200': (r) => r.status === 200,
    'profile has user data': (r) => r.json('email') !== undefined,
  }) || errorRate.add(1);

  sleep(1);

  // 3. 질문 목록 조회
  const questionsRes = http.get(`${config.services.question}/api/v1/questions?page=0&size=10`, {
    headers,
    tags: { name: 'list-questions' },
  });

  check(questionsRes, {
    'questions status 200': (r) => r.status === 200 || r.status === 204,
  }) || errorRate.add(1);

  sleep(1);

  // 4. 면접 세션 목록 조회
  const sessionsRes = http.get(`${config.services.interview}/api/v1/sessions?page=0&size=10`, {
    headers,
    tags: { name: 'list-sessions' },
  });

  check(sessionsRes, {
    'sessions status 200': (r) => r.status === 200 || r.status === 204,
  }) || errorRate.add(1);

  sleep(1);
}

export function teardown(data) {
  console.log(`Smoke test completed. Started at: ${data.startTime}`);
}
