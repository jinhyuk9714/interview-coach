'use client';

import { useState, useEffect } from 'react';
import { motion } from 'motion/react';
import { Card, ScoreRing } from '@/components/ui';
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
  Loader2
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
  const [isLoading, setIsLoading] = useState(true);
  const [overallStats, setOverallStats] = useState<OverallStats>({
    totalInterviews: 0,
    totalQuestions: 0,
    avgScore: 0,
    scoreTrend: 0,
    streakDays: 0,
    rank: '-',
  });
  const [categoryStats, setCategoryStats] = useState<CategoryStat[]>([]);
  const [weeklyActivity, setWeeklyActivity] = useState<Array<{ day: string; count: number; score: number }>>([]);
  const [recentProgress, setRecentProgress] = useState<Array<{ date: string; score: number }>>([]);
  const [weakPoints, setWeakPoints] = useState<WeakPoint[]>([]);
  const [achievements, setAchievements] = useState<Achievement[]>(defaultAchievements);

  useEffect(() => {
    const fetchStatistics = async () => {
      setIsLoading(true);
      try {
        const [statsRes, interviewsRes] = await Promise.allSettled([
          statisticsApi.get(),
          interviewApi.list(),
        ]);

        // Process overall stats
        if (statsRes.status === 'fulfilled' && statsRes.value.data) {
          const data = statsRes.value.data;
          setOverallStats({
            totalInterviews: data.totalInterviews || 0,
            totalQuestions: data.totalQuestions || 0,
            avgScore: data.avgScore || 0,
            scoreTrend: data.scoreTrend || 0,
            streakDays: data.streakDays || 0,
            rank: data.rank || getRank(data.avgScore || 0),
          });

          if (data.categoryStats) {
            setCategoryStats(data.categoryStats.map((cat: CategoryStat, i: number) => ({
              ...cat,
              color: categoryColors[i % categoryColors.length],
            })));
          }

          if (data.weeklyActivity) {
            setWeeklyActivity(data.weeklyActivity);
          }

          if (data.recentProgress) {
            setRecentProgress(data.recentProgress);
          }

          if (data.weakPoints) {
            setWeakPoints(data.weakPoints);
          }

          // Update achievements based on stats
          const updatedAchievements = [...defaultAchievements];
          if (data.totalInterviews >= 1) updatedAchievements[0].unlocked = true;
          if (data.totalQuestions >= 10) updatedAchievements[1].unlocked = true;
          if (data.avgScore >= 90) updatedAchievements[2].unlocked = true;
          if (data.totalQuestions >= 100) updatedAchievements[3].unlocked = true;
          setAchievements(updatedAchievements);
        } else if (interviewsRes.status === 'fulfilled') {
          // Fallback: calculate from interviews
          const interviews = interviewsRes.value.data || [];
          const completedInterviews = interviews.filter((i: { status: string }) => i.status === 'completed');
          const totalScore = completedInterviews.reduce((acc: number, i: { score?: number }) => acc + (i.score || 0), 0);
          const avgScore = completedInterviews.length > 0 ? Math.round(totalScore / completedInterviews.length) : 0;

          setOverallStats({
            totalInterviews: interviews.length,
            totalQuestions: interviews.length * 5, // Approximate
            avgScore,
            scoreTrend: 0,
            streakDays: 0,
            rank: getRank(avgScore),
          });

          const updatedAchievements = [...defaultAchievements];
          if (interviews.length >= 1) updatedAchievements[0].unlocked = true;
          setAchievements(updatedAchievements);
        }
      } catch (err) {
        console.error('Failed to fetch statistics:', err);
      } finally {
        setIsLoading(false);
      }
    };

    fetchStatistics();
  }, []);

  const getRank = (score: number): string => {
    if (score >= 90) return 'S';
    if (score >= 80) return 'A';
    if (score >= 70) return 'B';
    if (score >= 60) return 'C';
    if (score >= 50) return 'D';
    return '-';
  };
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
          <div className="flex items-center justify-center py-20">
            <Loader2 className="w-8 h-8 animate-spin text-neutral-400" />
            <p className="ml-3 text-neutral-500">통계를 불러오는 중...</p>
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
                    {weeklyActivity.map((day, index) => (
                      <motion.div
                        key={day.day}
                        initial={{ height: 0 }}
                        animate={{ height: `${(day.count / 6) * 100}%` }}
                        transition={{ delay: 0.5 + index * 0.1, duration: 0.5 }}
                        className="flex-1 flex flex-col items-center"
                      >
                        <div
                          className="w-full bg-accent-lime border-2 border-ink relative group cursor-pointer"
                          style={{ height: `${(day.count / 6) * 100}%`, minHeight: '20px' }}
                        >
                          <div className="absolute -top-8 left-1/2 -translate-x-1/2 bg-ink text-paper px-2 py-1 text-xs font-mono opacity-0 group-hover:opacity-100 transition-opacity whitespace-nowrap">
                            {day.count}회 / {day.score}점
                          </div>
                        </div>
                        <span className="mt-2 text-xs font-mono text-neutral-500">{day.day}</span>
                      </motion.div>
                    ))}
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
                  <div className="relative h-48">
                    {/* Y-axis labels */}
                    <div className="absolute left-0 top-0 bottom-0 w-8 flex flex-col justify-between text-xs font-mono text-neutral-400">
                      <span>100</span>
                      <span>75</span>
                      <span>50</span>
                      <span>25</span>
                      <span>0</span>
                    </div>

                    {/* Chart area */}
                    <div className="ml-10 h-full relative">
                      {/* Grid lines */}
                      {[0, 25, 50, 75, 100].map((line) => (
                        <div
                          key={line}
                          className="absolute left-0 right-0 border-t border-dashed border-neutral-200"
                          style={{ top: `${100 - line}%` }}
                        />
                      ))}

                      {/* Data points and line */}
                      <svg className="absolute inset-0 w-full h-full overflow-visible">
                        <motion.polyline
                          points={recentProgress.map((p, i) => {
                            const x = recentProgress.length > 1 ? (i / (recentProgress.length - 1)) * 100 : 50;
                            const y = 100 - p.score;
                            return `${x}%,${y}%`;
                          }).join(' ')}
                          fill="none"
                          stroke="#2E5CFF"
                          strokeWidth="3"
                          initial={{ pathLength: 0 }}
                          animate={{ pathLength: 1 }}
                          transition={{ duration: 1.5, delay: 0.6 }}
                        />
                        {recentProgress.map((p, i) => {
                          const x = recentProgress.length > 1 ? (i / (recentProgress.length - 1)) * 100 : 50;
                          const y = 100 - p.score;
                          return (
                            <motion.circle
                              key={i}
                              cx={`${x}%`}
                              cy={`${y}%`}
                              r="6"
                              fill="#2E5CFF"
                              stroke="white"
                              strokeWidth="2"
                              initial={{ scale: 0 }}
                              animate={{ scale: 1 }}
                              transition={{ delay: 0.8 + i * 0.1 }}
                            />
                          );
                        })}
                      </svg>
                    </div>

                    {/* X-axis labels */}
                    <div className="ml-10 flex justify-between mt-2 text-xs font-mono text-neutral-400">
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
                  상위 15% 수준입니다
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
