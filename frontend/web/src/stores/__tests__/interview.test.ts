import { describe, it, expect, beforeEach } from 'vitest';
import { useInterviewStore } from '../interview';

describe('useInterviewStore', () => {
  beforeEach(() => {
    useInterviewStore.getState().reset();
  });

  it('should have initial state', () => {
    const state = useInterviewStore.getState();
    expect(state.session).toBeNull();
    expect(state.currentQuestionIndex).toBe(0);
    expect(state.isLoading).toBe(false);
    expect(state.isSubmitting).toBe(false);
    expect(state.selectedJd).toBeNull();
    expect(state.questions).toEqual([]);
    expect(state.selectedQuestionIds).toEqual([]);
    expect(state.streamingFeedback).toBe('');
    expect(state.isStreaming).toBe(false);
  });

  it('should set session', () => {
    const session = {
      id: 1,
      userId: 1,
      jdId: 1,
      interviewType: 'technical' as const,
      status: 'in_progress' as const,
      totalQuestions: 5,
      startedAt: '2024-01-01T00:00:00Z',
    };

    useInterviewStore.getState().setSession(session);
    expect(useInterviewStore.getState().session).toEqual(session);
  });

  it('should navigate questions with nextQuestion/prevQuestion', () => {
    const session = {
      id: 1,
      userId: 1,
      jdId: 1,
      interviewType: 'technical' as const,
      status: 'in_progress' as const,
      totalQuestions: 5,
      startedAt: '2024-01-01T00:00:00Z',
    };

    useInterviewStore.getState().setSession(session);
    expect(useInterviewStore.getState().currentQuestionIndex).toBe(0);

    useInterviewStore.getState().nextQuestion();
    expect(useInterviewStore.getState().currentQuestionIndex).toBe(1);

    useInterviewStore.getState().nextQuestion();
    expect(useInterviewStore.getState().currentQuestionIndex).toBe(2);

    useInterviewStore.getState().prevQuestion();
    expect(useInterviewStore.getState().currentQuestionIndex).toBe(1);
  });

  it('should not go below 0 with prevQuestion', () => {
    useInterviewStore.getState().prevQuestion();
    expect(useInterviewStore.getState().currentQuestionIndex).toBe(0);
  });

  it('should not exceed totalQuestions with nextQuestion', () => {
    const session = {
      id: 1,
      userId: 1,
      jdId: 1,
      interviewType: 'technical' as const,
      status: 'in_progress' as const,
      totalQuestions: 2,
      startedAt: '2024-01-01T00:00:00Z',
    };

    useInterviewStore.getState().setSession(session);
    useInterviewStore.getState().nextQuestion();
    useInterviewStore.getState().nextQuestion(); // Should not go to 2
    expect(useInterviewStore.getState().currentQuestionIndex).toBe(1);
  });

  it('should toggle question selection', () => {
    useInterviewStore.getState().toggleQuestionSelection(1);
    expect(useInterviewStore.getState().selectedQuestionIds).toEqual([1]);

    useInterviewStore.getState().toggleQuestionSelection(2);
    expect(useInterviewStore.getState().selectedQuestionIds).toEqual([1, 2]);

    useInterviewStore.getState().toggleQuestionSelection(1);
    expect(useInterviewStore.getState().selectedQuestionIds).toEqual([2]);
  });

  it('should manage streaming feedback', () => {
    useInterviewStore.getState().setStreamingFeedback('Hello');
    expect(useInterviewStore.getState().streamingFeedback).toBe('Hello');

    useInterviewStore.getState().appendStreamingFeedback(' World');
    expect(useInterviewStore.getState().streamingFeedback).toBe('Hello World');
  });

  it('should reset to initial state', () => {
    useInterviewStore.getState().setSession({
      id: 1, userId: 1, jdId: 1,
      interviewType: 'technical' as const,
      status: 'in_progress' as const,
      totalQuestions: 5,
      startedAt: '2024-01-01T00:00:00Z',
    });
    useInterviewStore.getState().setStreamingFeedback('test');
    useInterviewStore.getState().setIsStreaming(true);

    useInterviewStore.getState().reset();

    const state = useInterviewStore.getState();
    expect(state.session).toBeNull();
    expect(state.streamingFeedback).toBe('');
    expect(state.isStreaming).toBe(false);
  });
});
