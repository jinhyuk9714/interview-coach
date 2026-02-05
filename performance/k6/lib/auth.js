import http from 'k6/http';
import { check } from 'k6';
import { config, httpOptions } from './config.js';

/**
 * JWT 인증 헬퍼 모듈
 */

// JWT 토큰 저장소
let cachedToken = null;
let tokenExpiry = null;

/**
 * 로그인하여 JWT 토큰 획득
 */
export function login(email, password) {
  const url = `${config.services.user}/api/v1/auth/login`;
  const payload = JSON.stringify({
    email: email || config.testUser.email,
    password: password || config.testUser.password,
  });

  const response = http.post(url, payload, {
    headers: httpOptions.headers,
    tags: { name: 'login' },
  });

  const success = check(response, {
    'login successful': (r) => r.status === 200,
    'has access token': (r) => r.json('accessToken') !== undefined,
  });

  if (success) {
    cachedToken = response.json('accessToken');
    // JWT 만료 시간 추출 (기본 1시간으로 가정)
    tokenExpiry = Date.now() + 55 * 60 * 1000; // 5분 여유
  }

  return {
    success,
    token: cachedToken,
    response,
  };
}

/**
 * 회원가입
 */
export function register(userData) {
  const url = `${config.services.user}/api/v1/auth/register`;
  const payload = JSON.stringify({
    email: userData.email,
    password: userData.password,
    name: userData.name || 'Test User',
  });

  const response = http.post(url, payload, {
    headers: httpOptions.headers,
    tags: { name: 'register' },
  });

  check(response, {
    'registration successful': (r) => r.status === 201 || r.status === 200,
  });

  return response;
}

/**
 * 인증 헤더 생성
 */
export function getAuthHeaders(token) {
  return {
    ...httpOptions.headers,
    'Authorization': `Bearer ${token || cachedToken}`,
  };
}

/**
 * 토큰 유효성 확인 및 필요시 재발급
 */
export function ensureValidToken() {
  if (!cachedToken || !tokenExpiry || Date.now() > tokenExpiry) {
    const result = login();
    if (!result.success) {
      console.error('Failed to obtain valid token');
    }
  }
  return cachedToken;
}

/**
 * 토큰 갱신
 */
export function refreshToken(token) {
  const url = `${config.services.user}/api/v1/auth/refresh`;

  const response = http.post(url, null, {
    headers: getAuthHeaders(token),
    tags: { name: 'refresh-token' },
  });

  const success = check(response, {
    'token refresh successful': (r) => r.status === 200,
    'has new access token': (r) => r.json('accessToken') !== undefined,
  });

  if (success) {
    cachedToken = response.json('accessToken');
    tokenExpiry = Date.now() + 55 * 60 * 1000;
  }

  return {
    success,
    token: cachedToken,
    response,
  };
}

/**
 * 인증된 HTTP 요청 수행
 */
export function authenticatedRequest(method, url, body, options = {}) {
  const token = ensureValidToken();
  const headers = getAuthHeaders(token);

  const requestOptions = {
    headers: { ...headers, ...(options.headers || {}) },
    tags: options.tags || {},
    timeout: options.timeout || httpOptions.timeout,
  };

  let response;
  switch (method.toUpperCase()) {
    case 'GET':
      response = http.get(url, requestOptions);
      break;
    case 'POST':
      response = http.post(url, body ? JSON.stringify(body) : null, requestOptions);
      break;
    case 'PUT':
      response = http.put(url, body ? JSON.stringify(body) : null, requestOptions);
      break;
    case 'DELETE':
      response = http.del(url, body ? JSON.stringify(body) : null, requestOptions);
      break;
    case 'PATCH':
      response = http.patch(url, body ? JSON.stringify(body) : null, requestOptions);
      break;
    default:
      throw new Error(`Unsupported HTTP method: ${method}`);
  }

  // 401 응답 시 토큰 재발급 후 재시도
  if (response.status === 401) {
    const refreshResult = refreshToken();
    if (refreshResult.success) {
      requestOptions.headers = getAuthHeaders(refreshResult.token);
      switch (method.toUpperCase()) {
        case 'GET':
          response = http.get(url, requestOptions);
          break;
        case 'POST':
          response = http.post(url, body ? JSON.stringify(body) : null, requestOptions);
          break;
        case 'PUT':
          response = http.put(url, body ? JSON.stringify(body) : null, requestOptions);
          break;
        case 'DELETE':
          response = http.del(url, body ? JSON.stringify(body) : null, requestOptions);
          break;
        case 'PATCH':
          response = http.patch(url, body ? JSON.stringify(body) : null, requestOptions);
          break;
      }
    }
  }

  return response;
}

export default {
  login,
  register,
  getAuthHeaders,
  ensureValidToken,
  refreshToken,
  authenticatedRequest,
};
