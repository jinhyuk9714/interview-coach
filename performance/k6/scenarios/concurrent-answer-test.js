import http from 'k6/http';
import { check, sleep } from 'k6';
import { Counter, Rate, Trend } from 'k6/metrics';
import { config, httpOptions } from '../lib/config.js';
import { login, getAuthHeaders } from '../lib/auth.js';

/**
 * [B-3] 동시 답변 제출 (Race Condition) 테스트
 *
 * 목적: 동시에 같은 사용자의 통계를 업데이트할 때 데이터 정합성 측정
 *
 * 시나리오:
 *   1. 50명의 VU가 동시에 같은 사용자의 POST /statistics/record 호출
 *   2. 실행 전후 total_questions 비교
 *   3. 기대값 = 초기값 + 총 요청 수, 실제값과 비교
 *
 * Before (락 없음): ~30% 통계 불일치 (Lost Update)
 * After (PESSIMISTIC_WRITE): 100% 정합성
 */

// 커스텀 메트릭
const recordDuration = new Trend('record_answer_duration', true);
const successCount = new Counter('successful_records');
const failedCount = new Counter('failed_records');
const errorRate = new Rate('errors');

export const options = {
  scenarios: {
    concurrent_record: {
      executor: 'shared-iterations',
      vus: 50,
      iterations: 200,
      maxDuration: '2m',
    },
  },

  thresholds: {
    errors: ['rate<0.01'],
    record_answer_duration: ['p(95)<1000'],
  },
};

const CATEGORIES = ['Java', 'Spring', 'Database', 'Algorithm', 'System Design'];

export function setup() {
  const loginResult = login();
  if (!loginResult.success) {
    console.error('Setup failed: Could not authenticate');
    return {};
  }

  const headers = getAuthHeaders(loginResult.token);

  // 기준값 기록: 현재 총 통계 조회
  const statsRes = http.get(
    `${config.services.feedback}/api/v1/statistics`,
    { headers, tags: { name: 'get-baseline-stats' } }
  );

  let baselineTotalQuestions = 0;
  if (statsRes.status === 200) {
    try {
      baselineTotalQuestions = statsRes.json('totalQuestions') || 0;
    } catch (e) {
      console.log('Could not parse baseline stats');
    }
  }

  console.log(`Baseline total_questions: ${baselineTotalQuestions}`);
  console.log(`Expected after test: ${baselineTotalQuestions + 200}`);

  return {
    token: loginResult.token,
    baselineTotalQuestions,
    expectedTotal: baselineTotalQuestions + 200,
  };
}

export default function (data) {
  if (!data.token) return;

  const headers = getAuthHeaders(data.token);
  const category = CATEGORIES[Math.floor(Math.random() * CATEGORIES.length)];
  const score = Math.floor(Math.random() * 100);
  const isCorrect = score >= 60;

  const payload = JSON.stringify({
    skillCategory: category,
    isCorrect: isCorrect,
    score: score,
    weakPoint: score < 60 ? `${category} 기초 부족` : null,
  });

  const res = http.post(
    `${config.services.feedback}/api/v1/statistics/record`,
    payload,
    { headers: { ...headers, ...httpOptions.headers }, tags: { name: 'record-answer' } }
  );

  recordDuration.add(res.timings.duration);

  const success = check(res, {
    'record status 200': (r) => r.status === 200,
    'record response time < 1000ms': (r) => r.timings.duration < 1000,
  });

  if (success) {
    successCount.add(1);
  } else {
    failedCount.add(1);
    errorRate.add(1);
  }
}

export function teardown(data) {
  if (!data.token) return;

  const headers = getAuthHeaders(data.token);

  // 실행 후 통계 조회하여 정합성 검증
  sleep(2); // DB 커밋 대기

  const statsRes = http.get(
    `${config.services.feedback}/api/v1/statistics`,
    { headers, tags: { name: 'get-final-stats' } }
  );

  if (statsRes.status === 200) {
    try {
      const actualTotal = statsRes.json('totalQuestions') || 0;
      const expected = data.expectedTotal;
      const diff = expected - actualTotal;
      const accuracy = ((actualTotal - data.baselineTotalQuestions) / 200 * 100).toFixed(1);

      console.log('=== Race Condition 검증 결과 ===');
      console.log(`기준값: ${data.baselineTotalQuestions}`);
      console.log(`기대값: ${expected}`);
      console.log(`실제값: ${actualTotal}`);
      console.log(`차이: ${diff} (손실된 업데이트)`);
      console.log(`정합성: ${accuracy}%`);

      if (diff === 0) {
        console.log('✅ 데이터 정합성 100% - Race condition 해결됨');
      } else {
        console.log(`⚠️ ${diff}건 Lost Update 발생 - 비관적 락 적용 필요`);
      }
    } catch (e) {
      console.log('Could not parse final stats:', e.message);
    }
  }
}
