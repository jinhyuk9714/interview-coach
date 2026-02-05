'use client';

import { motion } from 'motion/react';
import { Card, ScoreRing } from '@/components/ui';
import {
  TrendingUp,
  TrendingDown,
  Target,
  Calendar,
  Award,
  BookOpen,
  Zap,
  ChevronUp,
  ChevronDown
} from 'lucide-react';

// Mock statistics data
const overallStats = {
  totalInterviews: 24,
  totalQuestions: 156,
  avgScore: 78,
  scoreTrend: +5,
  streakDays: 7,
  rank: 'A',
};

const categoryStats = [
  { category: 'Java', score: 85, total: 32, trend: +8, color: 'bg-accent-lime' },
  { category: 'Spring', score: 72, total: 28, trend: -3, color: 'bg-accent-coral' },
  { category: 'JPA', score: 58, total: 24, trend: +12, color: 'bg-accent-blue' },
  { category: 'Database', score: 65, total: 20, trend: +5, color: 'bg-accent-purple' },
  { category: 'Network', score: 70, total: 18, trend: 0, color: 'bg-neutral-400' },
  { category: '인성', score: 82, total: 34, trend: +2, color: 'bg-accent-coral' },
];

const weeklyActivity = [
  { day: '월', count: 3, score: 75 },
  { day: '화', count: 5, score: 82 },
  { day: '수', count: 2, score: 68 },
  { day: '목', count: 4, score: 79 },
  { day: '금', count: 6, score: 85 },
  { day: '토', count: 1, score: 72 },
  { day: '일', count: 3, score: 80 },
];

const recentProgress = [
  { date: '1주차', score: 62 },
  { date: '2주차', score: 68 },
  { date: '3주차', score: 71 },
  { date: '4주차', score: 78 },
];

const weakPoints = [
  { skill: 'JPA N+1 문제', score: 45, suggestion: 'Fetch Join 복습 권장' },
  { skill: '트랜잭션 격리수준', score: 52, suggestion: 'ACID 원리 복습' },
  { skill: 'Redis 캐싱 전략', score: 58, suggestion: 'Cache-Aside 패턴 학습' },
];

const achievements = [
  { icon: Zap, title: '첫 면접', description: '첫 모의 면접 완료', unlocked: true },
  { icon: Target, title: '10연속', description: '10개 연속 정답', unlocked: true },
  { icon: Award, title: '90점 달성', description: '평균 90점 이상', unlocked: false },
  { icon: BookOpen, title: '전문가', description: '100개 질문 완료', unlocked: true },
];

export default function StatisticsPage() {
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
                  {categoryStats.map((cat, index) => (
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
                  ))}
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
                          const x = (i / (recentProgress.length - 1)) * 100;
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
                        const x = (i / (recentProgress.length - 1)) * 100;
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
                  {weakPoints.map((point, index) => (
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
                  ))}
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
      </div>
    </div>
  );
}
