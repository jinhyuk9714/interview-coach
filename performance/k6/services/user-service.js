import http from 'k6/http';
import { check, sleep, group } from 'k6';
import { Rate, Trend, Counter } from 'k6/metrics';
import { randomString, randomIntBetween } from 'https://jslib.k6.io/k6-utils/1.4.0/index.js';
import { config, httpOptions } from '../lib/config.js';

/**
 * User Service 성능 테스트
 *
 * 테스트 대상:
 * - 회원가입
 * - 로그인
 * - JWT 토큰 발급/갱신
 * - 프로필 조회/수정
 */

export const options = {
  scenarios: {
    // 회원가입 시나리오
    registration: {
      executor: 'constant-arrival-rate',
      rate: 5, // 초당 5개 요청
      timeUnit: '1s',
      duration: '5m',
      preAllocatedVUs: 10,
      maxVUs: 50,
      tags: { scenario: 'registration' },
    },
    // 로그인 시나리오
    login: {
      executor: 'ramping-vus',
      startVUs: 0,
      stages: [
        { duration: '2m', target: 30 },
        { duration: '3m', target: 30 },
        { duration: '2m', target: 50 },
        { duration: '3m', target: 50 },
        { duration: '2m', target: 0 },
      ],
      tags: { scenario: 'login' },
    },
    // 프로필 조회 시나리오
    profile: {
      executor: 'constant-vus',
      vus: 20,
      duration: '10m',
      tags: { scenario: 'profile' },
    },
  },

  thresholds: {
    'http_req_duration{scenario:registration}': ['p(95)<500'],
    'http_req_duration{scenario:login}': ['p(95)<200'],
    'http_req_duration{scenario:profile}': ['p(95)<100'],
    'http_req_failed{scenario:registration}': ['rate<0.05'],
    'http_req_failed{scenario:login}': ['rate<0.01'],
    'http_req_failed{scenario:profile}': ['rate<0.01'],
  },
};

// 커스텀 메트릭
const registrationTime = new Trend('registration_time');
const loginTime = new Trend('login_time');
const tokenRefreshTime = new Trend('token_refresh_time');
const profileLoadTime = new Trend('profile_load_time');
const registrationSuccess = new Rate('registration_success');
const loginSuccess = new Rate('login_success');
const duplicateEmails = new Counter('duplicate_email_errors');

// 생성된 사용자 저장 (로그인 테스트용)
const createdUsers = [];

export function setup() {
  // 테스트용 사용자 미리 생성
  const users = [];
  for (let i = 0; i < 10; i++) {
    const email = `loadtest_${randomString(8)}@test.com`;
    const password = 'Test1234!';

    const res = http.post(
      `${config.services.user}/api/v1/auth/register`,
      JSON.stringify({
        email,
        password,
        name: `Test User ${i}`,
      }),
      { headers: httpOptions.headers }
    );

    if (res.status === 201 || res.status === 200) {
      users.push({ email, password });
    }
  }

  return { preCreatedUsers: users };
}

export default function (data) {
  const scenario = __ENV.K6_SCENARIO_NAME;

  switch (scenario) {
    case 'registration':
      testRegistration();
      break;
    case 'login':
      testLogin(data.preCreatedUsers);
      break;
    case 'profile':
      testProfile(data.preCreatedUsers);
      break;
    default:
      // 기본: 모든 테스트 순차 실행
      testRegistration();
      testLogin(data.preCreatedUsers);
      testProfile(data.preCreatedUsers);
  }
}

function testRegistration() {
  group('Registration Flow', function () {
    const email = `user_${randomString(12)}_${Date.now()}@test.com`;
    const password = 'Test1234!';

    const startTime = Date.now();
    const res = http.post(
      `${config.services.user}/api/v1/auth/register`,
      JSON.stringify({
        email,
        password,
        name: `Test User ${randomIntBetween(1, 1000)}`,
      }),
      {
        headers: httpOptions.headers,
        tags: { name: 'register' },
      }
    );

    const duration = Date.now() - startTime;
    registrationTime.add(duration);

    const success = check(res, {
      'registration status 2xx': (r) => r.status >= 200 && r.status < 300,
      'registration has user id': (r) => {
        try {
          return r.json('id') !== undefined;
        } catch {
          return false;
        }
      },
    });

    registrationSuccess.add(success ? 1 : 0);

    // 중복 이메일 에러 추적
    if (res.status === 409) {
      duplicateEmails.add(1);
    }

    if (success) {
      createdUsers.push({ email, password });
    }

    sleep(randomIntBetween(1, 3));
  });
}

