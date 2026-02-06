'use client';

import { useMemo } from 'react';
import { motion } from 'motion/react';
import { useQuery } from '@tanstack/react-query';
import { Card, ScoreRing, Skeleton } from '@/components/ui';
import { statisticsApi, interviewApi } from '@/lib/api';
import {
  TrendingUp,
  TrendingDown,
  Target,
  Calendar,
  Award,
  BookOpen,
  Zap,
  ChevronUp,
  ChevronDown,
} from 'lucide-react';

interface OverallStats {
  totalInterviews: number;
  totalQuestions: number;
  avgScore: number;
  scoreTrend: number;
  streakDays: number;
  rank: string;
}

interface CategoryStat {
  category: string;
  score: number;
  total: number;
  trend: number;
  color: string;
}

interface WeakPoint {
  skill: string;
  score: number;
  suggestion: string;
}

interface Achievement {
  icon: typeof Zap;
  title: string;
  description: string;
  unlocked: boolean;
}

const defaultAchievements: Achievement[] = [
  { icon: Zap, title: '첫 면접', description: '첫 모의 면접 완료', unlocked: false },
  { icon: Target, title: '10연속', description: '10개 연속 정답', unlocked: false },
  { icon: Award, title: '90점 달성', description: '평균 90점 이상', unlocked: false },
  { icon: BookOpen, title: '전문가', description: '100개 질문 완료', unlocked: false },
];

const categoryColors = [
  'bg-accent-lime',
  'bg-accent-coral',
  'bg-accent-blue',
  'bg-accent-purple',
  'bg-neutral-400',
];

