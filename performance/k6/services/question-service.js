import http from 'k6/http';
import { check, sleep, group } from 'k6';
import { Rate, Trend, Counter } from 'k6/metrics';
import { randomIntBetween } from 'https://jslib.k6.io/k6-utils/1.4.0/index.js';
import { config, httpOptions } from '../lib/config.js';
import { login, getAuthHeaders } from '../lib/auth.js';

/**
 * Question Service 성능 테스트
 *
 * 테스트 대상:
 * - JD 분석 (LLM 호출)
 * - RAG 기반 질문 검색
 * - 질문 CRUD
 * - 캐시 히트율 측정
 */

export const options = {
  scenarios: {
    // JD 분석 (LLM) - 느린 응답 예상
    jd_analysis: {
      executor: 'constant-arrival-rate',
      rate: 2, // 초당 2개 요청 (LLM 비용 고려)
      timeUnit: '1s',
      duration: '10m',
      preAllocatedVUs: 20,
      maxVUs: 100,
      tags: { scenario: 'jd_analysis' },
    },
    // RAG 검색 - 중간 속도
    rag_search: {
      executor: 'ramping-vus',
      startVUs: 5,
      stages: [
        { duration: '2m', target: 20 },
        { duration: '5m', target: 30 },
        { duration: '3m', target: 0 },
      ],
      tags: { scenario: 'rag_search' },
    },
    // 질문 조회 - 빠른 응답
    questions_crud: {
      executor: 'constant-vus',
      vus: 30,
      duration: '10m',
      tags: { scenario: 'questions_crud' },
    },
  },

  thresholds: {
    // LLM 분석은 긴 응답 시간 허용
    'http_req_duration{scenario:jd_analysis}': ['p(95)<30000', 'p(99)<60000'],
    'http_req_failed{scenario:jd_analysis}': ['rate<0.1'],

    // RAG 검색
    'http_req_duration{scenario:rag_search}': ['p(95)<2000', 'p(99)<5000'],
    'http_req_failed{scenario:rag_search}': ['rate<0.05'],

    // 일반 CRUD
    'http_req_duration{scenario:questions_crud}': ['p(95)<500'],
    'http_req_failed{scenario:questions_crud}': ['rate<0.01'],
  },
};

// 커스텀 메트릭
const jdAnalysisTime = new Trend('jd_analysis_duration');
const ragSearchTime = new Trend('rag_search_duration');
const questionFetchTime = new Trend('question_fetch_duration');
const cacheHitRate = new Rate('cache_hit_rate');
const llmTokensUsed = new Counter('llm_tokens_used');
const questionsGenerated = new Counter('questions_generated');

// 테스트 JD 데이터
const jobDescriptions = [
  {
    title: 'Senior Backend Developer',
    company: 'Tech Corp',
    description: `
      We are looking for a Senior Backend Developer to join our team.
      You will be responsible for designing and implementing scalable microservices
      using Java and Spring Boot. Experience with Kubernetes and cloud platforms (AWS/GCP) is required.
      The ideal candidate has strong knowledge of RESTful API design, database optimization,
      and distributed systems.
    `,
    requirements: ['Java', 'Spring Boot', 'PostgreSQL', 'Redis', 'Kubernetes', 'AWS'],
  },
  {
    title: 'ML Platform Engineer',
    company: 'AI Startup',
    description: `
      Join our ML Platform team to build and maintain infrastructure for machine learning workloads.
      You'll work on model serving, feature stores, and MLOps pipelines.
      Strong Python skills and experience with ML frameworks are essential.
    `,
    requirements: ['Python', 'TensorFlow', 'Kubernetes', 'Apache Spark', 'MLflow'],
  },
  {
    title: 'Full Stack Developer',
    company: 'E-commerce Inc',
    description: `
      Looking for a Full Stack Developer to work on our e-commerce platform.
      You'll build features across the entire stack using React, Node.js, and PostgreSQL.
      Experience with payment systems and high-traffic applications is a plus.
    `,
    requirements: ['React', 'Node.js', 'TypeScript', 'PostgreSQL', 'Redis'],
  },
  {
    title: 'DevOps/SRE Engineer',
    company: 'Cloud Services',
    description: `
      We need a DevOps Engineer to improve our deployment pipelines and system reliability.
      You'll manage our Kubernetes clusters, implement monitoring solutions,
      and automate infrastructure using Terraform.
    `,
    requirements: ['Kubernetes', 'Terraform', 'Prometheus', 'AWS', 'Python', 'Go'],
  },
];

export function setup() {
  const loginResult = login();

  return {
    token: loginResult.token,
    startTime: Date.now(),
  };
}

export default function (data) {
  const scenario = __ENV.K6_SCENARIO_NAME;
  const headers = getAuthHeaders(data.token);

  switch (scenario) {
    case 'jd_analysis':
      testJDAnalysis(headers);
      break;
    case 'rag_search':
      testRAGSearch(headers);
      break;
    case 'questions_crud':
      testQuestionsCRUD(headers);
      break;
    default:
      // 가중치 기반 랜덤 실행
      const random = Math.random();
      if (random < 0.1) {
        testJDAnalysis(headers);
      } else if (random < 0.3) {
        testRAGSearch(headers);
      } else {
        testQuestionsCRUD(headers);
      }
  }
}

