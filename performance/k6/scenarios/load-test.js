import http from 'k6/http';
import { check, sleep, group } from 'k6';
import { Rate, Trend } from 'k6/metrics';
import { config, httpOptions, vuScenarios } from '../lib/config.js';
import { login, getAuthHeaders, authenticatedRequest } from '../lib/auth.js';

/**
 * Load Test
 *
 * 목적: 일반적인 운영 부하에서의 성능 측정
 * - 점진적 부하 증가 (50 → 100 VU)
 * - 중간 시간 (10분)
 * - 실제 사용자 시나리오 시뮬레이션
 */

export const options = {
  stages: [
    { duration: '2m', target: 50 },   // 램프업
    { duration: '5m', target: 50 },   // 유지
    { duration: '2m', target: 100 },  // 증가
    { duration: '5m', target: 100 },  // 유지
    { duration: '2m', target: 0 },    // 램프다운
  ],

  thresholds: {
    http_req_duration: ['p(95)<500', 'p(99)<1000'],
    http_req_failed: ['rate<0.01'],
    'http_req_duration{name:login}': ['p(95)<300'],
    'http_req_duration{name:get-questions}': ['p(95)<500'],
    'http_req_duration{name:analyze-jd}': ['p(95)<10000'],
    checks: ['rate>0.95'],
  },

  tags: {
    testType: 'load',
  },
};

// 커스텀 메트릭
const errorRate = new Rate('errors');
const jdAnalysisTime = new Trend('jd_analysis_time');
const questionSearchTime = new Trend('question_search_time');

// 테스트 데이터
const jobDescriptions = [
  {
    title: 'Backend Developer',
    company: 'Tech Company',
    description: 'Looking for a skilled backend developer with experience in Java, Spring Boot, and microservices architecture. Must have strong knowledge of RESTful APIs and database design.',
    requirements: ['Java', 'Spring Boot', 'PostgreSQL', 'Redis', 'Docker'],
  },
  {
    title: 'Full Stack Developer',
    company: 'Startup Inc',
    description: 'We need a versatile developer who can work on both frontend and backend. Experience with React and Node.js is required.',
    requirements: ['React', 'Node.js', 'TypeScript', 'MongoDB', 'AWS'],
  },
  {
    title: 'DevOps Engineer',
    company: 'Cloud Corp',
    description: 'Seeking a DevOps engineer to manage our cloud infrastructure and CI/CD pipelines.',
    requirements: ['Kubernetes', 'AWS', 'Terraform', 'Jenkins', 'Python'],
  },
];

export function setup() {
  const loginResult = login();

  if (!loginResult.success) {
    console.error('Setup failed: Could not authenticate');
    return { token: null };
  }

  return {
    token: loginResult.token,
    startTime: new Date().toISOString(),
  };
}

export default function (data) {
  if (!data.token) {
    console.error('No authentication token available');
    return;
  }

  const headers = getAuthHeaders(data.token);

  // 시나리오 1: 일반 사용자 플로우 (70%)
  // 시나리오 2: JD 분석 플로우 (20%)
  // 시나리오 3: 면접 세션 플로우 (10%)

  const scenario = Math.random();

  if (scenario < 0.7) {
    normalUserFlow(headers);
  } else if (scenario < 0.9) {
    jdAnalysisFlow(headers);
  } else {
    interviewSessionFlow(headers);
  }
}