export default function StatisticsPage() {
  const getRank = (score: number): string => {
    if (score >= 90) return 'S';
    if (score >= 80) return 'A';
    if (score >= 70) return 'B';
    if (score >= 60) return 'C';
    if (score >= 50) return 'D';
    return '-';
  };

  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  const { data: statsData, isLoading: isStatsLoading } = useQuery<any>({
    queryKey: ['statistics'],
    queryFn: () => statisticsApi.get().then(res => res.data),
  });

  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  const { data: interviewsData } = useQuery<any>({
    queryKey: ['interviews'],
    queryFn: () => interviewApi.list().then(res => res.data),
  });

  const isLoading = isStatsLoading;

  const { overallStats, categoryStats, weeklyActivity, recentProgress, weakPoints, achievements } = useMemo(() => {
    const defaultOverall: OverallStats = { totalInterviews: 0, totalQuestions: 0, avgScore: 0, scoreTrend: 0, streakDays: 0, rank: '-' };
    let overall = defaultOverall;
    let cats: CategoryStat[] = [];
    let weekly: Array<{ day: string; count: number; score: number }> = [];
    let progress: Array<{ date: string; score: number }> = [];
    let weak: WeakPoint[] = [];
    const achs = [...defaultAchievements];

    if (statsData) {
      const data = statsData;
      overall = {
        totalInterviews: data.totalInterviews || 0,
        totalQuestions: data.totalQuestions || 0,
        avgScore: data.avgScore || 0,
        scoreTrend: data.scoreTrend || 0,
        streakDays: data.streakDays || 0,
        rank: data.rank || getRank(data.avgScore || 0),
      };

      if (data.categoryStats?.length > 0) {
        cats = data.categoryStats.map((cat: CategoryStat, i: number) => ({
          category: cat.category, score: cat.score, total: cat.total,
          trend: cat.trend || 0, color: categoryColors[i % categoryColors.length],
        }));
      }
      if (data.weeklyActivity) weekly = data.weeklyActivity;
      if (data.recentProgress) progress = data.recentProgress;
      if (data.weakPoints?.length > 0) weak = data.weakPoints;

      if (data.totalInterviews >= 1) achs[0].unlocked = true;
      if (data.totalQuestions >= 10) achs[1].unlocked = true;
      if (data.avgScore >= 90) achs[2].unlocked = true;
      if (data.totalQuestions >= 100) achs[3].unlocked = true;
    } else if (interviewsData) {
      const interviews = Array.isArray(interviewsData) ? interviewsData : (interviewsData.interviews || []);
      const completed = interviews.filter((i: { status: string }) => i.status === 'completed');
      const totalScore = completed.reduce((acc: number, i: { score?: number }) => acc + (i.score || 0), 0);
      const avgScore = completed.length > 0 ? Math.round(totalScore / completed.length) : 0;
      overall = { totalInterviews: interviews.length, totalQuestions: interviews.length * 5, avgScore, scoreTrend: 0, streakDays: 0, rank: getRank(avgScore) };
      if (interviews.length >= 1) achs[0].unlocked = true;
    }

    return { overallStats: overall, categoryStats: cats, weeklyActivity: weekly, recentProgress: progress, weakPoints: weak, achievements: achs };
  }, [statsData, interviewsData]);

  return (
    <div className="py-8">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        {/* Header */}
        <motion.div
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          className="mb-8"
        >
          <h1 className="text-display-md font-display mb-2">학습 통계</h1>
          <p className="text-neutral-600">
            나의 면접 준비 현황을 한눈에 확인하세요
          </p>
        </motion.div>

        {isLoading && (
          <div>
            <div className="grid grid-cols-2 lg:grid-cols-4 gap-4 mb-8">
              {[...Array(4)].map((_, i) => (
                <Card key={i} className="p-5">
                  <Skeleton className="h-4 w-16 mb-4" />
                  <Skeleton className="h-9 w-12 mb-1" />
                  <Skeleton className="h-3 w-8" />
                </Card>
              ))}
            </div>
            <div className="grid lg:grid-cols-3 gap-8">
              <div className="lg:col-span-2 space-y-8">
                <Card className="p-6">
                  <Skeleton className="h-6 w-40 mb-6" />
                  <div className="space-y-4">
                    {[...Array(4)].map((_, i) => (
                      <div key={i}>
                        <Skeleton className="h-4 w-1/3 mb-2" />
                        <Skeleton className="h-3 w-full" />
                      </div>
                    ))}
                  </div>
                </Card>
                <Card className="p-6">
                  <Skeleton className="h-6 w-32 mb-6" />
                  <Skeleton className="h-40 w-full" />
                </Card>
              </div>
              <div className="space-y-8">
                <Card className="p-6 text-center">
                  <Skeleton className="h-4 w-20 mx-auto mb-4" />
                  <Skeleton className="h-24 w-24 mx-auto rounded-full mb-4" />
                  <Skeleton className="h-8 w-16 mx-auto" />
                </Card>
              </div>
            </div>
          </div>
        )}

        {!isLoading && (
          <>
        {/* Overview Cards */}
        <motion.div
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ delay: 0.1 }}
          className="grid grid-cols-2 lg:grid-cols-4 gap-4 mb-8"
        >
          <Card className="p-5">
            <div className="flex items-center justify-between mb-4">
              <span className="text-xs font-mono text-neutral-500 uppercase">총 면접</span>
              <Calendar className="w-4 h-4 text-neutral-400" />
            </div>
            <div className="text-3xl font-display">{overallStats.totalInterviews}</div>
            <div className="text-xs text-neutral-500 mt-1">회</div>
          </Card>

          <Card className="p-5">
            <div className="flex items-center justify-between mb-4">
              <span className="text-xs font-mono text-neutral-500 uppercase">총 질문</span>
              <BookOpen className="w-4 h-4 text-neutral-400" />
            </div>
            <div className="text-3xl font-display">{overallStats.totalQuestions}</div>
            <div className="text-xs text-neutral-500 mt-1">개</div>
          </Card>

          <Card className="p-5">
            <div className="flex items-center justify-between mb-4">
              <span className="text-xs font-mono text-neutral-500 uppercase">평균 점수</span>
              <div className={`flex items-center gap-1 text-xs ${
                overallStats.scoreTrend > 0 ? 'text-green-500' : 'text-red-500'
              }`}>
                {overallStats.scoreTrend > 0 ? <ChevronUp className="w-3 h-3" /> : <ChevronDown className="w-3 h-3" />}
                {Math.abs(overallStats.scoreTrend)}%
              </div>
            </div>
            <div className="text-3xl font-display">{overallStats.avgScore}</div>
            <div className="text-xs text-neutral-500 mt-1">점</div>
          </Card>

          <Card className="p-5">
            <div className="flex items-center justify-between mb-4">
              <span className="text-xs font-mono text-neutral-500 uppercase">연속 학습</span>
              <Zap className="w-4 h-4 text-accent-coral" />
            </div>
            <div className="text-3xl font-display">{overallStats.streakDays}</div>
            <div className="text-xs text-neutral-500 mt-1">일</div>
          </Card>
        </motion.div>

        <div className="grid lg:grid-cols-3 gap-8">
          {/* Left Column */}
          <div className="lg:col-span-2 space-y-8">
            {/* Category Performance */}
            <motion.div
              initial={{ opacity: 0, y: 20 }}
              animate={{ opacity: 1, y: 0 }}
              transition={{ delay: 0.2 }}
            >
              <Card className="p-6">
                <h2 className="text-xl font-display mb-6 flex items-center gap-2">
                  <Target className="w-5 h-5 text-accent-coral" />
                  카테고리별 성적
                </h2>

                <div className="space-y-4">
                  {categoryStats.length === 0 ? (
                    <div className="p-8 text-center text-neutral-500">
                      <Target className="w-8 h-8 mx-auto text-neutral-300 mb-2" />
                      <p className="text-sm">면접 기록이 쌓이면 카테고리별 통계가 표시됩니다</p>
                    </div>
                  ) : (
                    categoryStats.map((cat, index) => (
                      <motion.div
                        key={cat.category}
                        initial={{ opacity: 0, x: -20 }}
                        animate={{ opacity: 1, x: 0 }}
                        transition={{ delay: 0.3 + index * 0.05 }}
                      >
                        <div className="flex items-center justify-between mb-2">
                          <div className="flex items-center gap-3">
                            <div className={`w-3 h-3 ${cat.color}`} />
                            <span className="font-semibold">{cat.category}</span>
                            <span className="text-xs text-neutral-400 font-mono">
                              ({cat.total}문제)
                            </span>
                          </div>
                          <div className="flex items-center gap-3">
                            <span className="font-mono font-bold">{cat.score}%</span>
                            {cat.trend !== 0 && (
                              <span className={`flex items-center text-xs ${
                                cat.trend > 0 ? 'text-green-500' : 'text-red-500'
                              }`}>
                                {cat.trend > 0 ? <TrendingUp className="w-3 h-3" /> : <TrendingDown className="w-3 h-3" />}
                                {Math.abs(cat.trend)}
                              </span>
                            )}
                          </div>
                        </div>
                        <div className="h-3 bg-neutral-100 border border-ink overflow-hidden">
                          <motion.div
                            initial={{ width: 0 }}
                            animate={{ width: `${cat.score}%` }}
                            transition={{ delay: 0.5 + index * 0.1, duration: 0.8 }}
                            className={`h-full ${cat.color}`}
                          />
                        </div>
                      </motion.div>
                    ))
                  )}
                </div>
              </Card>
            </motion.div>

            {/* Weekly Activity */}
            <motion.div
              initial={{ opacity: 0, y: 20 }}
              animate={{ opacity: 1, y: 0 }}
              transition={{ delay: 0.4 }}
            >
              <Card className="p-6">
                <h2 className="text-xl font-display mb-6">주간 활동</h2>

                {weeklyActivity.length === 0 ? (
                  <div className="h-40 flex items-center justify-center text-neutral-500">
                    <div className="text-center">
                      <Calendar className="w-8 h-8 mx-auto text-neutral-300 mb-2" />
                      <p className="text-sm">이번 주 활동 기록이 없습니다</p>
                    </div>
                  </div>
                ) : (
                  <div className="flex items-end justify-between h-40 gap-2">
                    {weeklyActivity.map((day, index) => {
                      // Calculate max count for normalization
                      const maxCount = Math.max(...weeklyActivity.map(d => d.count), 1);
                      const heightPercent = day.count > 0 ? Math.max((day.count / maxCount) * 100, 15) : 5;

                      return (
                        <motion.div
                          key={day.day}
                          initial={{ opacity: 0, scaleY: 0 }}
                          animate={{ opacity: 1, scaleY: 1 }}
                          transition={{ delay: 0.5 + index * 0.1, duration: 0.5 }}
                          className="flex-1 flex flex-col items-center justify-end h-full"
                          style={{ originY: 1 }}
                        >
                          <div
                            className={`w-full border-2 border-ink relative group cursor-pointer transition-colors ${
                              day.count > 0 ? 'bg-accent-lime' : 'bg-neutral-100'
                            }`}
                            style={{ height: `${heightPercent}%` }}
                          >
                            <div className="absolute -top-10 left-1/2 -translate-x-1/2 bg-ink text-paper px-2 py-1 text-xs font-mono opacity-0 group-hover:opacity-100 transition-opacity whitespace-nowrap z-10">
                              {day.count}회 {day.score > 0 ? `/ ${day.score}점` : ''}
                            </div>
                          </div>
                          <span className="mt-2 text-xs font-mono text-neutral-500">{day.day}</span>
                        </motion.div>
                      );
                    })}
                  </div>
                )}
              </Card>
            </motion.div>

            {/* Progress Chart */}
            <motion.div
              initial={{ opacity: 0, y: 20 }}
              animate={{ opacity: 1, y: 0 }}
              transition={{ delay: 0.5 }}
            >
              <Card className="p-6">
                <h2 className="text-xl font-display mb-6 flex items-center gap-2">
                  <TrendingUp className="w-5 h-5 text-accent-blue" />
                  성장 추이
                </h2>

                {recentProgress.length === 0 ? (
                  <div className="h-48 flex items-center justify-center text-neutral-500">
                    <div className="text-center">
                      <TrendingUp className="w-8 h-8 mx-auto text-neutral-300 mb-2" />
                      <p className="text-sm">면접 기록이 쌓이면 성장 추이가 표시됩니다</p>
                    </div>
                  </div>
                ) : (
                  <div className="relative h-52">
                    {/* Y-axis labels */}
                    <div className="absolute left-0 top-0 h-40 w-8 flex flex-col justify-between text-xs font-mono text-neutral-400">
                      <span>100</span>
                      <span>75</span>
                      <span>50</span>
                      <span>25</span>
                      <span>0</span>
                    </div>

                    {/* Chart area */}
                    <div className="ml-10 h-40 relative bg-neutral-50/50 border border-neutral-100">
                      {/* Grid lines */}
                      {[25, 50, 75].map((line) => (
                        <div
                          key={line}
                          className="absolute left-0 right-0 border-t border-dashed border-neutral-200"
                          style={{ top: `${100 - line}%` }}
                        />
                      ))}

                      {/* Line chart using SVG */}
                      <svg
                        className="absolute inset-0 w-full h-full"
                        viewBox="0 0 100 100"
                        preserveAspectRatio="none"
                        style={{ overflow: 'visible' }}
                      >
                        <motion.polyline
                          points={recentProgress.map((p, i) => {
                            const x = recentProgress.length > 1 ? (i / (recentProgress.length - 1)) * 100 : 50;
                            const y = 100 - p.score;
                            return `${x},${y}`;
                          }).join(' ')}
                          fill="none"
                          stroke="#2E5CFF"
                          strokeWidth="2"
                          strokeLinejoin="round"
                          strokeLinecap="round"
                          vectorEffect="non-scaling-stroke"
                          initial={{ pathLength: 0 }}
                          animate={{ pathLength: 1 }}
                          transition={{ duration: 1, delay: 0.3 }}
                        />
                      </svg>

                      {/* Data points as positioned divs (not affected by SVG scaling) */}
                      {recentProgress.map((p, i) => {
                        const xPercent = recentProgress.length > 1 ? (i / (recentProgress.length - 1)) * 100 : 50;
                        const yPercent = 100 - p.score;
                        return (
                          <motion.div
                            key={i}
                            className="absolute w-3 h-3 bg-accent-blue border-2 border-white rounded-full shadow-sm cursor-pointer hover:scale-125 transition-transform"
                            style={{
                              left: `${xPercent}%`,
                              top: `${yPercent}%`,
                              transform: 'translate(-50%, -50%)',
                            }}
                            initial={{ scale: 0 }}
                            animate={{ scale: 1 }}
                            transition={{ delay: 0.5 + i * 0.1 }}
                            title={`${p.date}: ${p.score}점`}
                          />
                        );
                      })}
                    </div>

                    {/* X-axis labels */}
                    <div className="ml-10 flex justify-between mt-3 text-xs font-mono text-neutral-400">
                      {recentProgress.map((p) => (
                        <span key={p.date}>{p.date}</span>
                      ))}
                    </div>
                  </div>
                )}
              </Card>
            </motion.div>
          </div>

          {/* Right Column */}
          <div className="space-y-8">
            {/* Overall Score */}
            <motion.div
              initial={{ opacity: 0, x: 20 }}
              animate={{ opacity: 1, x: 0 }}
              transition={{ delay: 0.3 }}
            >
              <Card className="p-6 text-center">
                <h2 className="text-sm font-mono text-neutral-500 uppercase tracking-wider mb-4">
                  종합 등급
                </h2>
                <motion.div
                  initial={{ scale: 0 }}
                  animate={{ scale: 1 }}
                  transition={{ type: 'spring', bounce: 0.5, delay: 0.5 }}
                  className="mb-4"
                >
                  <ScoreRing score={overallStats.avgScore} size="lg" />
                </motion.div>
                <div className="inline-flex items-center gap-2 px-4 py-2 bg-accent-lime border-2 border-ink">
                  <Award className="w-5 h-5" />
                  <span className="font-display text-2xl">{overallStats.rank}</span>
                </div>
                <p className="text-sm text-neutral-500 mt-4">
                  {overallStats.rank === 'S' && '상위 5% 수준입니다'}
                  {overallStats.rank === 'A' && '상위 15% 수준입니다'}
                  {overallStats.rank === 'B' && '상위 30% 수준입니다'}
                  {overallStats.rank === 'C' && '상위 50% 수준입니다'}
                  {overallStats.rank === 'D' && '더 노력이 필요합니다'}
                  {overallStats.rank === '-' && '면접 연습을 시작해보세요'}
                </p>
              </Card>
            </motion.div>

            {/* Weak Points */}
            <motion.div
              initial={{ opacity: 0, x: 20 }}
              animate={{ opacity: 1, x: 0 }}
              transition={{ delay: 0.4 }}
            >
              <Card className="p-6">
                <h2 className="text-xl font-display mb-4 flex items-center gap-2">
                  <TrendingDown className="w-5 h-5 text-accent-coral" />
                  취약 분야
                </h2>

                <div className="space-y-4">
                  {weakPoints.length === 0 ? (
                    <div className="p-6 text-center text-neutral-500">
                      <TrendingDown className="w-8 h-8 mx-auto text-neutral-300 mb-2" />
                      <p className="text-sm">취약 분야가 아직 분석되지 않았습니다</p>
                    </div>
                  ) : (
                    weakPoints.map((point, index) => (
                      <motion.div
                        key={point.skill}
                        initial={{ opacity: 0 }}
                        animate={{ opacity: 1 }}
                        transition={{ delay: 0.5 + index * 0.1 }}
                        className="p-3 bg-cream border-l-4 border-accent-coral"
                      >
                        <div className="flex items-center justify-between mb-1">
                          <span className="font-semibold text-sm">{point.skill}</span>
                          <span className="font-mono text-accent-coral">{point.score}%</span>
                        </div>
                        <p className="text-xs text-neutral-500">{point.suggestion}</p>
                      </motion.div>
                    ))
                  )}
                </div>
              </Card>
            </motion.div>

            {/* Achievements */}
            <motion.div
              initial={{ opacity: 0, x: 20 }}
              animate={{ opacity: 1, x: 0 }}
              transition={{ delay: 0.5 }}
            >
              <Card className="p-6">
                <h2 className="text-xl font-display mb-4 flex items-center gap-2">
                  <Award className="w-5 h-5 text-accent-blue" />
                  업적
                </h2>

                <div className="grid grid-cols-2 gap-3">
                  {achievements.map((ach, index) => {
                    const Icon = ach.icon;
                    return (
                      <motion.div
                        key={ach.title}
                        initial={{ opacity: 0, scale: 0.9 }}
                        animate={{ opacity: 1, scale: 1 }}
                        transition={{ delay: 0.6 + index * 0.1 }}
                        className={`p-3 border-2 border-ink text-center ${
                          ach.unlocked ? 'bg-white' : 'bg-neutral-100 opacity-50'
                        }`}
                      >
                        <div className={`w-10 h-10 mx-auto mb-2 flex items-center justify-center ${
                          ach.unlocked ? 'bg-accent-lime' : 'bg-neutral-300'
                        }`}>
                          <Icon className="w-5 h-5" />
                        </div>
                        <h4 className="font-semibold text-xs">{ach.title}</h4>
                        <p className="text-[10px] text-neutral-500">{ach.description}</p>
                      </motion.div>
                    );
                  })}
                </div>
              </Card>
            </motion.div>
          </div>
        </div>
        </>
        )}
      </div>
    </div>
  );
}
