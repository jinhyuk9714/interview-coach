// User types
export interface User {
  id: number;
  email: string;
  nickname: string;
  targetPosition?: string;
  experienceYears?: number;
  createdAt: string;
}

export interface TokenResponse {
  accessToken: string;
  refreshToken: string;
  tokenType: string;
  expiresIn: number;
}

// JD types
export interface JobDescription {
  id: number;
  userId: number;
  companyName: string;
  position: string;
  originalText: string;
  originalUrl?: string;
  parsedSkills: string[];
  parsedRequirements: string[];
  createdAt: string;
}

export interface JdAnalysis {
  jdId: number;
  skills: string[];
  requirements: string[];
  summary: string;
}

// Question types
export type QuestionType = 'technical' | 'behavioral' | 'mixed' | 'follow_up';
export type Difficulty = 1 | 2 | 3 | 4 | 5;

export interface GeneratedQuestion {
  id: number;
  jdId: number;
  questionType: QuestionType;
  skillCategory: string;
  questionText: string;
  hint?: string;
  idealAnswer?: string;
  difficulty: Difficulty;
  createdAt: string;
}

// Interview types
export type InterviewStatus = 'in_progress' | 'completed' | 'cancelled';

export interface InterviewSession {
  id: number;
  userId: number;
  jdId: number;
  interviewType: QuestionType;
  status: InterviewStatus;
  totalQuestions: number;
  avgScore?: number;
  startedAt: string;
  completedAt?: string;
  qnaList?: InterviewQna[];
}

export interface InterviewQna {
  id: number;
  sessionId: number;
  questionOrder: number;
  questionType: QuestionType;
  questionText: string;
  answerText?: string;
  feedback?: QnaFeedback;
  answeredAt?: string;
  parentQnaId?: number;
  followUpDepth?: number;
  isFollowUp?: boolean;
}

export interface QnaFeedback {
  score: number;
  strengths: string[];
  improvements: string[];
  tips: string[];
  followUpQuestion?: FollowUpQuestion;
  hasFollowUp?: boolean;
}

export interface FollowUpQuestion {
  questionText: string;
  focusArea: string;
  rationale: string;
  shouldAsk: boolean;
}

// Statistics types
export interface UserStatistics {
  userId: number;
  totalQuestions: number;
  totalCorrect: number;
  overallCorrectRate: number;
  byCategory: CategoryStatistics[];
}

export interface CategoryStatistics {
  category: string;
  totalQuestions: number;
  correctCount: number;
  correctRate: number;
}

// API Response types
export interface ApiResponse<T> {
  data: T;
  message?: string;
  status: number;
}

export interface PaginatedResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  page: number;
  size: number;
}
