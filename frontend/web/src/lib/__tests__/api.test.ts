import { describe, it, expect } from 'vitest';
import { api, authApi, jdApi, interviewApi, statisticsApi, userApi } from '../api';

describe('API client', () => {
  it('should create axios instance with correct base URL', () => {
    expect(api.defaults.baseURL).toBe('http://localhost:8080');
  });

  it('should set Content-Type header', () => {
    expect(api.defaults.headers['Content-Type']).toBe('application/json');
  });

  it('should have request interceptor for auth token', () => {
    // Axios interceptors are stored in the handlers array
    expect(api.interceptors.request.handlers.length).toBeGreaterThan(0);
  });

  it('should have response interceptor for token refresh', () => {
    expect(api.interceptors.response.handlers.length).toBeGreaterThan(0);
  });
});

describe('API modules', () => {
  it('authApi should have signup, login, refresh methods', () => {
    expect(typeof authApi.signup).toBe('function');
    expect(typeof authApi.login).toBe('function');
    expect(typeof authApi.refresh).toBe('function');
  });

  it('jdApi should have create, list, get, analyze, delete methods', () => {
    expect(typeof jdApi.create).toBe('function');
    expect(typeof jdApi.list).toBe('function');
    expect(typeof jdApi.get).toBe('function');
    expect(typeof jdApi.analyze).toBe('function');
    expect(typeof jdApi.delete).toBe('function');
  });

  it('interviewApi should have start, list, get, submitAnswer, complete methods', () => {
    expect(typeof interviewApi.start).toBe('function');
    expect(typeof interviewApi.list).toBe('function');
    expect(typeof interviewApi.get).toBe('function');
    expect(typeof interviewApi.submitAnswer).toBe('function');
    expect(typeof interviewApi.complete).toBe('function');
    expect(typeof interviewApi.pause).toBe('function');
    expect(typeof interviewApi.resume).toBe('function');
    expect(typeof interviewApi.search).toBe('function');
    expect(typeof interviewApi.addFollowUp).toBe('function');
  });

  it('statisticsApi should have get and record methods', () => {
    expect(typeof statisticsApi.get).toBe('function');
    expect(typeof statisticsApi.record).toBe('function');
  });

  it('userApi should have getProfile and updateProfile methods', () => {
    expect(typeof userApi.getProfile).toBe('function');
    expect(typeof userApi.updateProfile).toBe('function');
  });
});
