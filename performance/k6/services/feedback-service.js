import http from 'k6/http';
import { check, sleep, group } from 'k6';
import { Rate, Trend, Counter } from 'k6/metrics';
import { randomIntBetween } from 'https://jslib.k6.io/k6-utils/1.4.0/index.js';
import { config, httpOptions } from '../lib/config.js';
import { login, getAuthHeaders } from '../lib/auth.js';
import { connectSSE, sseMetrics } from '../lib/sse-helper.js';

/**
 * Feedback Service 성능 테스트
 *
 * 테스트 대상:
 * - SSE 스트리밍 피드백
 * - 피드백 생성/조회
 * - 통계 API
 */

export const options = {
  scenarios: {
    // SSE 스트리밍 테스트
    streaming_feedback: {
      executor: 'ramping-vus',
      startVUs: 5,
      stages: [
        { duration: '2m', target: 20 },
        { duration: '5m', target: 30 },
        { duration: '3m', target: 0 },
      ],
      tags: { scenario: 'streaming_feedback' },
    },
    // 피드백 조회
    feedback_read: {
      executor: 'constant-vus',
      vus: 20,
      duration: '10m',
      tags: { scenario: 'feedback_read' },
    },
    // 통계 API
    statistics: {
      executor: 'constant-arrival-rate',
      rate: 10,
      timeUnit: '1s',
      duration: '5m',
      preAllocatedVUs: 10,
      maxVUs: 30,
      tags: { scenario: 'statistics' },
    },
  },

  thresholds: {
    // SSE 스트리밍 (긴 응답 시간 허용)
    'http_req_duration{scenario:streaming_feedback}': ['p(95)<60000'],
    'http_req_failed{scenario:streaming_feedback}': ['rate<0.1'],

    // 피드백 조회
    'http_req_duration{scenario:feedback_read}': ['p(95)<500'],
    'http_req_failed{scenario:feedback_read}': ['rate<0.01'],

    // 통계
    'http_req_duration{scenario:statistics}': ['p(95)<1000'],
    'http_req_failed{scenario:statistics}': ['rate<0.02'],

    // SSE 커스텀 메트릭
    sse_first_event_time: ['p(95)<5000'],
    sse_total_stream_time: ['p(95)<60000'],
  },
};

// 커스텀 메트릭
const feedbackGenerationTime = new Trend('feedback_generation_time');
const streamingDuration = new Trend('streaming_duration');
const feedbackFetchTime = new Trend('feedback_fetch_time');
const statisticsTime = new Trend('statistics_time');
const streamConnectionSuccess = new Rate('stream_connection_success');
const eventsReceived = new Counter('total_events_received');

// 테스트용 답변 데이터
const sampleAnswers = [
  {
    questionId: 1,
    content: `
      Java의 가비지 컬렉션은 힙 메모리에서 더 이상 사용되지 않는 객체를 자동으로 제거하는 메커니즘입니다.
      JVM은 Young Generation과 Old Generation으로 힙을 나누어 관리합니다.
      Young Generation에서는 Minor GC가, Old Generation에서는 Major GC가 발생합니다.
      G1 GC는 Java 9부터 기본 컬렉터로, Region 기반으로 동작하여 예측 가능한 일시 정지 시간을 제공합니다.
    `,
  },
  {
    questionId: 2,
    content: `
      마이크로서비스 아키텍처에서 서비스 간 통신은 크게 동기와 비동기 방식으로 나뉩니다.
      동기 통신은 REST API나 gRPC를 사용하고, 비동기 통신은 메시지 큐(Kafka, RabbitMQ)를 활용합니다.
      서비스 디스커버리는 Consul이나 Eureka를 통해 구현하고, API Gateway로 라우팅을 중앙화합니다.
      서킷 브레이커 패턴(Resilience4j)으로 장애 전파를 방지합니다.
    `,
  },
  {
    questionId: 3,
    content: `
      데이터베이스 인덱스는 B-Tree 또는 해시 구조로 구현됩니다.
      B-Tree 인덱스는 범위 검색에 효과적이고, 복합 인덱스의 컬럼 순서가 중요합니다.
      인덱스 선정 시 카디널리티가 높은 컬럼을 우선 고려하고, 쿼리 패턴을 분석해야 합니다.
      과도한 인덱스는 INSERT/UPDATE 성능을 저하시키므로 균형이 필요합니다.
    `,
  },
];

export function setup() {
  const loginResult = login();

  // 테스트용 답변 생성 (피드백 테스트에 사용)
  const headers = getAuthHeaders(loginResult.token);
  const answerIds = [];

  for (const answer of sampleAnswers) {
    const res = http.post(
      `${config.services.interview}/api/v1/answers`,
      JSON.stringify(answer),
      { headers }
    );

    if (res.status === 201 || res.status === 200) {
      try {
        answerIds.push(res.json('id'));
      } catch {
        // ignore
      }
    }
  }

  return {
    token: loginResult.token,
    answerIds,
    startTime: Date.now(),
  };
}