function normalUserFlow(headers) {
  group('Normal User Flow', function () {
    // 프로필 조회
    const profileRes = http.get(`${config.services.user}/api/v1/users/me`, {
      headers,
      tags: { name: 'get-profile' },
    });

    check(profileRes, {
      'profile status 200': (r) => r.status === 200,
    }) || errorRate.add(1);

    sleep(1);

    // 질문 목록 조회
    const page = Math.floor(Math.random() * 5);
    const questionsRes = http.get(
      `${config.services.question}/api/v1/questions?page=${page}&size=20`,
      { headers, tags: { name: 'get-questions' } }
    );

    check(questionsRes, {
      'questions status 200': (r) => r.status === 200 || r.status === 204,
    }) || errorRate.add(1);

    sleep(2);

    // 카테고리별 질문 검색
    const categories = ['backend', 'frontend', 'devops', 'system-design'];
    const category = categories[Math.floor(Math.random() * categories.length)];

    const searchRes = http.get(
      `${config.services.question}/api/v1/questions/search?category=${category}&page=0&size=10`,
      { headers, tags: { name: 'search-questions' } }
    );

    const searchTime = searchRes.timings.duration;
    questionSearchTime.add(searchTime);

    check(searchRes, {
      'search status 200': (r) => r.status === 200 || r.status === 204,
      'search response time < 1s': (r) => r.timings.duration < 1000,
    }) || errorRate.add(1);

    sleep(1);
  });
}

function jdAnalysisFlow(headers) {
  group('JD Analysis Flow', function () {
    // JD 선택
    const jd = jobDescriptions[Math.floor(Math.random() * jobDescriptions.length)];

    // JD 분석 요청 (LLM 호출)
    const startTime = Date.now();
    const analyzeRes = http.post(
      `${config.services.question}/api/v1/jd/analyze`,
      JSON.stringify(jd),
      {
        headers,
        tags: { name: 'analyze-jd' },
        timeout: '60s',
      }
    );

    const analysisTime = Date.now() - startTime;
    jdAnalysisTime.add(analysisTime);

    check(analyzeRes, {
      'JD analysis status 200': (r) => r.status === 200,
      'JD analysis has questions': (r) => {
        try {
          const body = r.json();
          return body.questions && body.questions.length > 0;
        } catch (e) {
          return false;
        }
      },
    }) || errorRate.add(1);

    sleep(3);

    // RAG 기반 유사 질문 검색
    if (analyzeRes.status === 200) {
      const skills = jd.requirements.slice(0, 3).join(',');

      const ragRes = http.get(
        `${config.services.question}/api/v1/questions/similar?skills=${encodeURIComponent(skills)}&limit=5`,
        {
          headers,
          tags: { name: 'rag-search' },
        }
      );

      check(ragRes, {
        'RAG search status 200': (r) => r.status === 200 || r.status === 204,
      }) || errorRate.add(1);
    }

    sleep(2);
  });
}

function interviewSessionFlow(headers) {
  group('Interview Session Flow', function () {
    // 새 면접 세션 생성
    const sessionRes = http.post(
      `${config.services.interview}/api/v1/sessions`,
      JSON.stringify({
        title: `Load Test Session ${Date.now()}`,
        type: 'TECHNICAL',
        duration: 30,
      }),
      {
        headers,
        tags: { name: 'create-session' },
      }
    );

    let sessionId = null;
    if (sessionRes.status === 201 || sessionRes.status === 200) {
      try {
        sessionId = sessionRes.json('id');
      } catch (e) {
        // ignore
      }
    }

    check(sessionRes, {
      'session created': (r) => r.status === 201 || r.status === 200,
    }) || errorRate.add(1);

    sleep(2);

    // 세션에 질문 추가
    if (sessionId) {
      const addQuestionRes = http.post(
        `${config.services.interview}/api/v1/sessions/${sessionId}/questions`,
        JSON.stringify({
          questionId: Math.floor(Math.random() * 100) + 1,
        }),
        {
          headers,
          tags: { name: 'add-question-to-session' },
        }
      );

      check(addQuestionRes, {
        'question added': (r) => r.status === 200 || r.status === 201,
      });

      sleep(1);

      // 세션 조회
      const getSessionRes = http.get(
        `${config.services.interview}/api/v1/sessions/${sessionId}`,
        {
          headers,
          tags: { name: 'get-session' },
        }
      );

      check(getSessionRes, {
        'session retrieved': (r) => r.status === 200,
      });
    }

    sleep(2);
  });
}

export function teardown(data) {
  console.log(`Load test completed. Started at: ${data.startTime}`);
}
