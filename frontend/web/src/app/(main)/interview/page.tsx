'use client';

import { useState, useEffect, useRef, useCallback } from 'react';
import { useSearchParams } from 'next/navigation';
import Link from 'next/link';
import { motion } from 'motion/react';
import { Button, Card, Textarea, Tag, ScoreRing } from '@/components/ui';
import { useInterviewStore } from '@/stores/interview';
import { interviewApi, feedbackApi, questionApi, statisticsApi, jdApi } from '@/lib/api';
import {
  Brain,
  Send,
  ChevronRight,
  Lightbulb,
  CheckCircle2,
  XCircle,
  Play,
  ArrowRight,
  RotateCcw,
  MessageSquare,
  AlertCircle,
  Loader2,
  FileText,
  Pause,
  Timer,
  Eye,
  EyeOff
} from 'lucide-react';
import type { InterviewQna, QnaFeedback, GeneratedQuestion, JobDescription, FollowUpQuestion } from '@/types';
import { toast } from 'sonner';

// Track question with its skill category for statistics
interface QuestionWithCategory {
  questionType: string;
  questionText: string;
  skillCategory: string;
}

// Extended feedback type that may include idealAnswer from LLM
interface FeedbackWithIdealAnswer extends QnaFeedback {
  idealAnswer?: string;
}