function testLogin(preCreatedUsers) {
  group('Login Flow', function () {
    // 미리 생성된 사용자로 로그인
    const users = preCreatedUsers && preCreatedUsers.length > 0
      ? preCreatedUsers
      : [{ email: config.testUser.email, password: config.testUser.password }];

    const user = users[Math.floor(Math.random() * users.length)];

    const startTime = Date.now();
    const res = http.post(
      `${config.services.user}/api/v1/auth/login`,
      JSON.stringify({
        email: user.email,
        password: user.password,
      }),
      {
        headers: httpOptions.headers,
        tags: { name: 'login' },
      }
    );

    const duration = Date.now() - startTime;
    loginTime.add(duration);

    const success = check(res, {
      'login status 200': (r) => r.status === 200,
      'login has access token': (r) => {
        try {
          return r.json('accessToken') !== undefined;
        } catch {
          return false;
        }
      },
      'login has refresh token': (r) => {
        try {
          return r.json('refreshToken') !== undefined;
        } catch {
          return false;
        }
      },
    });

    loginSuccess.add(success ? 1 : 0);

    // 토큰 갱신 테스트
    if (success) {
      sleep(1);
      testTokenRefresh(res.json('refreshToken'));
    }

    sleep(randomIntBetween(2, 5));
  });
}

function testTokenRefresh(refreshToken) {
  const startTime = Date.now();
  const res = http.post(
    `${config.services.user}/api/v1/auth/refresh`,
    JSON.stringify({ refreshToken }),
    {
      headers: httpOptions.headers,
      tags: { name: 'refresh-token' },
    }
  );

  const duration = Date.now() - startTime;
  tokenRefreshTime.add(duration);

  check(res, {
    'token refresh status 200': (r) => r.status === 200,
    'refresh returns new access token': (r) => {
      try {
        return r.json('accessToken') !== undefined;
      } catch {
        return false;
      }
    },
  });
}

function testProfile(preCreatedUsers) {
  group('Profile Operations', function () {
    // 먼저 로그인하여 토큰 획득
    const users = preCreatedUsers && preCreatedUsers.length > 0
      ? preCreatedUsers
      : [{ email: config.testUser.email, password: config.testUser.password }];

    const user = users[Math.floor(Math.random() * users.length)];

    const loginRes = http.post(
      `${config.services.user}/api/v1/auth/login`,
      JSON.stringify({
        email: user.email,
        password: user.password,
      }),
      { headers: httpOptions.headers }
    );

    if (loginRes.status !== 200) {
      return;
    }

    const token = loginRes.json('accessToken');
    const authHeaders = {
      ...httpOptions.headers,
      'Authorization': `Bearer ${token}`,
    };

    // 프로필 조회
    const startTime = Date.now();
    const profileRes = http.get(`${config.services.user}/api/v1/users/me`, {
      headers: authHeaders,
      tags: { name: 'get-profile' },
    });

    const duration = Date.now() - startTime;
    profileLoadTime.add(duration);

    check(profileRes, {
      'profile status 200': (r) => r.status === 200,
      'profile has email': (r) => {
        try {
          return r.json('email') !== undefined;
        } catch {
          return false;
        }
      },
    });

    sleep(randomIntBetween(1, 3));

    // 프로필 수정 (10% 확률)
    if (Math.random() < 0.1) {
      const updateRes = http.patch(
        `${config.services.user}/api/v1/users/me`,
        JSON.stringify({
          name: `Updated User ${Date.now()}`,
        }),
        {
          headers: authHeaders,
          tags: { name: 'update-profile' },
        }
      );

      check(updateRes, {
        'profile update status 200': (r) => r.status === 200,
      });
    }

    sleep(randomIntBetween(2, 5));
  });
}

export function teardown(data) {
  console.log('User Service test completed');
  console.log(`Pre-created users: ${data.preCreatedUsers ? data.preCreatedUsers.length : 0}`);
}
