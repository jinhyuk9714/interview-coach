'use client';

import { useState, useEffect, useRef } from 'react';
import { motion } from 'motion/react';
import { Button, Card, Textarea, Tag, ScoreRing } from '@/components/ui';
import { useInterviewStore } from '@/stores/interview';
// import { interviewApi, feedbackApi } from '@/lib/api';
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
  MessageSquare
} from 'lucide-react';
import type { InterviewQna, QnaFeedback } from '@/types';

// Mock interview session for demo
const mockSession = {
  id: 1,
  userId: 1,
  jdId: 1,
  interviewType: 'mixed' as const,
  status: 'in_progress' as const,
  totalQuestions: 5,
  startedAt: new Date().toISOString(),
  qnaList: [
    {
      id: 1,
      sessionId: 1,
      questionOrder: 1,
      questionType: 'technical' as const,
      questionText: 'Java의 가비지 컬렉션(GC) 동작 원리와 G1 GC의 특징을 설명해주세요.',
    },
    {
      id: 2,
      sessionId: 1,
      questionOrder: 2,
      questionType: 'technical' as const,
      questionText: 'Spring의 @Transactional 어노테이션의 propagation 옵션들에 대해 설명해주세요.',
    },
    {
      id: 3,
      sessionId: 1,
      questionOrder: 3,
      questionType: 'technical' as const,
      questionText: 'JPA N+1 문제가 무엇이며, 어떻게 해결할 수 있나요?',
    },
    {
      id: 4,
      sessionId: 1,
      questionOrder: 4,
      questionType: 'behavioral' as const,
      questionText: '팀 내에서 기술적인 의견 충돌이 있었을 때 어떻게 해결하셨나요?',
    },
    {
      id: 5,
      sessionId: 1,
      questionOrder: 5,
      questionType: 'technical' as const,
      questionText: '마이크로서비스 아키텍처에서 서비스 간 통신 방식을 설명해주세요.',
    },
  ],
};

