import axios, { AxiosError, InternalAxiosRequestConfig } from 'axios';
import { useAuthStore } from '@/stores/auth';

const API_BASE_URL = process.env.NEXT_PUBLIC_API_URL || 'http://localhost:8080';

export const api = axios.create({
  baseURL: API_BASE_URL,
  headers: {
    'Content-Type': 'application/json',
  },
});

// Request interceptor - add auth token
api.interceptors.request.use(
  (config: InternalAxiosRequestConfig) => {
    const token = useAuthStore.getState().accessToken;
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
  },
  (error) => Promise.reject(error)
);

// Response interceptor - handle token refresh
api.interceptors.response.use(
  (response) => response,
  async (error: AxiosError) => {
    const originalRequest = error.config;

    if (error.response?.status === 401 && originalRequest) {
      const refreshToken = useAuthStore.getState().refreshToken;

      if (refreshToken) {
        try {
          const response = await axios.post(`${API_BASE_URL}/api/v1/auth/refresh`, {
            refreshToken,
          });

          const { accessToken, refreshToken: newRefreshToken } = response.data;
          useAuthStore.getState().setTokens(accessToken, newRefreshToken);

          originalRequest.headers.Authorization = `Bearer ${accessToken}`;
          return api(originalRequest);
        } catch {
          useAuthStore.getState().logout();
          window.location.href = '/login';
        }
      } else {
        useAuthStore.getState().logout();
        window.location.href = '/login';
      }
    }

    return Promise.reject(error);
  }
);

// Auth API
export const authApi = {
  signup: (data: { email: string; password: string; nickname: string }) =>
    api.post('/api/v1/auth/signup', data),

  login: (data: { email: string; password: string }) =>
    api.post('/api/v1/auth/login', data),

  refresh: (refreshToken: string) =>
    api.post('/api/v1/auth/refresh', { refreshToken }),
};

// JD API
export const jdApi = {
  create: (data: { companyName: string; position: string; originalText: string; originalUrl?: string }) =>
    api.post('/api/v1/jd', data),

  list: () => api.get('/api/v1/jd'),

  get: (id: number) => api.get(`/api/v1/jd/${id}`),

  analyze: (id: number) => api.post(`/api/v1/jd/${id}/analyze`),

  delete: (id: number) => api.delete(`/api/v1/jd/${id}`),
};

// Question API
export const questionApi = {
  generate: (data: {
    jdId: number;
    questionType: string;
    count: number;
    difficulty: number;
    weakCategories?: Array<{ category: string; score: number }>;
  }) => api.post('/api/v1/questions/generate', data),

  listByJd: (jdId: number) => api.get(`/api/v1/questions/jd/${jdId}`),
};

// Interview API
export const interviewApi = {
  start: (data: { jdId: number; questions: Array<{ questionType: string; questionText: string }>; interviewType: string }) =>
    api.post('/api/v1/interviews', data),

  list: () => api.get('/api/v1/interviews'),

  // [A-1] 면접 기록 검색
  search: (keyword: string) => api.get(`/api/v1/interviews/search`, { params: { keyword } }),

  get: (id: number) => api.get(`/api/v1/interviews/${id}`),

  submitAnswer: (id: number, data: { questionOrder: number; answerText: string }) =>
    api.post(`/api/v1/interviews/${id}/answer`, data),

  updateFeedback: (id: number, questionOrder: number, feedback: { score: number; strengths: string[]; improvements: string[]; tips?: string[] }) =>
    api.put(`/api/v1/interviews/${id}/qna/${questionOrder}/feedback`, feedback),

  complete: (id: number) => api.post(`/api/v1/interviews/${id}/complete`),

  // [A-2] 면접 일시정지/재개
  pause: (id: number) => api.patch(`/api/v1/interviews/${id}/pause`),
  resume: (id: number) => api.patch(`/api/v1/interviews/${id}/resume`),

  addFollowUp: (id: number, data: { parentQnaId: number; questionText: string; followUpDepth: number; focusArea?: string }) =>
    api.post(`/api/v1/interviews/${id}/follow-up`, data),
};

