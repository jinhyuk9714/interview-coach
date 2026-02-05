import { create } from 'zustand';
import type { InterviewSession, InterviewQna, GeneratedQuestion, JobDescription } from '@/types';

interface InterviewState {
  // Current interview session
  session: InterviewSession | null;
  currentQuestionIndex: number;
  isLoading: boolean;
  isSubmitting: boolean;

  // JD and questions
  selectedJd: JobDescription | null;
  questions: GeneratedQuestion[];
  selectedQuestionIds: number[];

  // Feedback streaming
  streamingFeedback: string;
  isStreaming: boolean;

  // Actions
  setSession: (session: InterviewSession | null) => void;
  setCurrentQuestionIndex: (index: number) => void;
  nextQuestion: () => void;
  prevQuestion: () => void;
  setSelectedJd: (jd: JobDescription | null) => void;
  setQuestions: (questions: GeneratedQuestion[]) => void;
  toggleQuestionSelection: (questionId: number) => void;
  setSelectedQuestionIds: (ids: number[]) => void;
  setLoading: (loading: boolean) => void;
  setSubmitting: (submitting: boolean) => void;
  setStreamingFeedback: (feedback: string) => void;
  appendStreamingFeedback: (chunk: string) => void;
  setIsStreaming: (streaming: boolean) => void;
  updateQnaAnswer: (qnaId: number, answer: string) => void;
  reset: () => void;
}

const initialState = {
  session: null,
  currentQuestionIndex: 0,
  isLoading: false,
  isSubmitting: false,
  selectedJd: null,
  questions: [],
  selectedQuestionIds: [],
  streamingFeedback: '',
  isStreaming: false,
};

export const useInterviewStore = create<InterviewState>((set, get) => ({
  ...initialState,

  setSession: (session) => set({ session }),

  setCurrentQuestionIndex: (index) => set({ currentQuestionIndex: index }),

  nextQuestion: () => {
    const { currentQuestionIndex, session } = get();
    if (session && currentQuestionIndex < session.totalQuestions - 1) {
      set({ currentQuestionIndex: currentQuestionIndex + 1 });
    }
  },

  prevQuestion: () => {
    const { currentQuestionIndex } = get();
    if (currentQuestionIndex > 0) {
      set({ currentQuestionIndex: currentQuestionIndex - 1 });
    }
  },

  setSelectedJd: (jd) => set({ selectedJd: jd }),

  setQuestions: (questions) => set({ questions }),

  toggleQuestionSelection: (questionId) => {
    const { selectedQuestionIds } = get();
    const index = selectedQuestionIds.indexOf(questionId);
    if (index > -1) {
      set({ selectedQuestionIds: selectedQuestionIds.filter((id) => id !== questionId) });
    } else {
      set({ selectedQuestionIds: [...selectedQuestionIds, questionId] });
    }
  },

  setSelectedQuestionIds: (ids) => set({ selectedQuestionIds: ids }),

  setLoading: (loading) => set({ isLoading: loading }),

  setSubmitting: (submitting) => set({ isSubmitting: submitting }),

  setStreamingFeedback: (feedback) => set({ streamingFeedback: feedback }),

  appendStreamingFeedback: (chunk) =>
    set((state) => ({ streamingFeedback: state.streamingFeedback + chunk })),

  setIsStreaming: (streaming) => set({ isStreaming: streaming }),

  updateQnaAnswer: (qnaId, answer) => {
    const { session } = get();
    if (!session?.qnaList) return;

    const updatedQnaList = session.qnaList.map((qna: InterviewQna) =>
      qna.id === qnaId ? { ...qna, answerText: answer } : qna
    );

    set({ session: { ...session, qnaList: updatedQnaList } });
  },

  reset: () => set(initialState),
}));