export default function InterviewPage() {
  const searchParams = useSearchParams();
  const jdId = searchParams.get('jdId');

  const {
    session,
    setSession,
    currentQuestionIndex,
    setCurrentQuestionIndex,
    isSubmitting,
    setSubmitting,
    streamingFeedback,
    setStreamingFeedback,
    isStreaming,
    setIsStreaming,
  } = useInterviewStore();

  const [answer, setAnswer] = useState('');
  const [feedback, setFeedback] = useState<FeedbackWithIdealAnswer | null>(null);
  const [showFeedback, setShowFeedback] = useState(false);
  const [isStarted, setIsStarted] = useState(false);
  const [isCompleted, setIsCompleted] = useState(false);
  const [results, setResults] = useState<{ qna: InterviewQna; feedback: FeedbackWithIdealAnswer; skillCategory: string }[]>([]);
  const [error, setError] = useState<string | null>(null);
  const [isLoading, setIsLoading] = useState(false);
  const [availableQuestions, setAvailableQuestions] = useState<GeneratedQuestion[]>([]);
  const [jdList, setJdList] = useState<JobDescription[]>([]);
  const [isLoadingJds, setIsLoadingJds] = useState(false);
  const [selectedJdId, setSelectedJdId] = useState<number | null>(jdId ? parseInt(jdId) : null);
  // eslint-disable-next-line @typescript-eslint/no-unused-vars
  const [questionsWithCategory, setQuestionsWithCategory] = useState<QuestionWithCategory[]>([]);
  const questionsWithCategoryRef = useRef<QuestionWithCategory[]>([]);
  const textareaRef = useRef<HTMLTextAreaElement>(null);
  const [followUpQuestion, setFollowUpQuestion] = useState<FollowUpQuestion | null>(null);
  const [currentFollowUpDepth, setCurrentFollowUpDepth] = useState(0);
  const [isAddingFollowUp, setIsAddingFollowUp] = useState(false);
  const [returnToQuestionIndex, setReturnToQuestionIndex] = useState<number | null>(null);
  const [originalTotalQuestions, setOriginalTotalQuestions] = useState(0);

  // A-3: Timer states
  const [timerDuration, setTimerDuration] = useState(180); // 180s (3min) or 300s (5min)
  const [timeRemaining, setTimeRemaining] = useState(180);
  const timerRef = useRef<NodeJS.Timeout | null>(null);

  // A-4: Model Answer state
  const [showIdealAnswer, setShowIdealAnswer] = useState(false);

  // A-2: Pause/Resume states
  const [isPaused, setIsPaused] = useState(false);

  // Load JD list when no jdId is provided
  useEffect(() => {
    const loadJdList = async () => {
      if (!jdId) {
        setIsLoadingJds(true);
        try {
          const response = await jdApi.list();
          setJdList(response.data || []);
        } catch {
          // Non-critical: JD list load failure
        } finally {
          setIsLoadingJds(false);
        }
      }
    };
    loadJdList();
  }, [jdId]);

  // Load questions for the JD
  useEffect(() => {
    const loadQuestions = async () => {
      const targetJdId = jdId ? parseInt(jdId) : selectedJdId;
      if (targetJdId) {
        try {
          const response = await questionApi.listByJd(targetJdId);
          setAvailableQuestions(response.data || []);
        } catch {
          // Non-critical: question load failure
        }
      }
    };
    loadQuestions();
  }, [jdId, selectedJdId]);

  // A-3: Timer countdown effect
  useEffect(() => {
    // Only run timer when interview is started, answer phase is active (not showing feedback), and not paused
    if (isStarted && !showFeedback && !isCompleted && !isPaused) {
      timerRef.current = setInterval(() => {
        setTimeRemaining((prev) => {
          if (prev <= 1) {
            // Time's up
            if (timerRef.current) clearInterval(timerRef.current);
            return 0;
          }
          return prev - 1;
        });
      }, 1000);
    } else {
      if (timerRef.current) {
        clearInterval(timerRef.current);
        timerRef.current = null;
      }
    }

    return () => {
      if (timerRef.current) {
        clearInterval(timerRef.current);
        timerRef.current = null;
      }
    };
  }, [isStarted, showFeedback, isCompleted, isPaused, currentQuestionIndex]);

  // A-3: Reset timer when question changes
  useEffect(() => {
    setTimeRemaining(timerDuration);
  }, [currentQuestionIndex, timerDuration]);

  const currentQna = session?.qnaList?.[currentQuestionIndex];

  // A-3: Format time as mm:ss
  const formatTime = (seconds: number) => {
    const mins = Math.floor(seconds / 60);
    const secs = seconds % 60;
    return `${mins}:${secs.toString().padStart(2, '0')}`;
  };

  // A-2: Handle pause
  const handlePause = async () => {
    if (!session) return;
    try {
      await interviewApi.pause(session.id);
      setIsPaused(true);
    } catch {
      // Pause failure handled silently
    }
  };

  // A-2: Handle resume
  const handleResume = async () => {
    if (!session) return;
    try {
      await interviewApi.resume(session.id);
      setIsPaused(false);
    } catch {
      // Resume failure handled silently
    }
  };

  const handleStart = useCallback(async () => {
    const targetJdId = jdId ? parseInt(jdId) : selectedJdId;
    if (!targetJdId) {
      setError('JD를 선택해주세요.');
      return;
    }

    setIsLoading(true);
    setError(null);

    try {
      const selectedQuestions = availableQuestions.slice(0, 5);
      const questions = selectedQuestions.map(q => ({
        questionType: q.questionType,
        questionText: q.questionText,
      }));

      if (questions.length === 0) {
        setError('면접 질문이 없습니다. JD 분석 후 질문을 생성해주세요.');
        setIsLoading(false);
        return;
      }

      // Store questions with their skill categories for statistics
      const questionsWithCat = selectedQuestions.map(q => ({
        questionType: q.questionType,
        questionText: q.questionText,
        skillCategory: q.skillCategory || q.questionType,
      }));
      setQuestionsWithCategory(questionsWithCat);
      questionsWithCategoryRef.current = questionsWithCat;

      const response = await interviewApi.start({
        jdId: targetJdId,
        questions,
        interviewType: 'mixed',
      });

      // Reset currentQuestionIndex before setting new session
      setCurrentQuestionIndex(0);
      setSession(response.data);
      setOriginalTotalQuestions(response.data.totalQuestions || questions.length);
      setIsStarted(true);
      // A-3: Reset timer on start
      setTimeRemaining(timerDuration);
    } catch {
      // Error handled via UI state
      setError('면접 시작에 실패했습니다. 다시 시도해주세요.');
    } finally {
      setIsLoading(false);
    }
  }, [jdId, selectedJdId, availableQuestions, setSession, setCurrentQuestionIndex, timerDuration]);

  const handleSubmitAnswer = async () => {
    if (!answer.trim() || !currentQna || !session) return;

    setSubmitting(true);
    setShowFeedback(true);
    setStreamingFeedback('');
    setIsStreaming(true);
    setError(null);
    // A-4: Reset ideal answer toggle on new submission
    setShowIdealAnswer(false);

    try {
      // Submit the answer
      await interviewApi.submitAnswer(session.id, {
        questionOrder: currentQuestionIndex + 1,
        answerText: answer,
      });

      // Use POST for streaming feedback (supports long answers without URL length limits)
      let feedbackData: FeedbackWithIdealAnswer | null = null;
      const questionSkillCategory = questionsWithCategoryRef.current[currentQuestionIndex]?.skillCategory || currentQna.questionType || 'general';

      await feedbackApi.streamPost(
        session.id,
        {
          qnaId: currentQna?.id,
          question: currentQna?.questionText,
          answer: answer,
          followUpDepth: currentFollowUpDepth,
        },
        // onFeedback callback
        (data) => {
          const followUp = data.followUpQuestion as FollowUpQuestion | undefined;
          feedbackData = {
            score: (data.score as number) || 75,
            strengths: (data.strengths as string[]) || [],
            improvements: (data.improvements as string[]) || [],
            tips: Array.isArray(data.tips) ? data.tips as string[] : (data.tips ? [data.tips as string] : []),
            followUpQuestion: followUp,
            hasFollowUp: (data.hasFollowUp as boolean) || false,
            idealAnswer: (data.idealAnswer as string) || undefined,
          };
          // Capture follow-up question for UI
          if (followUp && data.hasFollowUp) {
            setFollowUpQuestion(followUp);
          } else {
            setFollowUpQuestion(null);
          }
          // Display overall comment as streaming feedback
          if (data.overallComment) {
            setStreamingFeedback(data.overallComment as string);
          }
        },
        // onComplete callback
        async () => {
          setIsStreaming(false);
          if (feedbackData && session) {
            setFeedback(feedbackData);
            setResults((prev) => [...prev, { qna: currentQna, feedback: feedbackData!, skillCategory: questionSkillCategory }]);

            // Save feedback to interview-service for persistence
            try {
              await interviewApi.updateFeedback(session.id, currentQuestionIndex + 1, feedbackData);
            } catch {
              // Non-critical: feedback persistence failure
            }

            // Record statistics immediately (don't wait until interview end)
            try {
              await statisticsApi.record({
                skillCategory: questionSkillCategory,
                isCorrect: feedbackData.score >= 60,
                score: feedbackData.score,
                weakPoint: feedbackData.score < 60 ? feedbackData.improvements[0] : undefined,
              });
            } catch {
              // Non-critical: statistics recording failure
            }
          }
        },
        // onError callback
        async () => {
          // SSE stream error - fallback to placeholder feedback
          setIsStreaming(false);
          // If we didn't get feedback from SSE, create a placeholder
          if (!feedbackData) {
            feedbackData = {
              score: 70,
              strengths: ['답변이 제출되었습니다'],
              improvements: ['피드백을 불러오는 중 오류가 발생했습니다'],
              tips: [],
            };
            setFeedback(feedbackData);
            setResults((prev) => [...prev, { qna: currentQna, feedback: feedbackData!, skillCategory: questionSkillCategory }]);

            // Save fallback feedback
            if (session) {
              try {
                await interviewApi.updateFeedback(session.id, currentQuestionIndex + 1, feedbackData);
              } catch {
                // Non-critical: fallback feedback persistence failure
              }

              // Record statistics even on error (so it counts in stats)
              try {
                await statisticsApi.record({
                  skillCategory: questionSkillCategory,
                  isCorrect: feedbackData.score >= 60,
                  score: feedbackData.score,
                  weakPoint: feedbackData.score < 60 ? feedbackData.improvements[0] : undefined,
                });
              } catch {
                // Non-critical: statistics recording failure on error path
              }
            }
          }
        }
      );

    } catch {
      setError('답변 제출에 실패했습니다. 다시 시도해주세요.');
      toast.error('답변 제출에 실패했습니다.');
      setShowFeedback(false);
      setIsStreaming(false);
    } finally {
      setSubmitting(false);
    }
  };

  const handleNext = async () => {
    // If we're in a follow-up question, return to original question sequence
    if (returnToQuestionIndex !== null && currentQna?.isFollowUp) {
      // Return to the next original question
      if (returnToQuestionIndex < originalTotalQuestions) {
        setCurrentQuestionIndex(returnToQuestionIndex);
        setReturnToQuestionIndex(null);
        setCurrentFollowUpDepth(0);
        setAnswer('');
        setFeedback(null);
        setShowFeedback(false);
        setStreamingFeedback('');
        setFollowUpQuestion(null);
        setShowIdealAnswer(false);
        return;
      }
    }

    // Check if there are more original questions to answer
    const nextOriginalIndex = currentQuestionIndex + 1;
    if (nextOriginalIndex < originalTotalQuestions) {
      setCurrentQuestionIndex(nextOriginalIndex);
      setAnswer('');
      setFeedback(null);
      setShowFeedback(false);
      setStreamingFeedback('');
      setFollowUpQuestion(null);
      setCurrentFollowUpDepth(0);
      setReturnToQuestionIndex(null);
      setShowIdealAnswer(false);
    } else {
      // Complete the interview (statistics already recorded per question)
      if (session) {
        try {
          await interviewApi.complete(session.id);
        } catch {
          // Non-critical: complete API call failure
        }
      }
      setIsCompleted(true);
      toast.success('면접이 완료되었습니다!');
    }
  };

  const handleAnswerFollowUp = async () => {
    if (!followUpQuestion || !currentQna || !session) return;

    setIsAddingFollowUp(true);
    setError(null);

    try {
      // Save current position to return after follow-up (only if not already in follow-up)
      if (returnToQuestionIndex === null) {
        setReturnToQuestionIndex(currentQuestionIndex + 1);
      }

      const response = await interviewApi.addFollowUp(session.id, {
        parentQnaId: currentQna.id,
        questionText: followUpQuestion.questionText,
        followUpDepth: currentFollowUpDepth + 1,
        focusArea: followUpQuestion.focusArea,
      });

      // Update session with new QnA
      const newQna = response.data;
      const updatedQnaList = [...(session.qnaList || []), newQna];
      setSession({
        ...session,
        qnaList: updatedQnaList,
        totalQuestions: updatedQnaList.length,
      });

      // Move to the new follow-up question
      setCurrentQuestionIndex(updatedQnaList.length - 1);
      setCurrentFollowUpDepth(currentFollowUpDepth + 1);
      setAnswer('');
      setFeedback(null);
      setShowFeedback(false);
      setStreamingFeedback('');
      setFollowUpQuestion(null);
      setShowIdealAnswer(false);
    } catch {
      // Error handled via UI state
      setError('꼬리 질문 추가에 실패했습니다.');
    } finally {
      setIsAddingFollowUp(false);
    }
  };

  const handleRestart = () => {
    setIsStarted(false);
    setIsCompleted(false);
    setSession(null);
    setCurrentQuestionIndex(0);
    setAnswer('');
    setFeedback(null);
    setShowFeedback(false);
    setStreamingFeedback('');
    setResults([]);
    setError(null);
    setQuestionsWithCategory([]);
    questionsWithCategoryRef.current = [];
    setFollowUpQuestion(null);
    setCurrentFollowUpDepth(0);
    setReturnToQuestionIndex(null);
    setOriginalTotalQuestions(0);
    // A-3: Reset timer states
    setTimeRemaining(timerDuration);
    // A-4: Reset ideal answer
    setShowIdealAnswer(false);
    // A-2: Reset pause state
    setIsPaused(false);
  };

  // Start screen
  if (!isStarted) {
    return (
      <div className="py-8">
        <div className="max-w-4xl mx-auto px-4">
          <motion.div
            initial={{ opacity: 0, y: 20 }}
            animate={{ opacity: 1, y: 0 }}
          >
            <Card className="p-12 text-center">
              <motion.div
                initial={{ scale: 0 }}
                animate={{ scale: 1 }}
                transition={{ type: 'spring', bounce: 0.5 }}
                className="w-24 h-24 bg-accent-blue border-2 border-ink flex items-center justify-center mx-auto mb-8"
              >
                <Brain className="w-12 h-12 text-white" />
              </motion.div>

              <h1 className="text-display-md font-display mb-4">
                AI 모의 면접
              </h1>
              <p className="text-neutral-600 mb-8 max-w-md mx-auto">
                AI 면접관과 함께 실전처럼 면접 연습을 해보세요.
                답변 직후 즉각적인 피드백을 받을 수 있습니다.
              </p>

              <div className="grid grid-cols-3 gap-4 max-w-lg mx-auto mb-8">
                <div className="p-4 bg-cream border-2 border-ink">
                  <div className="text-2xl font-display mb-1">
                    {availableQuestions.length || '-'}
                  </div>
                  <div className="text-xs text-neutral-500 uppercase">질문</div>
                </div>
                <div className="p-4 bg-cream border-2 border-ink">
                  <div className="text-2xl font-display mb-1">
                    {availableQuestions.length > 0
                      ? (() => {
                          const types = new Set(availableQuestions.map(q => q.questionType));
                          if (types.size > 1) return '혼합';
                          const type = availableQuestions[0]?.questionType;
                          return type === 'technical' ? '기술' : type === 'behavioral' ? '인성' : '혼합';
                        })()
                      : '-'}
                  </div>
                  <div className="text-xs text-neutral-500 uppercase">유형</div>
                </div>
                <div className="p-4 bg-cream border-2 border-ink">
                  <div className="text-2xl font-display mb-1">
                    {availableQuestions.length > 0
                      ? (() => {
                          const avgDifficulty = Math.round(
                            availableQuestions.reduce((sum, q) => sum + q.difficulty, 0) / availableQuestions.length
                          );
                          const labels = ['', '입문', '초급', '중급', '고급', '전문가'];
                          return labels[avgDifficulty] || '중급';
                        })()
                      : '-'}
                  </div>
                  <div className="text-xs text-neutral-500 uppercase">난이도</div>
                </div>
              </div>

              {/* A-3: Timer Duration Selector */}
              <div className="mb-8 max-w-lg mx-auto">
                <h3 className="text-sm font-semibold uppercase tracking-wider text-neutral-500 mb-4 flex items-center gap-2 justify-center">
                  <Timer className="w-4 h-4" />
                  답변 시간 설정
                </h3>
                <div className="flex gap-4 justify-center">
                  <button
                    onClick={() => setTimerDuration(180)}
                    className={`px-6 py-3 border-2 font-semibold transition-all ${
                      timerDuration === 180
                        ? 'border-ink bg-accent-lime shadow-[4px_4px_0_0_black]'
                        : 'border-neutral-200 hover:border-neutral-400 bg-white'
                    }`}
                  >
                    <div className="text-lg font-display">3분</div>
                    <div className="text-xs text-neutral-500">180초</div>
                  </button>
                  <button
                    onClick={() => setTimerDuration(300)}
                    className={`px-6 py-3 border-2 font-semibold transition-all ${
                      timerDuration === 300
                        ? 'border-ink bg-accent-lime shadow-[4px_4px_0_0_black]'
                        : 'border-neutral-200 hover:border-neutral-400 bg-white'
                    }`}
                  >
                    <div className="text-lg font-display">5분</div>
                    <div className="text-xs text-neutral-500">300초</div>
                  </button>
                </div>
              </div>

              {error && (
                <div className="mb-6 p-4 bg-accent-coral/10 border-2 border-accent-coral flex items-center gap-3 max-w-md mx-auto">
                  <AlertCircle className="w-5 h-5 text-accent-coral shrink-0" />
                  <p className="text-sm text-accent-coral">{error}</p>
                </div>
              )}

              {!jdId && (
                <div className="mb-8 max-w-lg mx-auto">
                  <h3 className="text-sm font-semibold uppercase tracking-wider text-neutral-500 mb-4 flex items-center gap-2">
                    <FileText className="w-4 h-4" />
                    JD 선택
                  </h3>
                  {isLoadingJds ? (
                    <div className="p-8 text-center">
                      <Loader2 className="w-6 h-6 animate-spin mx-auto text-neutral-400" />
                      <p className="text-sm text-neutral-500 mt-2">JD 목록 불러오는 중...</p>
                    </div>
                  ) : jdList.length === 0 ? (
                    <div className="p-6 bg-neutral-50 border-2 border-dashed border-neutral-300 text-center">
                      <FileText className="w-8 h-8 mx-auto text-neutral-300 mb-2" />
                      <p className="text-sm text-neutral-500 mb-4">등록된 JD가 없습니다</p>
                      <Link href="/jd">
                        <Button size="sm" variant="secondary">
                          JD 등록하러 가기
                        </Button>
                      </Link>
                    </div>
                  ) : (
                    <div className="space-y-2">
                      {jdList.map((jd) => (
                        <button
                          key={jd.id}
                          onClick={() => setSelectedJdId(jd.id)}
                          className={`w-full p-4 text-left border-2 transition-all ${
                            selectedJdId === jd.id
                              ? 'border-ink bg-accent-lime/20'
                              : 'border-neutral-200 hover:border-neutral-400 bg-white'
                          }`}
                        >
                          <div className="flex items-center gap-3">
                            <div className={`w-10 h-10 border-2 border-ink flex items-center justify-center font-display ${
                              selectedJdId === jd.id ? 'bg-accent-lime' : 'bg-white'
                            }`}>
                              {jd.companyName.charAt(0)}
                            </div>
                            <div className="flex-1">
                              <h4 className="font-semibold">{jd.companyName}</h4>
                              <p className="text-sm text-neutral-500">{jd.position}</p>
                            </div>
                            {selectedJdId === jd.id && (
                              <CheckCircle2 className="w-5 h-5 text-ink" />
                            )}
                          </div>
                        </button>
                      ))}
                    </div>
                  )}
                  {selectedJdId && availableQuestions.length === 0 && (
                    <div className="mt-4 p-4 bg-accent-blue/10 border-2 border-accent-blue">
                      <p className="text-sm text-accent-blue">
                        선택한 JD에 생성된 질문이 없습니다.
                        <Link href={`/jd`} className="underline font-semibold ml-1">
                          JD 분석 페이지에서 질문을 생성해주세요.
                        </Link>
                      </p>
                    </div>
                  )}
                </div>
              )}

              <Button
                size="lg"
                onClick={handleStart}
                leftIcon={isLoading ? <Loader2 className="w-5 h-5 animate-spin" /> : <Play className="w-5 h-5" />}
                disabled={isLoading || (!jdId && !selectedJdId) || availableQuestions.length === 0}
              >
                {isLoading ? '면접 준비 중...' : '면접 시작하기'}
              </Button>
            </Card>
          </motion.div>
        </div>
      </div>
    );
  }

  // Completed screen
  if (isCompleted) {
    const avgScore = results.reduce((acc, r) => acc + r.feedback.score, 0) / results.length;

    return (
      <div className="py-8">
        <div className="max-w-4xl mx-auto px-4">
          <motion.div
            initial={{ opacity: 0, y: 20 }}
            animate={{ opacity: 1, y: 0 }}
          >
            <Card className="p-8">
              <div className="text-center mb-8">
                <motion.div
                  initial={{ scale: 0 }}
                  animate={{ scale: 1 }}
                  transition={{ type: 'spring', bounce: 0.5 }}
                  className="mb-6"
                >
                  <ScoreRing score={Math.round(avgScore)} size="lg" />
                </motion.div>

                <h1 className="text-display-md font-display mb-2">
                  면접 완료!
                </h1>
                <p className="text-neutral-600">
                  총 {results.length}개의 질문에 답변하셨습니다
                </p>
              </div>

              <div className="space-y-4 mb-8">
                {results.map((result, index) => (
                  <motion.div
                    key={result.qna.id}
                    initial={{ opacity: 0, x: -20 }}
                    animate={{ opacity: 1, x: 0 }}
                    transition={{ delay: index * 0.1 }}
                    className={`p-4 border-2 border-ink ${result.qna.isFollowUp ? 'bg-accent-blue/5 ml-4' : 'bg-cream'}`}
                  >
                    <div className="flex items-start justify-between">
                      <div className="flex-1">
                        <div className="flex items-center gap-2 mb-2">
                          <span className="font-mono text-sm text-neutral-400">
                            Q{index + 1}
                          </span>
                          <Tag
                            variant={result.qna.questionType === 'technical' ? 'blue' : result.qna.questionType === 'follow_up' ? 'lime' : 'coral'}
                            size="sm"
                          >
                            {result.qna.questionType === 'technical' ? '기술' : result.qna.questionType === 'follow_up' ? '꼬리' : '인성'}
                          </Tag>
                          {result.qna.isFollowUp && (
                            <span className="text-xs text-neutral-400">(깊이: {result.qna.followUpDepth})</span>
                          )}
                        </div>
                        <p className="text-sm">{result.qna.questionText}</p>
                      </div>
                      <ScoreRing score={result.feedback.score} size="sm" />
                    </div>
                  </motion.div>
                ))}
              </div>

              <div className="flex gap-4 justify-center">
                <Button
                  variant="secondary"
                  onClick={handleRestart}
                  leftIcon={<RotateCcw className="w-4 h-4" />}
                >
                  다시 연습하기
                </Button>
                <Link href={session ? `/interview/${session.id}` : '/interview'}>
                  <Button rightIcon={<ArrowRight className="w-4 h-4" />}>
                    상세 리포트 보기
                  </Button>
                </Link>
              </div>
            </Card>
          </motion.div>
        </div>
      </div>
    );
  }

  // Interview in progress
  return (
    <div className="py-8">
      <div className="max-w-4xl mx-auto px-4">
        {/* A-2: Paused Overlay */}
        {isPaused && (
          <motion.div
            initial={{ opacity: 0 }}
            animate={{ opacity: 1 }}
            className="fixed inset-0 z-50 bg-black/60 flex items-center justify-center"
          >
            <motion.div
              initial={{ scale: 0.8, opacity: 0 }}
              animate={{ scale: 1, opacity: 1 }}
              transition={{ type: 'spring', bounce: 0.4 }}
            >
              <Card className="p-12 text-center max-w-md mx-4">
                <div className="w-20 h-20 bg-accent-coral border-2 border-ink flex items-center justify-center mx-auto mb-6">
                  <Pause className="w-10 h-10 text-white" />
                </div>
                <h2 className="text-display-sm font-display mb-2">일시 정지</h2>
                <p className="text-neutral-600 mb-8">
                  면접이 일시 정지되었습니다.
                  <br />
                  준비가 되면 재개 버튼을 눌러주세요.
                </p>
                <Button
                  size="lg"
                  onClick={handleResume}
                  leftIcon={<Play className="w-5 h-5" />}
                >
                  면접 재개하기
                </Button>
              </Card>
            </motion.div>
          </motion.div>
        )}

        {/* Progress Header */}
        <motion.div
          initial={{ opacity: 0, y: -10 }}
          animate={{ opacity: 1, y: 0 }}
          className="mb-6"
        >
          <div className="flex items-center justify-between mb-4">
            <div className="flex items-center gap-3">
              <div className="w-10 h-10 bg-accent-blue border-2 border-ink flex items-center justify-center">
                <Brain className="w-5 h-5 text-white" />
              </div>
              <div>
                <h2 className="font-sans font-semibold">AI 면접관</h2>
                <p className="text-xs text-neutral-500">기술 면접 모드</p>
              </div>
            </div>
            <div className="flex items-center gap-4">
              {/* A-2: Pause Button */}
              <button
                onClick={handlePause}
                className="w-8 h-8 border-2 border-ink bg-white hover:bg-neutral-100 flex items-center justify-center transition-colors"
                title="일시 정지"
              >
                <Pause className="w-4 h-4" />
              </button>
              <span className="text-sm font-mono text-neutral-500">
                질문 {returnToQuestionIndex !== null ? returnToQuestionIndex : Math.min(currentQuestionIndex + 1, originalTotalQuestions)} / {originalTotalQuestions || session?.totalQuestions}
                {returnToQuestionIndex !== null && (
                  <span className="text-accent-blue ml-1">(꼬리 질문)</span>
                )}
              </span>
              <div className="flex gap-1">
                {Array.from({ length: originalTotalQuestions || session?.totalQuestions || 5 }).map((_, i) => {
                  const displayIndex = returnToQuestionIndex !== null ? returnToQuestionIndex - 1 : currentQuestionIndex;
                  return (
                  <div
                    key={i}
                    className={`w-8 h-1.5 ${
                      i < displayIndex
                        ? 'bg-accent-lime'
                        : i === displayIndex
                        ? returnToQuestionIndex !== null ? 'bg-accent-blue' : 'bg-accent-coral'
                        : 'bg-neutral-200'
                    }`}
                  />
                  );
                })}
              </div>
            </div>
          </div>
        </motion.div>

        {/* Question Card */}
        <motion.div
          key={currentQuestionIndex}
          initial={{ opacity: 0, x: 20 }}
          animate={{ opacity: 1, x: 0 }}
          transition={{ duration: 0.3 }}
        >
          <Card className="p-6 mb-6">
            <div className="flex items-center gap-2 mb-4">
              <Tag variant={currentQna?.questionType === 'technical' ? 'blue' : currentQna?.questionType === 'follow_up' ? 'lime' : 'coral'}>
                {currentQna?.questionType === 'technical' ? '기술 질문' : currentQna?.questionType === 'follow_up' ? '꼬리 질문' : '인성 질문'}
              </Tag>
              <span className="text-xs font-mono text-neutral-400">
                #{currentQuestionIndex + 1}
              </span>
              {currentQna?.isFollowUp && (
                <span className="text-xs text-neutral-400">
                  (깊이: {currentQna.followUpDepth})
                </span>
              )}
            </div>

            <blockquote className="font-display text-2xl leading-relaxed border-l-4 border-ink pl-6">
              &ldquo;{currentQna?.questionText}&rdquo;
            </blockquote>
          </Card>
        </motion.div>

        {/* Answer Section */}
        {!showFeedback ? (
          <motion.div
            initial={{ opacity: 0, y: 20 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ delay: 0.2 }}
          >
            <Card className="p-6">
              <div className="flex items-center justify-between mb-4">
                <div className="flex items-center gap-2">
                  <MessageSquare className="w-4 h-4 text-neutral-400" />
                  <span className="text-sm font-sans font-semibold uppercase tracking-wider text-neutral-500">
                    나의 답변
                  </span>
                </div>

                {/* A-3: Timer Display */}
                <div className={`flex items-center gap-2 px-3 py-1.5 border-2 font-mono text-sm ${
                  timeRemaining <= 30
                    ? 'border-accent-coral bg-accent-coral/10 text-accent-coral animate-pulse'
                    : timeRemaining <= 60
                    ? 'border-orange-400 bg-orange-50 text-orange-600'
                    : 'border-neutral-300 bg-white text-neutral-600'
                }`}>
                  <Timer className="w-4 h-4" />
                  <span className="font-semibold">{formatTime(timeRemaining)}</span>
                </div>
              </div>

              {/* A-3: Time's up warning */}
              {timeRemaining === 0 && (
                <motion.div
                  initial={{ opacity: 0, y: -10 }}
                  animate={{ opacity: 1, y: 0 }}
                  className="mb-4 p-3 bg-accent-coral/10 border-2 border-accent-coral flex items-center gap-3"
                >
                  <AlertCircle className="w-5 h-5 text-accent-coral shrink-0" />
                  <p className="text-sm text-accent-coral font-semibold">
                    시간이 초과되었습니다. 답변을 제출해주세요.
                  </p>
                </motion.div>
              )}

              <Textarea
                ref={textareaRef}
                value={answer}
                onChange={(e) => setAnswer(e.target.value)}
                placeholder="여기에 답변을 입력하세요. 실제 면접처럼 구체적으로 답변해보세요..."
                className="min-h-[200px] mb-4"
              />

              <div className="flex items-center justify-between">
                <p className="text-xs text-neutral-400 font-mono">
                  {answer.length}자
                </p>
                <Button
                  onClick={handleSubmitAnswer}
                  disabled={!answer.trim()}
                  isLoading={isSubmitting}
                  rightIcon={<Send className="w-4 h-4" />}
                >
                  답변 제출
                </Button>
              </div>
            </Card>
          </motion.div>
        ) : (
          /* Feedback Section */
          <motion.div
            initial={{ opacity: 0, y: 20 }}
            animate={{ opacity: 1, y: 0 }}
          >
            <Card className="p-6">
              <div className="flex items-center justify-between mb-6">
                <div className="flex items-center gap-2">
                  <Lightbulb className="w-5 h-5 text-accent-coral" />
                  <span className="font-sans font-semibold">AI 피드백</span>
                </div>
                {feedback && <ScoreRing score={feedback.score} size="sm" />}
              </div>

              {/* Streaming feedback */}
              <div className="prose prose-sm max-w-none mb-6">
                <div className="whitespace-pre-wrap font-sans leading-relaxed">
                  {streamingFeedback}
                  {isStreaming && (
                    <span className="animate-blink text-accent-coral">|</span>
                  )}
                </div>
              </div>

              {/* Structured feedback after streaming */}
              {feedback && !isStreaming && (
                <motion.div
                  initial={{ opacity: 0 }}
                  animate={{ opacity: 1 }}
                  className="space-y-4 pt-4 border-t-2 border-dashed border-neutral-200"
                >
                  {/* Strengths */}
                  <div className="p-4 bg-green-50 border-l-4 border-green-500">
                    <h4 className="font-semibold text-green-800 flex items-center gap-2 mb-2">
                      <CheckCircle2 className="w-4 h-4" />
                      강점
                    </h4>
                    <ul className="space-y-1">
                      {feedback.strengths.map((s, i) => (
                        <li key={i} className="text-sm text-green-700">• {s}</li>
                      ))}
                    </ul>
                  </div>

                  {/* Improvements */}
                  <div className="p-4 bg-orange-50 border-l-4 border-orange-500">
                    <h4 className="font-semibold text-orange-800 flex items-center gap-2 mb-2">
                      <XCircle className="w-4 h-4" />
                      개선점
                    </h4>
                    <ul className="space-y-1">
                      {feedback.improvements.map((s, i) => (
                        <li key={i} className="text-sm text-orange-700">• {s}</li>
                      ))}
                    </ul>
                  </div>

                  {/* A-4: Model Answer Toggle */}
                  {feedback.idealAnswer && (
                    <div>
                      <button
                        onClick={() => setShowIdealAnswer(!showIdealAnswer)}
                        className="flex items-center gap-2 px-4 py-2 border-2 border-ink bg-accent-blue/10 hover:bg-accent-blue/20 transition-colors font-semibold text-sm"
                      >
                        {showIdealAnswer ? (
                          <EyeOff className="w-4 h-4" />
                        ) : (
                          <Eye className="w-4 h-4" />
                        )}
                        모범 답안 보기
                      </button>
                      {showIdealAnswer && (
                        <motion.div
                          initial={{ opacity: 0, height: 0 }}
                          animate={{ opacity: 1, height: 'auto' }}
                          exit={{ opacity: 0, height: 0 }}
                          className="mt-3 p-4 bg-accent-blue/5 border-2 border-accent-blue"
                        >
                          <h4 className="font-semibold text-accent-blue flex items-center gap-2 mb-3">
                            <Lightbulb className="w-4 h-4" />
                            모범 답안
                          </h4>
                          <div className="text-sm text-neutral-700 whitespace-pre-wrap leading-relaxed">
                            {feedback.idealAnswer}
                          </div>
                        </motion.div>
                      )}
                    </div>
                  )}
                </motion.div>
              )}

              {/* Follow-up Question */}
              {feedback && !isStreaming && followUpQuestion && (
                <motion.div
                  initial={{ opacity: 0, y: 10 }}
                  animate={{ opacity: 1, y: 0 }}
                  className="mt-4 p-4 bg-accent-blue/10 border-2 border-accent-blue"
                >
                  <h4 className="font-semibold text-accent-blue flex items-center gap-2 mb-2">
                    <RotateCcw className="w-4 h-4" />
                    꼬리 질문
                  </h4>
                  <p className="text-sm text-neutral-700 mb-2">
                    &ldquo;{followUpQuestion.questionText}&rdquo;
                  </p>
                  <div className="flex items-center gap-2 text-xs text-neutral-500">
                    <Tag variant="blue" size="sm">{followUpQuestion.focusArea}</Tag>
                    <span>{followUpQuestion.rationale}</span>
                  </div>
                </motion.div>
              )}

              {/* Navigation */}
              {!isStreaming && (
                <motion.div
                  initial={{ opacity: 0 }}
                  animate={{ opacity: 1 }}
                  className="flex justify-between mt-6 pt-4 border-t-2 border-neutral-200"
                >
                  <Button
                    variant="ghost"
                    onClick={() => {
                      setShowFeedback(false);
                      setAnswer('');
                      setFollowUpQuestion(null);
                      setShowIdealAnswer(false);
                    }}
                    leftIcon={<RotateCcw className="w-4 h-4" />}
                  >
                    다시 답변하기
                  </Button>
                  <div className="flex gap-2">
                    {followUpQuestion && (
                      <Button
                        variant="secondary"
                        onClick={handleAnswerFollowUp}
                        isLoading={isAddingFollowUp}
                        leftIcon={<MessageSquare className="w-4 h-4" />}
                      >
                        꼬리 질문 답변하기
                      </Button>
                    )}
                    <Button
                      onClick={handleNext}
                      rightIcon={<ChevronRight className="w-4 h-4" />}
                    >
                      {returnToQuestionIndex !== null
                        ? '다음 질문으로'
                        : currentQuestionIndex < originalTotalQuestions - 1
                        ? '다음 질문'
                        : '면접 완료'}
                    </Button>
                  </div>
                </motion.div>
              )}
            </Card>
          </motion.div>
        )}
      </div>
    </div>
  );
}