export default function InterviewPage() {
  const {
    session,
    setSession,
    currentQuestionIndex,
    setCurrentQuestionIndex,
    nextQuestion,
    isSubmitting,
    setSubmitting,
    streamingFeedback,
    setStreamingFeedback,
    appendStreamingFeedback,
    isStreaming,
    setIsStreaming,
  } = useInterviewStore();

  const [answer, setAnswer] = useState('');
  const [feedback, setFeedback] = useState<QnaFeedback | null>(null);
  const [showFeedback, setShowFeedback] = useState(false);
  const [isStarted, setIsStarted] = useState(false);
  const [isCompleted, setIsCompleted] = useState(false);
  const [results, setResults] = useState<{ qna: InterviewQna; feedback: QnaFeedback }[]>([]);
  const textareaRef = useRef<HTMLTextAreaElement>(null);

  // Start interview with mock data for demo
  useEffect(() => {
    if (!session && isStarted) {
      setSession(mockSession as unknown as typeof session);
    }
  }, [isStarted, session, setSession]);

  const currentQna = session?.qnaList?.[currentQuestionIndex];

  const handleStart = () => {
    setIsStarted(true);
  };

  const handleSubmitAnswer = async () => {
    if (!answer.trim() || !currentQna) return;

    setSubmitting(true);
    setShowFeedback(true);
    setStreamingFeedback('');
    setIsStreaming(true);

    try {
      // In production, this would call the actual API
      // const response = await interviewApi.submitAnswer(session!.id, {
      //   qnaId: currentQna.id,
      //   answer: answer,
      // });

      // Mock streaming feedback
      const mockFeedbackText = `
답변에서 핵심 개념을 잘 파악하고 계십니다.

**강점:**
• 기본 개념을 정확하게 이해하고 있습니다
• 실제 사례를 들어 설명하려는 시도가 좋습니다
• 논리적인 구조로 답변을 전개했습니다

**개선점:**
• 좀 더 구체적인 예시를 추가하면 좋겠습니다
• 성능 관점에서의 trade-off를 언급하면 더 좋습니다
• 실무 경험을 더 연결 지어 설명해보세요

**팁:**
실제 면접에서는 "저희 프로젝트에서는..." 이런 식으로 실무 경험과 연결 지으면 면접관에게 더 좋은 인상을 줄 수 있습니다.
      `.trim();

      // Simulate streaming
      let currentIndex = 0;
      const streamInterval = setInterval(() => {
        if (currentIndex < mockFeedbackText.length) {
          appendStreamingFeedback(mockFeedbackText[currentIndex]);
          currentIndex++;
        } else {
          clearInterval(streamInterval);
          setIsStreaming(false);

          // Set final feedback
          const mockFeedback: QnaFeedback = {
            score: Math.floor(Math.random() * 30) + 60, // 60-90
            strengths: [
              '기본 개념을 정확하게 이해하고 있습니다',
              '실제 사례를 들어 설명하려는 시도가 좋습니다',
            ],
            improvements: [
              '좀 더 구체적인 예시를 추가하면 좋겠습니다',
              '성능 관점에서의 trade-off를 언급하면 더 좋습니다',
            ],
            tips: [
              '실무 경험과 연결 지어 설명하세요',
            ],
          };

          setFeedback(mockFeedback);
          setResults((prev) => [...prev, { qna: currentQna, feedback: mockFeedback }]);
        }
      }, 20);

    } catch (error) {
      console.error('Submit answer error:', error);
    } finally {
      setSubmitting(false);
    }
  };

  const handleNext = () => {
    if (currentQuestionIndex < (session?.totalQuestions || 0) - 1) {
      nextQuestion();
      setAnswer('');
      setFeedback(null);
      setShowFeedback(false);
      setStreamingFeedback('');
    } else {
      setIsCompleted(true);
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
    setResults([]);
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
                  <div className="text-2xl font-display mb-1">5</div>
                  <div className="text-xs text-neutral-500 uppercase">질문</div>
                </div>
                <div className="p-4 bg-cream border-2 border-ink">
                  <div className="text-2xl font-display mb-1">혼합</div>
                  <div className="text-xs text-neutral-500 uppercase">유형</div>
                </div>
                <div className="p-4 bg-cream border-2 border-ink">
                  <div className="text-2xl font-display mb-1">중급</div>
                  <div className="text-xs text-neutral-500 uppercase">난이도</div>
                </div>
              </div>

              <Button
                size="lg"
                onClick={handleStart}
                leftIcon={<Play className="w-5 h-5" />}
              >
                면접 시작하기
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
                    className="p-4 bg-cream border-2 border-ink"
                  >
                    <div className="flex items-start justify-between">
                      <div className="flex-1">
                        <div className="flex items-center gap-2 mb-2">
                          <span className="font-mono text-sm text-neutral-400">
                            Q{index + 1}
                          </span>
                          <Tag
                            variant={result.qna.questionType === 'technical' ? 'blue' : 'coral'}
                            size="sm"
                          >
                            {result.qna.questionType === 'technical' ? '기술' : '인성'}
                          </Tag>
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
                <Button rightIcon={<ArrowRight className="w-4 h-4" />}>
                  상세 리포트 보기
                </Button>
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
              <span className="text-sm font-mono text-neutral-500">
                질문 {currentQuestionIndex + 1} / {session?.totalQuestions}
              </span>
              <div className="flex gap-1">
                {Array.from({ length: session?.totalQuestions || 5 }).map((_, i) => (
                  <div
                    key={i}
                    className={`w-8 h-1.5 ${
                      i < currentQuestionIndex
                        ? 'bg-accent-lime'
                        : i === currentQuestionIndex
                        ? 'bg-accent-coral'
                        : 'bg-neutral-200'
                    }`}
                  />
                ))}
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
              <Tag variant={currentQna?.questionType === 'technical' ? 'blue' : 'coral'}>
                {currentQna?.questionType === 'technical' ? '기술 질문' : '인성 질문'}
              </Tag>
              <span className="text-xs font-mono text-neutral-400">
                #{currentQuestionIndex + 1}
              </span>
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
              <div className="flex items-center gap-2 mb-4">
                <MessageSquare className="w-4 h-4 text-neutral-400" />
                <span className="text-sm font-sans font-semibold uppercase tracking-wider text-neutral-500">
                  나의 답변
                </span>
              </div>

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
                    }}
                    leftIcon={<RotateCcw className="w-4 h-4" />}
                  >
                    다시 답변하기
                  </Button>
                  <Button
                    onClick={handleNext}
                    rightIcon={<ChevronRight className="w-4 h-4" />}
                  >
                    {currentQuestionIndex < (session?.totalQuestions || 0) - 1
                      ? '다음 질문'
                      : '면접 완료'}
                  </Button>
                </motion.div>
              )}
            </Card>
          </motion.div>
        )}
      </div>
    </div>
  );
}