// Feedback API
export const feedbackApi = {
  // Legacy GET URL (for short answers)
  streamUrl: (sessionId: number, options?: { token?: string; qnaId?: number; question?: string; answer?: string }) => {
    const params = new URLSearchParams();
    if (options?.token) params.append('token', options.token);
    if (options?.qnaId) params.append('qnaId', options.qnaId.toString());
    if (options?.question) params.append('question', options.question);
    if (options?.answer) params.append('answer', options.answer);
    const queryString = params.toString();
    return `${API_BASE_URL}/api/v1/feedback/session/${sessionId}/stream${queryString ? `?${queryString}` : ''}`;
  },

  // POST stream for long answers (no URL length limit)
  streamPost: async (
    sessionId: number,
    data: { qnaId?: number; question?: string; answer?: string; followUpDepth?: number },
    onFeedback: (data: Record<string, unknown>) => void,
    onComplete: () => void | Promise<void>,
    onError: (error: Error) => void
  ) => {
    const token = useAuthStore.getState().accessToken;
    let feedbackReceived = false;
    let completeCalled = false;
    let feedbackData: Record<string, unknown> | null = null;

    try {
      const response = await fetch(`${API_BASE_URL}/api/v1/feedback/session/${sessionId}/stream`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          ...(token ? { 'Authorization': `Bearer ${token}` } : {}),
        },
        body: JSON.stringify(data),
      });

      if (!response.ok) {
        throw new Error(`HTTP error! status: ${response.status}`);
      }

      const reader = response.body?.getReader();
      if (!reader) {
        throw new Error('No response body');
      }

      const decoder = new TextDecoder();
      let buffer = '';
      let currentEventType = '';
      let currentData = '';

      while (true) {
        const { done, value } = await reader.read();
        if (done) break;

        buffer += decoder.decode(value, { stream: true });

        // SSE messages are separated by double newlines
        const messages = buffer.split('\n\n');
        buffer = messages.pop() || ''; // Keep incomplete message in buffer

        for (const message of messages) {
          if (!message.trim()) continue;

          const lines = message.split('\n');
          currentEventType = '';
          currentData = '';

          for (const line of lines) {
            if (line.startsWith('event:')) {
              currentEventType = line.substring(6).trim();
            } else if (line.startsWith('data:')) {
              currentData += line.substring(5);
            } else if (currentData && !line.startsWith('event:') && !line.startsWith(':')) {
              // Continuation of data (data split across chunks)
              currentData += line;
            }
          }

          if (currentData) {
            try {
              const parsed = JSON.parse(currentData.trim());
              if (currentEventType === 'feedback') {
                feedbackReceived = true;
                feedbackData = parsed;
                onFeedback(parsed);
              } else if (currentEventType === 'complete' && !completeCalled) {
                completeCalled = true;
                await Promise.resolve(onComplete());
                return;
              }
            } catch {
              // Ignore parse errors
            }
          }
        }
      }
      // Call onComplete if we got feedback but stream ended without complete event
      if (feedbackReceived && !completeCalled) {
        completeCalled = true;
        await Promise.resolve(onComplete());
      }
    } catch (error) {
      // Only call onError if we didn't successfully receive feedback
      if (!feedbackReceived || !feedbackData) {
        onError(error instanceof Error ? error : new Error(String(error)));
      } else {
        // We got feedback, so call onComplete even if there was an error after
        if (!completeCalled) {
          completeCalled = true;
          try {
            await Promise.resolve(onComplete());
          } catch {
            // Ignore errors in onComplete
          }
        }
      }
    }
  },
};

// Statistics API
export const statisticsApi = {
  get: () => api.get('/api/v1/statistics'),

  record: (data: { skillCategory: string; isCorrect: boolean; weakPoint?: string; score?: number }) =>
    api.post('/api/v1/statistics/record', data),
};

// User/Profile API
export const userApi = {
  getProfile: () => api.get('/api/v1/users/me'),

  updateProfile: (data: { nickname?: string; targetPosition?: string; experienceYears?: number }) =>
    api.put('/api/v1/users/me', data),
};

export default api;