function testJDAnalysis(headers) {
  group('JD Analysis (LLM)', function () {
    const jd = jobDescriptions[Math.floor(Math.random() * jobDescriptions.length)];

    const startTime = Date.now();
    const res = http.post(
      `${config.services.question}/api/v1/jd/analyze`,
      JSON.stringify({
        title: jd.title,
        company: jd.company,
        description: jd.description,
        requirements: jd.requirements,
      }),
      {
        headers,
        tags: { name: 'jd-analyze' },
        timeout: '120s', // LLM 호출이므로 긴 타임아웃
      }
    );

    const duration = Date.now() - startTime;
    jdAnalysisTime.add(duration);

    const success = check(res, {
      'JD analysis status 200': (r) => r.status === 200,
      'JD analysis has questions': (r) => {
        try {
          const body = r.json();
          if (body.questions && body.questions.length > 0) {
            questionsGenerated.add(body.questions.length);
            return true;
          }
          return false;
        } catch {
          return false;
        }
      },
      'JD analysis response time < 60s': (r) => r.timings.duration < 60000,
    });

    // 토큰 사용량 추적 (응답에 포함된 경우)
    if (res.status === 200) {
      try {
        const body = res.json();
        if (body.tokensUsed) {
          llmTokensUsed.add(body.tokensUsed);
        }
      } catch {
        // ignore
      }
    }

    // LLM 호출 후 충분한 대기 시간
    sleep(randomIntBetween(5, 10));
  });
}

function testRAGSearch(headers) {
  group('RAG Search', function () {
    const skillSets = [
      ['Java', 'Spring Boot', 'microservices'],
      ['Python', 'TensorFlow', 'machine learning'],
      ['React', 'TypeScript', 'frontend'],
      ['Kubernetes', 'Docker', 'DevOps'],
      ['PostgreSQL', 'Redis', 'database'],
      ['AWS', 'GCP', 'cloud'],
    ];

    const skills = skillSets[Math.floor(Math.random() * skillSets.length)];

    const startTime = Date.now();
    const res = http.get(
      `${config.services.question}/api/v1/questions/similar?skills=${encodeURIComponent(skills.join(','))}&limit=10`,
      {
        headers,
        tags: { name: 'rag-search' },
        timeout: '30s',
      }
    );

    const duration = Date.now() - startTime;
    ragSearchTime.add(duration);

    // 캐시 히트 체크 (응답 헤더 또는 응답 시간으로 추정)
    const isCacheHit = res.headers['X-Cache'] === 'HIT' || duration < 100;
    cacheHitRate.add(isCacheHit ? 1 : 0);

    check(res, {
      'RAG search status 200': (r) => r.status === 200 || r.status === 204,
      'RAG search has results': (r) => {
        try {
          const body = r.json();
          return Array.isArray(body) || (body.content && Array.isArray(body.content));
        } catch {
          return r.status === 204; // No content is acceptable
        }
      },
      'RAG search response time < 5s': (r) => r.timings.duration < 5000,
    });

    sleep(randomIntBetween(1, 3));
  });
}

function testQuestionsCRUD(headers) {
  group('Questions CRUD', function () {
    // 질문 목록 조회
    const page = randomIntBetween(0, 10);

    const startTime = Date.now();
    const listRes = http.get(
      `${config.services.question}/api/v1/questions?page=${page}&size=20`,
      {
        headers,
        tags: { name: 'list-questions' },
      }
    );

    const duration = Date.now() - startTime;
    questionFetchTime.add(duration);

    // 캐시 히트 체크
    const isCacheHit = listRes.headers['X-Cache'] === 'HIT' || duration < 50;
    cacheHitRate.add(isCacheHit ? 1 : 0);

    check(listRes, {
      'list questions status 200': (r) => r.status === 200 || r.status === 204,
    });

    sleep(randomIntBetween(1, 2));

    // 카테고리별 검색
    const categories = ['backend', 'frontend', 'devops', 'system-design', 'behavioral'];
    const category = categories[Math.floor(Math.random() * categories.length)];

    const searchRes = http.get(
      `${config.services.question}/api/v1/questions/search?category=${category}&page=0&size=10`,
      {
        headers,
        tags: { name: 'search-by-category' },
      }
    );

    check(searchRes, {
      'search by category status 200': (r) => r.status === 200 || r.status === 204,
      'search response time < 500ms': (r) => r.timings.duration < 500,
    });

    sleep(randomIntBetween(1, 2));

    // 개별 질문 조회 (10% 확률로 상세 조회)
    if (Math.random() < 0.1 && listRes.status === 200) {
      try {
        const questions = listRes.json('content') || listRes.json();
        if (questions && questions.length > 0) {
          const question = questions[Math.floor(Math.random() * questions.length)];
          if (question.id) {
            const detailRes = http.get(
              `${config.services.question}/api/v1/questions/${question.id}`,
              {
                headers,
                tags: { name: 'get-question-detail' },
              }
            );

            check(detailRes, {
              'get question detail status 200': (r) => r.status === 200,
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

export function teardown(data) {
  const totalTime = (Date.now() - data.startTime) / 1000;
  console.log(`Question Service test completed in ${totalTime.toFixed(2)} seconds`);
}
