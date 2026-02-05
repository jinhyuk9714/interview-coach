import http from 'k6/http';
import { check, sleep } from 'k6';
import { Rate, Counter, Trend } from 'k6/metrics';
import { config, httpOptions } from '../lib/config.js';
import { login, getAuthHeaders } from '../lib/auth.js';

/**
 * Spike Test
 *
 * 목적: 급격한 부하 변화에 대한 시스템 반응 테스트
 * - 갑작스러운 트래픽 폭증 시뮬레이션
 * - 오토스케일링 검증
 * - 서킷 브레이커 동작 확인
 */

export const options = {
  stages: [
    { duration: '1m', target: 10 },    // 정상 부하
    { duration: '30s', target: 500 },  // 급격한 증가 (스파이크)
    { duration: '1m', target: 500 },   // 피크 유지
    { duration: '30s', target: 10 },   // 급격한 감소
    { duration: '2m', target: 10 },    // 복구 확인
    { duration: '30s', target: 300 },  // 두 번째 스파이크
    { duration: '1m', target: 300 },   // 피크 유지
    { duration: '1m', target: 0 },     // 종료
  ],

  thresholds: {
    http_req_duration: ['p(95)<3000'],
    http_req_failed: ['rate<0.15'],  // 스파이크 시 15% 에러 허용
    checks: ['rate>0.75'],
  },

  tags: {
    testType: 'spike',
  },
};

// 커스텀 메트릭
const errorRate = new Rate('errors');
const spikeRecoveryTime = new Trend('spike_recovery_time');
const requestsDuringSpike = new Counter('requests_during_spike');
const errorsDuringSpike = new Counter('errors_during_spike');

// 스파이크 상태 추적
let spikePhase = 'normal';
let spikeStartTime = null;
let recoveryStartTime = null;

export function setup() {
  const loginResult = login();

  return {
    token: loginResult.token,
    startTime: Date.now(),
  };
}

export default function (data) {
  const elapsed = (Date.now() - data.startTime) / 1000; // 초 단위

  // 스파이크 단계 감지
  if (elapsed > 60 && elapsed <= 150) {
    if (spikePhase !== 'spike1') {
      spikePhase = 'spike1';
      spikeStartTime = Date.now();
    }
    requestsDuringSpike.add(1);
  } else if (elapsed > 150 && elapsed <= 270) {
    if (spikePhase === 'spike1') {
      spikePhase = 'recovery1';
      recoveryStartTime = Date.now();
    }
  } else if (elapsed > 270 && elapsed <= 360) {
    if (spikePhase !== 'spike2') {
      spikePhase = 'spike2';
      spikeStartTime = Date.now();
    }
    requestsDuringSpike.add(1);
  }

  const headers = data.token ? getAuthHeaders(data.token) : httpOptions.headers;

  // 다양한 엔드포인트 호출
  const requests = [
    () => callHealthCheck(),
    () => callQuestionsAPI(headers),
    () => callUserAPI(headers),
    () => callSearchAPI(headers),
  ];

  const selectedRequest = requests[Math.floor(Math.random() * requests.length)];
  const result = selectedRequest();

  // 스파이크 중 에러 추적
  if (spikePhase.startsWith('spike') && !result.success) {
    errorsDuringSpike.add(1);
  }

  // 복구 시간 측정
  if (spikePhase.startsWith('recovery') && result.success && recoveryStartTime) {
    const recoveryTime = Date.now() - recoveryStartTime;
    spikeRecoveryTime.add(recoveryTime);
  }

  // 스파이크 시에는 더 빈번한 요청
  if (spikePhase.startsWith('spike')) {
    sleep(0.1 + Math.random() * 0.5);
  } else {
    sleep(1 + Math.random() * 2);
  }
}

function callHealthCheck() {
  const res = http.get(`${config.services.gateway}/actuator/health`, {
    tags: { name: 'health-check' },
    timeout: '5s',
  });

  const success = check(res, {
    'health check ok': (r) => r.status === 200,
  });

  if (!success) errorRate.add(1);

  return { success, response: res };
}

function callQuestionsAPI(headers) {
  const res = http.get(
    `${config.services.question}/api/v1/questions?page=0&size=10`,
    {
      headers,
      tags: { name: 'get-questions' },
      timeout: '10s',
    }
  );

  const success = check(res, {
    'questions ok': (r) => r.status === 200 || r.status === 204 || r.status === 503,
  });

  if (!success) errorRate.add(1);

  return { success, response: res };
}

function callUserAPI(headers) {
  const res = http.get(`${config.services.user}/api/v1/users/me`, {
    headers,
    tags: { name: 'get-profile' },
    timeout: '5s',
  });

  const success = check(res, {
    'profile ok': (r) => r.status === 200 || r.status === 503,
  });

  if (!success) errorRate.add(1);

  return { success, response: res };
}

function callSearchAPI(headers) {
  const keywords = ['java', 'spring', 'kubernetes'];
  const keyword = keywords[Math.floor(Math.random() * keywords.length)];

  const res = http.get(
    `${config.services.question}/api/v1/questions/search?keyword=${keyword}`,
    {
      headers,
      tags: { name: 'search-questions' },
      timeout: '15s',
    }
  );

  const success = check(res, {
    'search ok': (r) => r.status === 200 || r.status === 204 || r.status === 503,
  });

  if (!success) errorRate.add(1);

  return { success, response: res };
}

export function teardown(data) {
  const totalTime = (Date.now() - data.startTime) / 1000;
  console.log(`Spike test completed in ${totalTime.toFixed(2)} seconds`);
  console.log('Review spike_recovery_time and errors_during_spike metrics');
}
