import http from 'k6/http';
import { check, sleep } from 'k6';
import { Counter, Trend } from 'k6/metrics';
import { getAuthHeaders } from './auth.js';

/**
 * SSE (Server-Sent Events) 스트리밍 테스트 헬퍼
 */

// SSE 관련 메트릭
export const sseMetrics = {
  eventsReceived: new Counter('sse_events_received'),
  connectionTime: new Trend('sse_connection_time'),
  firstEventTime: new Trend('sse_first_event_time'),
  totalStreamTime: new Trend('sse_total_stream_time'),
  bytesReceived: new Counter('sse_bytes_received'),
};

/**
 * SSE 스트림 연결 및 이벤트 수신
 *
 * k6는 네이티브 SSE 지원이 제한적이므로,
 * 청크 응답을 처리하는 방식으로 구현
 */
export function connectSSE(url, options = {}) {
  const {
    token = null,
    timeout = 60000,
    expectedEvents = 1,
    onEvent = null,
  } = options;

  const startTime = Date.now();
  const headers = token ? getAuthHeaders(token) : { 'Accept': 'text/event-stream' };

  const response = http.get(url, {
    headers,
    timeout: `${timeout}ms`,
    tags: { name: options.tagName || 'sse-stream' },
  });

  const connectionTime = Date.now() - startTime;
  sseMetrics.connectionTime.add(connectionTime);

  // 응답 검증
  const success = check(response, {
    'SSE connection successful': (r) => r.status === 200,
    'Content-Type is event-stream': (r) =>
      r.headers['Content-Type'] &&
      r.headers['Content-Type'].includes('text/event-stream'),
  });

  if (!success) {
    return {
      success: false,
      events: [],
      connectionTime,
      response,
    };
  }

  // SSE 이벤트 파싱
  const events = parseSSEEvents(response.body);

  // 메트릭 기록
  sseMetrics.eventsReceived.add(events.length);
  sseMetrics.bytesReceived.add(response.body ? response.body.length : 0);
  sseMetrics.totalStreamTime.add(Date.now() - startTime);

  // 첫 번째 이벤트 시간 (있는 경우)
  if (events.length > 0 && events[0].timestamp) {
    sseMetrics.firstEventTime.add(events[0].timestamp - startTime);
  }

  // 콜백 실행
  if (onEvent && typeof onEvent === 'function') {
    events.forEach(event => onEvent(event));
  }

  return {
    success: true,
    events,
    connectionTime,
    response,
  };
}

/**
 * SSE 이벤트 문자열 파싱
 */
export function parseSSEEvents(body) {
  if (!body) return [];

  const events = [];
  const lines = body.split('\n');
  let currentEvent = { data: [], event: null, id: null, timestamp: Date.now() };

  for (const line of lines) {
    if (line.startsWith('data:')) {
      currentEvent.data.push(line.substring(5).trim());
    } else if (line.startsWith('event:')) {
      currentEvent.event = line.substring(6).trim();
    } else if (line.startsWith('id:')) {
      currentEvent.id = line.substring(3).trim();
    } else if (line === '' && currentEvent.data.length > 0) {
      // 빈 줄은 이벤트 구분자
      events.push({
        ...currentEvent,
        data: currentEvent.data.join('\n'),
      });
      currentEvent = { data: [], event: null, id: null, timestamp: Date.now() };
    }
  }

  // 마지막 이벤트 처리
  if (currentEvent.data.length > 0) {
    events.push({
      ...currentEvent,
      data: currentEvent.data.join('\n'),
    });
  }

  return events;
}

/**
 * SSE 스트리밍 피드백 테스트
 * (feedback-service용)
 */
export function streamFeedback(answerId, token, options = {}) {
  const { config } = require('./config.js');
  const url = `${config.services.feedback}/api/v1/feedback/stream/${answerId}`;

  return connectSSE(url, {
    token,
    tagName: 'feedback-stream',
    ...options,
  });
}

/**
 * SSE 연결 재시도 로직
 */
export function connectSSEWithRetry(url, options = {}) {
  const { maxRetries = 3, retryDelay = 1000 } = options;

  let attempt = 0;
  let result;

  while (attempt < maxRetries) {
    result = connectSSE(url, options);

    if (result.success) {
      return result;
    }

    attempt++;
    if (attempt < maxRetries) {
      sleep(retryDelay / 1000);
    }
  }

  return result;
}

/**
 * 장시간 SSE 연결 테스트
 * (연결 안정성 테스트용)
 */
export function longRunningSSETest(url, options = {}) {
  const {
    duration = 60000, // 기본 1분
    token = null,
    checkInterval = 5000,
  } = options;

  const startTime = Date.now();
  const results = {
    totalEvents: 0,
    reconnections: 0,
    errors: [],
    connectionDrops: 0,
  };

  while (Date.now() - startTime < duration) {
    const result = connectSSE(url, {
      token,
      timeout: checkInterval + 5000,
    });

    if (result.success) {
      results.totalEvents += result.events.length;
    } else {
      results.connectionDrops++;
      results.errors.push({
        timestamp: Date.now(),
        status: result.response ? result.response.status : 'unknown',
      });
    }

    sleep(checkInterval / 1000);
  }

  return results;
}

export default {
  connectSSE,
  parseSSEEvents,
  streamFeedback,
  connectSSEWithRetry,
  longRunningSSETest,
  sseMetrics,
};
