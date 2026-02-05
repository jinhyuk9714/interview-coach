'use client';

import { useState, useEffect } from 'react';
import { useParams, useRouter } from 'next/navigation';
import Link from 'next/link';
import { motion } from 'motion/react';
import { Button, Card, Tag, ScoreRing } from '@/components/ui';
import { interviewApi, jdApi } from '@/lib/api';
import { formatDate } from '@/lib/utils';
import {
  ArrowLeft,
  Calendar,
  Building2,
  Briefcase,
  CheckCircle2,
  XCircle,
  MessageSquare,
  Loader2,
  AlertCircle,
  TrendingUp,
  TrendingDown,
  RotateCcw
} from 'lucide-react';
import type { InterviewSession, InterviewQna, JobDescription } from '@/types';

// feedback 데이터에서 추출한 확장 정보
interface ExtendedQnaData {
  score: number;
  strengths: string[];
  improvements: string[];
}

export default function InterviewDetailPage() {
  const params = useParams();
  const router = useRouter();
  const id = params.id as string;

  const [session, setSession] = useState<InterviewSession | null>(null);
  const [jd, setJd] = useState<JobDescription | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [expandedQna, setExpandedQna] = useState<number | null>(null);

  useEffect(() => {
    const fetchData = async () => {
      if (!id) return;

      setIsLoading(true);
      setError(null);

      try {
        const sessionRes = await interviewApi.get(parseInt(id));
        const sessionData = sessionRes.data;
        setSession(sessionData);

        // Fetch JD info
        if (sessionData.jdId) {
          try {
            const jdRes = await jdApi.get(sessionData.jdId);
            setJd(jdRes.data);
          } catch {
            // JD not found is not critical
          }
        }
      } catch (err) {
        console.error('Failed to fetch interview:', err);
        setError('면접 결과를 불러오는데 실패했습니다.');
      } finally {
        setIsLoading(false);
      }
    };

    fetchData();
  }, [id]);

  // Helper function to extract feedback data from QnA
  const getQnaFeedback = (qna: InterviewQna): ExtendedQnaData => {
    const feedback = qna.feedback as Record<string, unknown> | undefined;
    return {
      score: typeof feedback?.score === 'number' ? feedback.score : 70,
      strengths: Array.isArray(feedback?.strengths) ? feedback.strengths as string[] : [],
      improvements: Array.isArray(feedback?.improvements) ? feedback.improvements as string[] : [],
    };
  };

  // Calculate statistics
  const qnaList = session?.qnaList || [];
  const answeredQna = qnaList.filter(q => q.answerText);
  const avgScore = answeredQna.length > 0
    ? Math.round(answeredQna.reduce((acc, q) => acc + getQnaFeedback(q).score, 0) / answeredQna.length)
    : 0;

  const technicalQna = answeredQna.filter(q => q.questionType === 'technical');
  const behavioralQna = answeredQna.filter(q => q.questionType === 'behavioral' || q.questionType === 'mixed');
  const followUpQna = answeredQna.filter(q => q.questionType === 'follow_up');

  const technicalAvg = technicalQna.length > 0
    ? Math.round(technicalQna.reduce((acc, q) => acc + getQnaFeedback(q).score, 0) / technicalQna.length)
    : 0;
  const behavioralAvg = behavioralQna.length > 0
    ? Math.round(behavioralQna.reduce((acc, q) => acc + getQnaFeedback(q).score, 0) / behavioralQna.length)
    : 0;
  const followUpAvg = followUpQna.length > 0
    ? Math.round(followUpQna.reduce((acc, q) => acc + getQnaFeedback(q).score, 0) / followUpQna.length)
    : 0;

  // Calculate original questions count (excluding follow-ups)
  const originalQnaCount = answeredQna.filter(q => q.questionType !== 'follow_up').length;

  // Collect all strengths and improvements
  const allStrengths = answeredQna.flatMap(q => getQnaFeedback(q).strengths);
  const allImprovements = answeredQna.flatMap(q => getQnaFeedback(q).improvements);

  if (isLoading) {
    return (
      <div className="py-8">
        <div className="max-w-4xl mx-auto px-4 flex items-center justify-center py-20">
          <Loader2 className="w-8 h-8 animate-spin text-neutral-400" />
          <p className="ml-3 text-neutral-500">면접 결과를 불러오는 중...</p>
        </div>
      </div>
    );
  }

  if (error || !session) {
    return (
      <div className="py-8">
        <div className="max-w-4xl mx-auto px-4">
          <Card className="p-8 text-center">
            <AlertCircle className="w-12 h-12 mx-auto text-accent-coral mb-4" />
            <h2 className="text-xl font-display mb-2">오류가 발생했습니다</h2>
            <p className="text-neutral-500 mb-6">{error || '면접 결과를 찾을 수 없습니다.'}</p>
            <Button onClick={() => router.back()} leftIcon={<ArrowLeft className="w-4 h-4" />}>
              뒤로 가기
            </Button>
          </Card>
        </div>
      </div>
    );
  }

  return (
    <div className="py-8">
      <div className="max-w-4xl mx-auto px-4">
        {/* Header */}
        <motion.div
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          className="mb-8"
        >
          <Link href="/dashboard" className="inline-flex items-center gap-2 text-neutral-500 hover:text-ink mb-4">
            <ArrowLeft className="w-4 h-4" />
            대시보드로 돌아가기
          </Link>

          <div className="flex flex-col md:flex-row md:items-center md:justify-between gap-4">
            <div>
              <h1 className="text-display-md font-display mb-2">면접 결과 리포트</h1>
              <div className="flex items-center gap-4 text-sm text-neutral-500">
                <span className="flex items-center gap-1">
                  <Calendar className="w-4 h-4" />
                  {formatDate(session.startedAt)}
                </span>
                <Tag variant={session.status === 'completed' ? 'lime' : 'default'}>
                  {session.status === 'completed' ? '완료' : session.status === 'in_progress' ? '진행중' : '취소됨'}
                </Tag>
              </div>
            </div>

            <Link href={`/interview?jdId=${session.jdId}`}>
              <Button variant="secondary" leftIcon={<RotateCcw className="w-4 h-4" />}>
                다시 연습하기
              </Button>
            </Link>
          </div>
        </motion.div>

        {/* JD Info */}
        {jd && (
          <motion.div
            initial={{ opacity: 0, y: 20 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ delay: 0.1 }}
            className="mb-6"
          >
            <Card className="p-4 bg-neutral-50">
              <div className="flex items-center gap-4">
                <div className="w-12 h-12 bg-accent-lime border-2 border-ink flex items-center justify-center font-display text-xl">
                  {jd.companyName.charAt(0)}
                </div>
                <div className="flex-1">
                  <div className="flex items-center gap-2">
                    <Building2 className="w-4 h-4 text-neutral-400" />
                    <span className="font-semibold">{jd.companyName}</span>
                  </div>
                  <div className="flex items-center gap-2 text-sm text-neutral-500">
                    <Briefcase className="w-4 h-4" />
                    <span>{jd.position}</span>
                  </div>
                </div>
              </div>
            </Card>
          </motion.div>
        )}

        {/* Score Overview */}
        <motion.div
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ delay: 0.2 }}
          className="mb-8"
        >
          <Card className="p-6">
            <div className="grid md:grid-cols-3 gap-6">
              {/* Overall Score */}
              <div className="text-center">
                <motion.div
                  initial={{ scale: 0 }}
                  animate={{ scale: 1 }}
                  transition={{ type: 'spring', bounce: 0.5, delay: 0.3 }}
                  className="mb-4"
                >
                  <ScoreRing score={avgScore} size="lg" />
                </motion.div>
                <h3 className="font-display text-lg">종합 점수</h3>
                <p className="text-sm text-neutral-500">
                  {originalQnaCount}개 질문{followUpQna.length > 0 ? ` + ${followUpQna.length}개 꼬리질문` : ''} 답변
                </p>
              </div>

              {/* Category Scores */}
              <div className="space-y-4 md:col-span-2">
                <h4 className="font-semibold text-sm uppercase tracking-wider text-neutral-500">
                  카테고리별 점수
                </h4>

                {/* Technical */}
                <div className="p-4 bg-cream border-2 border-ink">
                  <div className="flex items-center justify-between mb-2">
                    <div className="flex items-center gap-2">
                      <Tag variant="blue" size="sm">기술</Tag>
                      <span className="text-sm text-neutral-500">
                        {technicalQna.length}개 질문
                      </span>
                    </div>
                    <span className="font-mono font-bold">{technicalAvg}점</span>
                  </div>
                  <div className="h-2 bg-neutral-200 overflow-hidden">
                    <motion.div
                      initial={{ width: 0 }}
                      animate={{ width: `${technicalAvg}%` }}
                      transition={{ delay: 0.5, duration: 0.8 }}
                      className="h-full bg-accent-blue"
                    />
                  </div>
                </div>

                {/* Behavioral */}
                <div className="p-4 bg-cream border-2 border-ink">
                  <div className="flex items-center justify-between mb-2">
                    <div className="flex items-center gap-2">
                      <Tag variant="coral" size="sm">인성/혼합</Tag>
                      <span className="text-sm text-neutral-500">
                        {behavioralQna.length}개 질문
                      </span>
                    </div>
                    <span className="font-mono font-bold">{behavioralAvg}점</span>
                  </div>
                  <div className="h-2 bg-neutral-200 overflow-hidden">
                    <motion.div
                      initial={{ width: 0 }}
                      animate={{ width: `${behavioralAvg}%` }}
                      transition={{ delay: 0.6, duration: 0.8 }}
                      className="h-full bg-accent-coral"
                    />
                  </div>
                </div>

                {/* Follow-up */}
                {followUpQna.length > 0 && (
                  <div className="p-4 bg-cream border-2 border-ink">
                    <div className="flex items-center justify-between mb-2">
                      <div className="flex items-center gap-2">
                        <Tag variant="lime" size="sm">꼬리 질문</Tag>
                        <span className="text-sm text-neutral-500">
                          {followUpQna.length}개 질문
                        </span>
                      </div>
                      <span className="font-mono font-bold">{followUpAvg}점</span>
                    </div>
                    <div className="h-2 bg-neutral-200 overflow-hidden">
                      <motion.div
                        initial={{ width: 0 }}
                        animate={{ width: `${followUpAvg}%` }}
                        transition={{ delay: 0.7, duration: 0.8 }}
                        className="h-full bg-accent-lime"
                      />
                    </div>
                  </div>
                )}
              </div>
            </div>
          </Card>
        </motion.div>

        {/* Strengths & Improvements Summary */}
        <motion.div
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ delay: 0.3 }}
          className="grid md:grid-cols-2 gap-6 mb-8"
        >
          {/* Strengths */}
          <Card className="p-6">
            <h3 className="font-display text-lg flex items-center gap-2 mb-4">
              <TrendingUp className="w-5 h-5 text-green-500" />
              강점 종합
            </h3>
            {allStrengths.length > 0 ? (
              <ul className="space-y-2">
                {Array.from(new Set(allStrengths)).slice(0, 5).map((s, i) => (
                  <li key={i} className="flex items-start gap-2 text-sm">
                    <CheckCircle2 className="w-4 h-4 text-green-500 mt-0.5 shrink-0" />
                    <span>{s}</span>
                  </li>
                ))}
              </ul>
            ) : (
              <p className="text-sm text-neutral-500">
                피드백에서 강점이 분석되지 않았습니다.
              </p>
            )}
          </Card>

          {/* Improvements */}
          <Card className="p-6">
            <h3 className="font-display text-lg flex items-center gap-2 mb-4">
              <TrendingDown className="w-5 h-5 text-accent-coral" />
              개선점 종합
            </h3>
            {allImprovements.length > 0 ? (
              <ul className="space-y-2">
                {Array.from(new Set(allImprovements)).slice(0, 5).map((s, i) => (
                  <li key={i} className="flex items-start gap-2 text-sm">
                    <XCircle className="w-4 h-4 text-accent-coral mt-0.5 shrink-0" />
                    <span>{s}</span>
                  </li>
                ))}
              </ul>
            ) : (
              <p className="text-sm text-neutral-500">
                피드백에서 개선점이 분석되지 않았습니다.
              </p>
            )}
          </Card>
        </motion.div>

        {/* Question & Answer Detail */}
        <motion.div
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ delay: 0.4 }}
        >
          <Card className="p-6">
            <h3 className="font-display text-xl mb-6 flex items-center gap-2">
              <MessageSquare className="w-5 h-5 text-accent-blue" />
              질문별 상세 결과
            </h3>

            <div className="space-y-4">
              {qnaList.map((qna, index) => {
                const feedbackData = getQnaFeedback(qna);
                const isExpanded = expandedQna === qna.id;
                const score = feedbackData.score;

                return (
                  <motion.div
                    key={qna.id}
                    initial={{ opacity: 0, x: -20 }}
                    animate={{ opacity: 1, x: 0 }}
                    transition={{ delay: 0.5 + index * 0.05 }}
                    className={`border-2 border-ink ${qna.isFollowUp ? 'ml-6 border-l-4 border-l-accent-lime' : ''}`}
                  >
                    {/* Question Header */}
                    <button
                      onClick={() => setExpandedQna(isExpanded ? null : qna.id)}
                      className="w-full p-4 text-left flex items-start justify-between gap-4 hover:bg-neutral-50 transition-colors"
                    >
                      <div className="flex-1">
                        <div className="flex items-center gap-2 mb-2">
                          <span className="font-mono text-sm text-neutral-400">
                            Q{index + 1}
                          </span>
                          <Tag
                            variant={qna.questionType === 'technical' ? 'blue' : qna.questionType === 'follow_up' ? 'lime' : 'coral'}
                            size="sm"
                          >
                            {qna.questionType === 'technical' ? '기술' : qna.questionType === 'follow_up' ? '꼬리' : '인성'}
                          </Tag>
                          {qna.isFollowUp && qna.followUpDepth && (
                            <span className="text-xs text-neutral-400">(깊이 {qna.followUpDepth})</span>
                          )}
                        </div>
                        <p className="font-display text-lg">{qna.questionText}</p>
                      </div>
                      <div className="shrink-0">
                        <ScoreRing score={score} size="sm" />
                      </div>
                    </button>

                    {/* Expanded Content */}
                    {isExpanded && (
                      <motion.div
                        initial={{ opacity: 0, height: 0 }}
                        animate={{ opacity: 1, height: 'auto' }}
                        exit={{ opacity: 0, height: 0 }}
                        className="border-t-2 border-dashed border-neutral-200"
                      >
                        {/* Answer */}
                        <div className="p-4 bg-neutral-50">
                          <h4 className="font-semibold text-sm mb-2 text-neutral-500">내 답변</h4>
                          <p className="text-sm whitespace-pre-wrap">
                            {qna.answerText || '답변하지 않음'}
                          </p>
                        </div>

                        {/* Feedback */}
                        {qna.answerText && (
                          <div className="p-4 space-y-4">
                            {/* Strengths */}
                            {feedbackData.strengths.length > 0 && (
                              <div className="p-3 bg-green-50 border-l-4 border-green-500">
                                <h5 className="font-semibold text-green-800 flex items-center gap-2 mb-2 text-sm">
                                  <CheckCircle2 className="w-4 h-4" />
                                  강점
                                </h5>
                                <ul className="space-y-1">
                                  {feedbackData.strengths.map((s, i) => (
                                    <li key={i} className="text-sm text-green-700">• {s}</li>
                                  ))}
                                </ul>
                              </div>
                            )}

                            {/* Improvements */}
                            {feedbackData.improvements.length > 0 && (
                              <div className="p-3 bg-orange-50 border-l-4 border-orange-500">
                                <h5 className="font-semibold text-orange-800 flex items-center gap-2 mb-2 text-sm">
                                  <XCircle className="w-4 h-4" />
                                  개선점
                                </h5>
                                <ul className="space-y-1">
                                  {feedbackData.improvements.map((s, i) => (
                                    <li key={i} className="text-sm text-orange-700">• {s}</li>
                                  ))}
                                </ul>
                              </div>
                            )}

                            {/* Default message if no feedback */}
                            {feedbackData.strengths.length === 0 &&
                             feedbackData.improvements.length === 0 && (
                              <p className="text-sm text-neutral-500 italic">
                                상세 피드백이 제공되지 않았습니다.
                              </p>
                            )}
                          </div>
                        )}
                      </motion.div>
                    )}
                  </motion.div>
                );
              })}
            </div>
          </Card>
        </motion.div>

        {/* Bottom Actions */}
        <motion.div
          initial={{ opacity: 0 }}
          animate={{ opacity: 1 }}
          transition={{ delay: 0.6 }}
          className="mt-8 flex justify-center gap-4"
        >
          <Link href="/statistics">
            <Button variant="secondary">
              전체 통계 보기
            </Button>
          </Link>
          <Link href={`/interview?jdId=${session.jdId}`}>
            <Button leftIcon={<RotateCcw className="w-4 h-4" />}>
              다시 연습하기
            </Button>
          </Link>
        </motion.div>
      </div>
    </div>
  );
}
