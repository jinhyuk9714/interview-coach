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
  generate: (data: { jdId: number; questionType: string; count: number; difficulty: number }) =>
    api.post('/api/v1/questions/generate', data),

  listByJd: (jdId: number) => api.get(`/api/v1/questions/jd/${jdId}`),
};

// Interview API
export const interviewApi = {
  start: (data: { jdId: number; questions: Array<{ questionType: string; questionText: string }>; interviewType: string }) =>
    api.post('/api/v1/interviews', data),

  list: () => api.get('/api/v1/interviews'),

  get: (id: number) => api.get(`/api/v1/interviews/${id}`),

  submitAnswer: (id: number, data: { questionOrder: number; answerText: string }) =>
    api.post(`/api/v1/interviews/${id}/answer`, data),

  complete: (id: number) => api.post(`/api/v1/interviews/${id}/complete`),
};

// Feedback API
export const feedbackApi = {
  streamUrl: (sessionId: number, options?: { token?: string; question?: string; answer?: string }) => {
    const params = new URLSearchParams();
    if (options?.token) params.append('token', options.token);
    if (options?.question) params.append('question', options.question);
    if (options?.answer) params.append('answer', options.answer);
    const queryString = params.toString();
    return `${API_BASE_URL}/api/v1/feedback/session/${sessionId}/stream${queryString ? `?${queryString}` : ''}`;
  },
};

// Statistics API
export const statisticsApi = {
  get: () => api.get('/api/v1/statistics'),
};

export default api;
