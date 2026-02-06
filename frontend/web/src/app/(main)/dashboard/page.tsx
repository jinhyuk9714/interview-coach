'use client';

import { useMemo } from 'react';
import Link from 'next/link';
import { motion } from 'motion/react';
import { useQuery } from '@tanstack/react-query';
import { Button, Card, CardContent, Tag, ScoreRing } from '@/components/ui';
import { useAuthStore } from '@/stores/auth';
import { interviewApi, statisticsApi, jdApi } from '@/lib/api';
import {
  FileText,
  MessageSquare,
  TrendingUp,
  ArrowRight,
  Target,
  Zap,
  Calendar,
  ChevronRight,
  Loader2
} from 'lucide-react';
import { formatDate, parseUTCDate } from '@/lib/utils';

interface Interview {
  id: number;
  jdId: number;
  companyName?: string;
  position?: string;
  score?: number;
  avgScore?: number;  // API returns avgScore
  status: string;
  startedAt: string;
}

interface ApiStatistics {
  totalInterviews?: number;
  avgScore?: number;
  weakPoints?: Array<{ skill: string; score: number }>;
  weeklyActivity?: Array<{ day: string; count: number; score: number }>;
}

export default function DashboardPage() {
  const { user } = useAuthStore();

  // Fetch interviews
  const {
    data: interviewsData,
    isLoading: isInterviewsLoading,
    isError: isInterviewsError,
  } = useQuery({
    queryKey: ['interviews'],
    queryFn: () => interviewApi.list(),
  });

  // Fetch statistics
  const {
    data: statsData,
    isLoading: isStatsLoading,
    isError: isStatsError,
  } = useQuery({
    queryKey: ['statistics'],
    queryFn: () => statisticsApi.get(),
  });

  // Fetch JD list
  const {
    data: jdsData,
    isLoading: isJdsLoading,
    isError: isJdsError,
  } = useQuery({
    queryKey: ['jd-list'],
    queryFn: () => jdApi.list(),
  });

  const isLoading = isInterviewsLoading || isStatsLoading || isJdsLoading;
  const error = (isInterviewsError && isStatsError && isJdsError)
    ? '데이터를 불러오는데 실패했습니다.'
    : null;

  // Derive interviews from query data
  const interviews = useMemo<Interview[]>(() => {
    if (!interviewsData?.data) return [];
    const interviewData = interviewsData.data;
    // API returns {totalCount, interviews: []} format
    return Array.isArray(interviewData)
      ? interviewData
      : (interviewData?.interviews || []);
  }, [interviewsData]);

  const recentInterviews = useMemo(() => interviews.slice(0, 3), [interviews]);

  // Derive JD count
  const totalJds = useMemo(() => jdsData?.data?.length || 0, [jdsData]);

  // Derive statistics from query data
  const statistics = useMemo(() => {
    const apiStats: ApiStatistics | undefined = statsData?.data;

    if (apiStats) {
      // Calculate weeklyInterviews from weeklyActivity if available
      const weeklyInterviews = apiStats.weeklyActivity
        ? apiStats.weeklyActivity.reduce((sum: number, day: { count: number }) => sum + day.count, 0)
        : interviews.filter((i: Interview) => {
            const weekAgo = new Date();
            weekAgo.setDate(weekAgo.getDate() - 7);
            return parseUTCDate(i.startedAt) > weekAgo;
          }).length;

      return {
        totalInterviews: apiStats.totalInterviews ?? interviews.length,
        avgScore: apiStats.avgScore ?? 0,
        totalJds,
        weeklyInterviews,
        weakPoints: apiStats.weakPoints || [],
      };
    }

    // Fallback: calculate statistics from interviews
    const completedInterviews = interviews.filter(i => i.status === 'completed');
    const avgScore = completedInterviews.length > 0
      ? Math.round(completedInterviews.reduce((acc, i) => acc + (i.avgScore || i.score || 0), 0) / completedInterviews.length)
      : 0;

    const weekAgo = new Date();
    weekAgo.setDate(weekAgo.getDate() - 7);

    return {
      totalInterviews: interviews.length,
      avgScore,
      totalJds,
      weeklyInterviews: interviews.filter(i => parseUTCDate(i.startedAt) > weekAgo).length,
      weakPoints: [] as Array<{ skill: string; score: number }>,
    };
  }, [statsData, interviews, totalJds]);

  const quickStats = [
    { label: '총 면접 수', value: statistics.totalInterviews.toString(), icon: MessageSquare, color: 'bg-accent-lime' },
    { label: '평균 점수', value: statistics.avgScore.toString(), icon: Target, color: 'bg-accent-coral' },
    { label: '등록된 JD', value: statistics.totalJds.toString(), icon: FileText, color: 'bg-accent-blue' },
    { label: '이번 주 연습', value: `${statistics.weeklyInterviews}회`, icon: Calendar, color: 'bg-accent-purple' },
  ];

  const weakPoints = statistics.weakPoints || [];

  return (
    <div className="py-8">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        {/* Welcome Section */}
        <motion.div
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          className="mb-12"
        >
          <div className="flex flex-col lg:flex-row lg:items-end lg:justify-between gap-6">
            <div>
              <p className="text-neutral-500 font-mono text-sm mb-2">
                {new Date().toLocaleDateString('ko-KR', { weekday: 'long', month: 'long', day: 'numeric' })}
              </p>
              <h1 className="text-display-md font-display">
                안녕하세요, <span className="text-accent-coral italic">{user?.nickname}</span>님
              </h1>
              <p className="text-neutral-600 mt-2">오늘도 면접 준비 화이팅!</p>
            </div>
            <div className="flex gap-3">
              <Link href="/jd">
                <Button variant="secondary" leftIcon={<FileText className="w-4 h-4" />}>
                  JD 분석하기
                </Button>
              </Link>
              <Link href="/interview">
                <Button leftIcon={<Zap className="w-4 h-4" />}>
                  면접 시작
                </Button>
              </Link>
            </div>
          </div>
        </motion.div>

        {/* Error Alert */}
        {error && (
          <motion.div
            initial={{ opacity: 0, y: 10 }}
            animate={{ opacity: 1, y: 0 }}
            className="mb-8 p-4 bg-accent-coral/10 border-2 border-accent-coral flex items-center gap-3"
          >
            <TrendingUp className="w-5 h-5 text-accent-coral" />
            <p className="text-sm text-accent-coral">{error}</p>
          </motion.div>
        )}

        {/* Quick Stats */}
        <motion.div
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ delay: 0.1 }}
          className="grid grid-cols-2 lg:grid-cols-4 gap-4 mb-8"
        >
          {quickStats.map((stat, index) => {
            const Icon = stat.icon;
            return (
              <motion.div
                key={stat.label}
                initial={{ opacity: 0, y: 20 }}
                animate={{ opacity: 1, y: 0 }}
                transition={{ delay: 0.15 + index * 0.05 }}
              >
                <Card className="p-5">
                  <CardContent className="p-0">
                    <div className="flex items-start justify-between">
                      <div>
                        <p className="text-xs font-mono text-neutral-500 uppercase tracking-wider mb-1">
                          {stat.label}
                        </p>
                        <p className="text-3xl font-display">{stat.value}</p>
                      </div>
                      <div className={`w-10 h-10 ${stat.color} border-2 border-ink flex items-center justify-center`}>
                        <Icon className="w-5 h-5" />
                      </div>
                    </div>
                  </CardContent>
                </Card>
              </motion.div>
            );
          })}
        </motion.div>

        <div className="grid lg:grid-cols-3 gap-8">
          {/* Recent Interviews */}
          <motion.div
            initial={{ opacity: 0, y: 20 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ delay: 0.2 }}
            className="lg:col-span-2"
          >
            <Card className="p-6">
              <div className="flex items-center justify-between mb-6">
                <h2 className="text-xl font-display">최근 면접</h2>
                <Link href="/interview" className="text-sm text-neutral-500 hover:text-ink flex items-center gap-1">
                  전체 보기 <ChevronRight className="w-4 h-4" />
                </Link>
              </div>

              <div className="space-y-4">
                {isLoading ? (
                  <div className="p-8 text-center">
                    <Loader2 className="w-6 h-6 animate-spin mx-auto text-neutral-400" />
                    <p className="text-sm text-neutral-500 mt-2">불러오는 중...</p>
                  </div>
                ) : recentInterviews.length === 0 ? (
                  <div className="p-8 text-center text-neutral-500">
                    <MessageSquare className="w-8 h-8 mx-auto text-neutral-300 mb-2" />
                    <p className="text-sm">아직 면접 기록이 없습니다</p>
                  </div>
                ) : (
                  recentInterviews.map((interview, index) => (
                    <motion.div
                      key={interview.id}
                      initial={{ opacity: 0, x: -20 }}
                      animate={{ opacity: 1, x: 0 }}
                      transition={{ delay: 0.25 + index * 0.1 }}
                      className="flex items-center justify-between p-4 bg-cream border-2 border-transparent hover:border-ink transition-colors"
                    >
                      <div className="flex items-center gap-4">
                        <div className="w-12 h-12 bg-white border-2 border-ink flex items-center justify-center font-display text-lg">
                          {(interview.companyName || 'JD').charAt(0)}
                        </div>
                        <div>
                          <h3 className="font-sans font-semibold">{interview.companyName || `면접 #${interview.id}`}</h3>
                          <p className="text-sm text-neutral-500">{interview.position || '일반 면접'}</p>
                        </div>
                      </div>

                      <div className="flex items-center gap-4">
                        <div className="text-right hidden sm:block">
                          <p className="text-xs text-neutral-400 font-mono">
                            {formatDate(interview.startedAt)}
                          </p>
                        </div>
                        {interview.status === 'completed' && (interview.avgScore || interview.score) ? (
                          <ScoreRing score={interview.avgScore || interview.score || 0} size="sm" />
                        ) : (
                          <Tag variant="coral">진행중</Tag>
                        )}
                      </div>
                    </motion.div>
                  ))
                )}
              </div>

              <Link href="/interview">
                <Button variant="secondary" className="w-full mt-6" rightIcon={<ArrowRight className="w-4 h-4" />}>
                  새 면접 시작하기
                </Button>
              </Link>
            </Card>
          </motion.div>

          {/* Weak Points */}
          <motion.div
            initial={{ opacity: 0, y: 20 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ delay: 0.3 }}
          >
            <Card className="p-6">
              <div className="flex items-center justify-between mb-6">
                <h2 className="text-xl font-display">취약 분야</h2>
                <TrendingUp className="w-5 h-5 text-accent-coral" />
              </div>

              <div className="space-y-4">
                {weakPoints.length === 0 ? (
                  <div className="p-6 text-center text-neutral-500">
                    <TrendingUp className="w-8 h-8 mx-auto text-neutral-300 mb-2" />
                    <p className="text-sm">면접 기록이 쌓이면 취약 분야가 분석됩니다</p>
                  </div>
                ) : (
                  weakPoints.map((point, index) => (
                    <motion.div
                      key={point.skill}
                      initial={{ opacity: 0 }}
                      animate={{ opacity: 1 }}
                      transition={{ delay: 0.35 + index * 0.1 }}
                    >
                      <div className="flex items-center justify-between mb-2">
                        <span className="text-sm font-medium">{point.skill}</span>
                        <span className="text-xs font-mono text-neutral-500">{point.score}%</span>
                      </div>
                      <div className="h-2 bg-neutral-200 border border-ink">
                        <motion.div
                          initial={{ width: 0 }}
                          animate={{ width: `${point.score}%` }}
                          transition={{ delay: 0.5 + index * 0.1, duration: 0.5 }}
                          className="h-full bg-accent-coral"
                        />
                      </div>
                    </motion.div>
                  ))
                )}
              </div>

              {weakPoints.length > 0 && (
                <div className="mt-6 p-4 bg-accent-lime/20 border-2 border-dashed border-accent-lime">
                  <p className="text-sm">
                    <span className="font-semibold">팁:</span> 취약 분야를 집중적으로 연습해보세요.
                  </p>
                </div>
              )}

              <Link href="/statistics">
                <Button variant="ghost" className="w-full mt-4" size="sm">
                  상세 통계 보기
                </Button>
              </Link>
            </Card>
          </motion.div>
        </div>

        {/* Quick Actions */}
        <motion.div
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ delay: 0.4 }}
          className="mt-8 grid md:grid-cols-3 gap-4"
        >
          <Link href="/jd" className="group">
            <Card className="p-6 h-full hover:bg-accent-lime/5">
              <div className="flex items-start gap-4">
                <div className="w-12 h-12 bg-accent-lime border-2 border-ink flex items-center justify-center group-hover:rotate-6 transition-transform">
                  <FileText className="w-6 h-6" />
                </div>
                <div>
                  <h3 className="font-sans font-semibold mb-1">새 JD 분석</h3>
                  <p className="text-sm text-neutral-500">
                    채용공고를 붙여넣고 예상 질문을 받아보세요
                  </p>
                </div>
              </div>
            </Card>
          </Link>

          <Link href="/interview" className="group">
            <Card className="p-6 h-full hover:bg-accent-coral/5">
              <div className="flex items-start gap-4">
                <div className="w-12 h-12 bg-accent-coral border-2 border-ink flex items-center justify-center group-hover:rotate-6 transition-transform">
                  <MessageSquare className="w-6 h-6 text-white" />
                </div>
                <div>
                  <h3 className="font-sans font-semibold mb-1">빠른 연습</h3>
                  <p className="text-sm text-neutral-500">
                    기존 JD로 바로 면접 연습을 시작하세요
                  </p>
                </div>
              </div>
            </Card>
          </Link>

          <Link href="/statistics" className="group">
            <Card className="p-6 h-full hover:bg-accent-blue/5">
              <div className="flex items-start gap-4">
                <div className="w-12 h-12 bg-accent-blue border-2 border-ink flex items-center justify-center group-hover:rotate-6 transition-transform">
                  <TrendingUp className="w-6 h-6 text-white" />
                </div>
                <div>
                  <h3 className="font-sans font-semibold mb-1">학습 리포트</h3>
                  <p className="text-sm text-neutral-500">
                    나의 성장 추이와 취약점을 확인하세요
                  </p>
                </div>
              </div>
            </Card>
          </Link>
        </motion.div>
      </div>
    </div>
  );
}
