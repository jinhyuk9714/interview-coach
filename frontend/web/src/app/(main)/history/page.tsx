'use client';

import { useState, useEffect } from 'react';
import Link from 'next/link';
import { motion } from 'motion/react';
import { Card, Button, Tag, ScoreRing } from '@/components/ui';
import { interviewApi } from '@/lib/api';
import {
  Calendar,
  Clock,
  ChevronRight,
  FileText,
  Loader2,
  MessageSquare,
  TrendingUp,
} from 'lucide-react';
import { useAuthStore } from '@/stores/auth';

interface InterviewRecord {
  id: number;
  jdId: number;
  interviewType: string;
  status: string;
  totalQuestions: number;
  avgScore: number | null;
  startedAt: string;
  completedAt: string | null;
}

export default function HistoryPage() {
  const [interviews, setInterviews] = useState<InterviewRecord[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const { user, accessToken } = useAuthStore();

  useEffect(() => {
    const loadInterviews = async () => {
      try {
        console.log('Current user:', user);
        console.log('Has access token:', !!accessToken);
        console.log('Fetching interview history...');
        const response = await interviewApi.list();
        console.log('Interview API response:', response.data);
        setInterviews(response.data.interviews || []);
      } catch (err) {
        console.error('Failed to load interviews:', err);
        setError('면접 기록을 불러오는데 실패했습니다.');
      } finally {
        setIsLoading(false);
      }
    };

    loadInterviews();
  }, [user, accessToken]);

  const formatDate = (dateString: string) => {
    const date = new Date(dateString);
    return date.toLocaleDateString('ko-KR', {
      year: 'numeric',
      month: 'long',
      day: 'numeric',
    });
  };

  const formatTime = (dateString: string) => {
    const date = new Date(dateString);
    return date.toLocaleTimeString('ko-KR', {
      hour: '2-digit',
      minute: '2-digit',
    });
  };

  const getStatusLabel = (status: string) => {
    switch (status.toLowerCase()) {
      case 'completed':
        return { label: '완료', variant: 'lime' as const };
      case 'in_progress':
        return { label: '진행중', variant: 'blue' as const };
      default:
        return { label: status, variant: 'default' as const };
    }
  };

  const completedInterviews = interviews.filter(i => i.status.toLowerCase() === 'completed');
  const inProgressInterviews = interviews.filter(i => i.status.toLowerCase() === 'in_progress');

  if (isLoading) {
    return (
      <div className="py-12">
        <div className="max-w-4xl mx-auto px-4">
          <div className="flex items-center justify-center py-20">
            <Loader2 className="w-8 h-8 animate-spin text-neutral-400" />
            <span className="ml-3 text-neutral-500">면접 기록을 불러오는 중...</span>
          </div>
        </div>
      </div>
    );
  }

  if (error) {
    return (
      <div className="py-12">
        <div className="max-w-4xl mx-auto px-4">
          <Card className="p-8 text-center">
            <p className="text-red-500 mb-4">{error}</p>
            <Button onClick={() => window.location.reload()}>다시 시도</Button>
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
          <h1 className="text-display-md font-display mb-2">면접 기록</h1>
          <p className="text-neutral-600">
            지금까지 진행한 모의 면접 기록을 확인하세요
          </p>
        </motion.div>

        {/* Stats Summary */}
        <motion.div
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ delay: 0.1 }}
          className="grid grid-cols-3 gap-4 mb-8"
        >
          <Card className="p-4 text-center">
            <div className="text-3xl font-display font-bold text-ink mb-1">
              {completedInterviews.length}
            </div>
            <div className="text-sm text-neutral-500">완료한 면접</div>
          </Card>
          <Card className="p-4 text-center">
            <div className="text-3xl font-display font-bold text-accent-coral mb-1">
              {completedInterviews.length > 0
                ? Math.round(
                    completedInterviews.reduce((acc, i) => acc + (i.avgScore || 0), 0) /
                      completedInterviews.length
                  )
                : 0}
            </div>
            <div className="text-sm text-neutral-500">평균 점수</div>
          </Card>
          <Card className="p-4 text-center">
            <div className="text-3xl font-display font-bold text-accent-lime mb-1">
              {completedInterviews.reduce((acc, i) => acc + i.totalQuestions, 0)}
            </div>
            <div className="text-sm text-neutral-500">총 질문 수</div>
          </Card>
        </motion.div>

        {/* In Progress Interviews */}
        {inProgressInterviews.length > 0 && (
          <motion.div
            initial={{ opacity: 0, y: 20 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ delay: 0.2 }}
            className="mb-8"
          >
            <h2 className="text-lg font-semibold mb-4 flex items-center gap-2">
              <Clock className="w-5 h-5 text-blue-500" />
              진행 중인 면접
            </h2>
            <div className="space-y-3">
              {inProgressInterviews.map((interview) => (
                <Link key={interview.id} href={`/interview?resume=${interview.id}`}>
                  <Card className="p-4 hover:border-blue-500 transition-colors cursor-pointer">
                    <div className="flex items-center justify-between">
                      <div className="flex items-center gap-4">
                        <div className="w-12 h-12 bg-blue-100 border-2 border-blue-500 flex items-center justify-center">
                          <MessageSquare className="w-6 h-6 text-blue-600" />
                        </div>
                        <div>
                          <div className="flex items-center gap-2 mb-1">
                            <Tag variant="blue" size="sm">진행중</Tag>
                            <span className="text-sm text-neutral-500">
                              {interview.totalQuestions}개 질문
                            </span>
                          </div>
                          <p className="text-sm text-neutral-600">
                            {formatDate(interview.startedAt)} {formatTime(interview.startedAt)}
                          </p>
                        </div>
                      </div>
                      <ChevronRight className="w-5 h-5 text-neutral-400" />
                    </div>
                  </Card>
                </Link>
              ))}
            </div>
          </motion.div>
        )}

        {/* Completed Interviews */}
        <motion.div
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ delay: 0.3 }}
        >
          <h2 className="text-lg font-semibold mb-4 flex items-center gap-2">
            <FileText className="w-5 h-5" />
            완료된 면접 ({completedInterviews.length})
          </h2>

          {completedInterviews.length === 0 ? (
            <Card className="p-8 text-center">
              <div className="w-16 h-16 bg-neutral-100 mx-auto mb-4 flex items-center justify-center">
                <MessageSquare className="w-8 h-8 text-neutral-400" />
              </div>
              <h3 className="text-lg font-semibold mb-2">아직 완료된 면접이 없습니다</h3>
              <p className="text-neutral-500 mb-6">
                첫 모의 면접을 시작해보세요!
              </p>
              <Link href="/interview">
                <Button>면접 시작하기</Button>
              </Link>
            </Card>
          ) : (
            <div className="space-y-3">
              {completedInterviews.map((interview, index) => {
                const status = getStatusLabel(interview.status);
                return (
                  <motion.div
                    key={interview.id}
                    initial={{ opacity: 0, x: -20 }}
                    animate={{ opacity: 1, x: 0 }}
                    transition={{ delay: 0.1 * index }}
                  >
                    <Link href={`/interview/${interview.id}`}>
                      <Card className="p-4 hover:border-accent-coral transition-colors cursor-pointer group">
                        <div className="flex items-center justify-between">
                          <div className="flex items-center gap-4">
                            <ScoreRing
                              score={Math.round(interview.avgScore || 0)}
                              size="sm"
                            />
                            <div>
                              <div className="flex items-center gap-2 mb-1">
                                <Tag variant={status.variant} size="sm">
                                  {status.label}
                                </Tag>
                                <Tag
                                  variant={interview.interviewType === 'technical' ? 'blue' : 'coral'}
                                  size="sm"
                                >
                                  {interview.interviewType === 'technical' ? '기술 면접' : '인성 면접'}
                                </Tag>
                                <span className="text-sm text-neutral-500">
                                  {interview.totalQuestions}개 질문
                                </span>
                              </div>
                              <div className="flex items-center gap-3 text-sm text-neutral-600">
                                <span className="flex items-center gap-1">
                                  <Calendar className="w-4 h-4" />
                                  {formatDate(interview.startedAt)}
                                </span>
                                <span className="flex items-center gap-1">
                                  <Clock className="w-4 h-4" />
                                  {formatTime(interview.startedAt)}
                                </span>
                              </div>
                            </div>
                          </div>
                          <div className="flex items-center gap-3">
                            {interview.avgScore !== null && interview.avgScore >= 80 && (
                              <TrendingUp className="w-5 h-5 text-green-500" />
                            )}
                            <ChevronRight className="w-5 h-5 text-neutral-400 group-hover:text-accent-coral transition-colors" />
                          </div>
                        </div>
                      </Card>
                    </Link>
                  </motion.div>
                );
              })}
            </div>
          )}
        </motion.div>
      </div>
    </div>
  );
}