export default function (data) {
  const scenario = __ENV.K6_SCENARIO_NAME;
  const headers = getAuthHeaders(data.token);

  switch (scenario) {
    case 'streaming_feedback':
      testStreamingFeedback(headers, data.answerIds);
      break;
    case 'feedback_read':
      testFeedbackRead(headers);
      break;
    case 'statistics':
      testStatistics(headers);
      break;
    default:
      // 가중치 기반 랜덤 실행
      const random = Math.random();
      if (random < 0.3) {
        testStreamingFeedback(headers, data.answerIds);
      } else if (random < 0.7) {
        testFeedbackRead(headers);
      } else {
        testStatistics(headers);
      }
  }
}

function testStreamingFeedback(headers, answerIds) {
  group('SSE Streaming Feedback', function () {
    // 답변 제출 및 스트리밍 피드백 요청
    const answer = sampleAnswers[Math.floor(Math.random() * sampleAnswers.length)];

    // 새 답변 제출
    const submitRes = http.post(
      `${config.services.interview}/api/v1/answers`,
      JSON.stringify(answer),
      {
        headers,
        tags: { name: 'submit-answer' },
      }
    );

    let answerId;
    if (submitRes.status === 201 || submitRes.status === 200) {
      try {
        answerId = submitRes.json('id');
      } catch {
        // 기존 답변 ID 사용
        answerId = answerIds && answerIds.length > 0
          ? answerIds[Math.floor(Math.random() * answerIds.length)]
          : 1;
      }
    } else {
      answerId = answerIds && answerIds.length > 0
        ? answerIds[Math.floor(Math.random() * answerIds.length)]
        : 1;
    }

    sleep(1);

    // SSE 스트리밍 피드백 요청
    const startTime = Date.now();
    const streamUrl = `${config.services.feedback}/api/v1/feedback/stream/${answerId}`;

    const sseResult = connectSSE(streamUrl, {
      token: data.token,
      timeout: 60000,
      tagName: 'feedback-stream',
    });

    const duration = Date.now() - startTime;
    streamingDuration.add(duration);
    feedbackGenerationTime.add(duration);

    streamConnectionSuccess.add(sseResult.success ? 1 : 0);

    if (sseResult.success) {
      eventsReceived.add(sseResult.events.length);
    }

    check(sseResult, {
      'SSE connection successful': (r) => r.success,
      'received feedback events': (r) => r.events && r.events.length > 0,
    });

    // 스트리밍 완료 후 대기
    sleep(randomIntBetween(3, 5));
  });
}

function testFeedbackRead(headers) {
  group('Feedback Read Operations', function () {
    // 피드백 목록 조회
    const page = randomIntBetween(0, 5);

    const startTime = Date.now();
    const listRes = http.get(
      `${config.services.feedback}/api/v1/feedback?page=${page}&size=20`,
      {
        headers,
        tags: { name: 'list-feedback' },
      }
    );

    const duration = Date.now() - startTime;
    feedbackFetchTime.add(duration);

    check(listRes, {
      'list feedback status 200': (r) => r.status === 200 || r.status === 204,
      'list feedback response time < 500ms': (r) => r.timings.duration < 500,
    });

    sleep(randomIntBetween(1, 2));

    // 개별 피드백 조회
    if (listRes.status === 200) {
      try {
        const feedbacks = listRes.json('content') || listRes.json();
        if (feedbacks && feedbacks.length > 0) {
          const feedback = feedbacks[Math.floor(Math.random() * feedbacks.length)];
          if (feedback.id) {
            const detailRes = http.get(
              `${config.services.feedback}/api/v1/feedback/${feedback.id}`,
              {
                headers,
                tags: { name: 'get-feedback-detail' },
              }
            );

            check(detailRes, {
              'get feedback detail status 200': (r) => r.status === 200,
            });
          }
        }
      } catch {
        // ignore
      }
    }

    sleep(randomIntBetween(2, 4));
  });
}

function testStatistics(headers) {
  group('Statistics API', function () {
    // 사용자 통계 조회
    const startTime = Date.now();
    const statsRes = http.get(`${config.services.feedback}/api/v1/statistics/user`, {
      headers,
      tags: { name: 'user-statistics' },
    });

    const duration = Date.now() - startTime;
    statisticsTime.add(duration);

    check(statsRes, {
      'user statistics status 200': (r) => r.status === 200,
      'statistics response time < 1s': (r) => r.timings.duration < 1000,
    });

    sleep(1);

    // 카테고리별 통계
    const categories = ['backend', 'frontend', 'devops', 'system-design'];
    const category = categories[Math.floor(Math.random() * categories.length)];

    const categoryStatsRes = http.get(
      `${config.services.feedback}/api/v1/statistics/category/${category}`,
      {
        headers,
        tags: { name: 'category-statistics' },
      }
    );

    check(categoryStatsRes, {
      'category statistics status 200': (r) => r.status === 200 || r.status === 204,
    });

    sleep(1);

    // 시간별 통계 (최근 30일)
    const timeStatsRes = http.get(
      `${config.services.feedback}/api/v1/statistics/timeline?days=30`,
      {
        headers,
        tags: { name: 'timeline-statistics' },
      }
    );

    check(timeStatsRes, {
      'timeline statistics status 200': (r) => r.status === 200,
    });

    sleep(randomIntBetween(2, 4));
  });
}

export function teardown(data) {
  const totalTime = (Date.now() - data.startTime) / 1000;
  console.log(`Feedback Service test completed in ${totalTime.toFixed(2)} seconds`);
  console.log(`Created answer IDs: ${data.answerIds ? data.answerIds.length : 0}`);
}
