/**
 * k6 테스트 공통 설정
 */

// 환경 변수 기반 설정
export const config = {
  // 기본 URL
  baseUrl: __ENV.BASE_URL || 'http://localhost:8080',

  // 서비스별 URL
  services: {
    gateway: __ENV.GATEWAY_URL || 'http://localhost:8080',
    user: __ENV.USER_SERVICE_URL || 'http://localhost:8081',
    question: __ENV.QUESTION_SERVICE_URL || 'http://localhost:8082',
    interview: __ENV.INTERVIEW_SERVICE_URL || 'http://localhost:8083',
    feedback: __ENV.FEEDBACK_SERVICE_URL || 'http://localhost:8084',
  },

  // 테스트 사용자 정보
  testUser: {
    email: __ENV.TEST_USER_EMAIL || 'test@example.com',
    password: __ENV.TEST_USER_PASSWORD || 'Test1234!',
  },

  // InfluxDB 출력 설정
  influxdb: {
    url: __ENV.K6_INFLUXDB_URL || 'http://localhost:8086',
    token: __ENV.K6_INFLUXDB_TOKEN || 'my-super-secret-auth-token',
    organization: __ENV.K6_INFLUXDB_ORG || 'interview-coach',
    bucket: __ENV.K6_INFLUXDB_BUCKET || 'k6',
  },
};

// 공통 HTTP 옵션
export const httpOptions = {
  headers: {
    'Content-Type': 'application/json',
    'Accept': 'application/json',
  },
  timeout: '30s',
};

// 성능 임계값 (thresholds)
export const thresholds = {
  // 전체 API
  default: {
    http_req_duration: ['p(95)<500', 'p(99)<1000'],
    http_req_failed: ['rate<0.01'],
  },

  // 인증 관련 (빠른 응답 기대)
  auth: {
    http_req_duration: ['p(95)<200', 'p(99)<500'],
    http_req_failed: ['rate<0.01'],
  },

  // LLM 관련 (긴 응답 시간 허용)
  llm: {
    http_req_duration: ['p(95)<10000', 'p(99)<30000'],
    http_req_failed: ['rate<0.05'],
  },

  // RAG 검색
  rag: {
    http_req_duration: ['p(95)<2000', 'p(99)<5000'],
    http_req_failed: ['rate<0.02'],
  },

  // SSE 스트리밍
  sse: {
    http_req_duration: ['p(95)<60000'],
    http_req_failed: ['rate<0.05'],
  },
};

// VU 시나리오 프리셋
export const vuScenarios = {
  smoke: {
    vus: 1,
    duration: '1m',
  },
  load: {
    stages: [
      { duration: '2m', target: 50 },
      { duration: '5m', target: 50 },
      { duration: '2m', target: 100 },
      { duration: '5m', target: 100 },
      { duration: '2m', target: 0 },
    ],
  },
  stress: {
    stages: [
      { duration: '2m', target: 100 },
      { duration: '5m', target: 200 },
      { duration: '5m', target: 300 },
      { duration: '5m', target: 500 },
      { duration: '5m', target: 0 },
    ],
  },
  spike: {
    stages: [
      { duration: '1m', target: 10 },
      { duration: '30s', target: 500 },
      { duration: '1m', target: 500 },
      { duration: '30s', target: 10 },
      { duration: '2m', target: 10 },
    ],
  },
  soak: {
    stages: [
      { duration: '5m', target: 100 },
      { duration: '4h', target: 100 },
      { duration: '5m', target: 0 },
    ],
  },
};

export default config;
