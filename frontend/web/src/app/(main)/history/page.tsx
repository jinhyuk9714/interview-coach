'use client';

import { useState, useEffect, useCallback, useRef } from 'react';
import Link from 'next/link';
import { motion } from 'motion/react';
import { useQuery } from '@tanstack/react-query';
import { Card, Button, Tag, ScoreRing, Skeleton } from '@/components/ui';
import { interviewApi } from '@/lib/api';
import { formatDate, formatTime } from '@/lib/utils';
import {
  Calendar,
  Clock,
  ChevronRight,
  FileText,
  Loader2,
  MessageSquare,
  Search,
  TrendingUp,
  X,
} from 'lucide-react';
import { useAuthStore } from '@/stores/auth';
import { toast } from 'sonner';

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
  const [filteredInterviews, setFilteredInterviews] = useState<InterviewRecord[] | null>(null);
  const [searchKeyword, setSearchKeyword] = useState('');
  const [isSearching, setIsSearching] = useState(false);
  const { user, accessToken } = useAuthStore();
  const debounceTimerRef = useRef<NodeJS.Timeout | null>(null);

  const { data: interviews = [], isLoading, error: fetchError } = useQuery<InterviewRecord[]>({
    queryKey: ['interviews', user?.id],
    queryFn: () => interviewApi.list().then(res => res.data.interviews || []),
    enabled: !!accessToken,
  });
  const error = fetchError ? '면접 기록을 불러오는데 실패했습니다.' : null;

  const handleSearch = useCallback(async (keyword: string) => {
    const trimmed = keyword.trim();
    if (!trimmed) {
      setFilteredInterviews(null);
      return;
    }

    setIsSearching(true);
    try {
      const response = await interviewApi.search(trimmed);
      setFilteredInterviews(response.data.interviews || []);
    } catch {
      setFilteredInterviews(null);
      toast.error('검색에 실패했습니다.');
    } finally {
      setIsSearching(false);
    }
  }, []);

  const handleSearchInputChange = useCallback((value: string) => {
    setSearchKeyword(value);

    if (debounceTimerRef.current) {
      clearTimeout(debounceTimerRef.current);
    }

    if (!value.trim()) {
      setFilteredInterviews(null);
      return;
    }

    debounceTimerRef.current = setTimeout(() => {
      handleSearch(value);
    }, 300);
  }, [handleSearch]);

  const handleSearchKeyDown = useCallback((e: React.KeyboardEvent<HTMLInputElement>) => {
    if (e.key === 'Enter') {
      if (debounceTimerRef.current) {
        clearTimeout(debounceTimerRef.current);
      }
      handleSearch(searchKeyword);
    }
  }, [handleSearch, searchKeyword]);

  const clearSearch = useCallback(() => {
    setSearchKeyword('');
    setFilteredInterviews(null);
    if (debounceTimerRef.current) {
      clearTimeout(debounceTimerRef.current);
    }
  }, []);

  // Clean up debounce timer on unmount
  useEffect(() => {
    return () => {
      if (debounceTimerRef.current) {
        clearTimeout(debounceTimerRef.current);
      }
    };
  }, []);

  const getStatusLabel = (status: string) => {
    switch (status.toLowerCase()) {
      case 'completed':
        return { label: '완료', variant: 'lime' as const };
      case 'in_progress':
        return { label: '진행중', variant: 'blue' as const };
      case 'paused':
        return { label: '일시정지', variant: 'purple' as const };
      default:
        return { label: status, variant: 'default' as const };
    }
  };

  const displayInterviews = filteredInterviews ?? interviews;
  const completedInterviews = displayInterviews.filter(i => i.status.toLowerCase() === 'completed');
  const inProgressInterviews = displayInterviews.filter(
    i => i.status.toLowerCase() === 'in_progress' || i.status.toLowerCase() === 'paused'
  );

  if (isLoading) {
    return (
      <div className="py-8">
        <div className="max-w-4xl mx-auto px-4">
          <div className="mb-8">
            <Skeleton className="h-10 w-48 mb-2" />
            <Skeleton className="h-5 w-72" />
          </div>
          <div className="grid grid-cols-3 gap-4 mb-8">
            {[...Array(3)].map((_, i) => (
              <Card key={i} className="p-4 text-center">
                <Skeleton className="h-9 w-12 mx-auto mb-1" />
                <Skeleton className="h-4 w-20 mx-auto" />
              </Card>
            ))}
          </div>
          <div className="space-y-3">
            {[...Array(5)].map((_, i) => (
              <Card key={i} className="p-4">
                <div className="flex items-center gap-4">
                  <Skeleton className="w-12 h-12 rounded-full" />
                  <div className="flex-1 space-y-2">
                    <Skeleton className="h-4 w-1/3" />
                    <Skeleton className="h-3 w-1/4" />
                  </div>
                </div>
              </Card>
            ))}
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

        {/* Search Bar */}
        <motion.div
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ delay: 0.05 }}
          className="mb-6"
        >
          <div className="relative">
            <Search className="absolute left-3 top-1/2 -translate-y-1/2 w-5 h-5 text-neutral-400" />
            <input
              type="text"
              value={searchKeyword}
              onChange={(e) => handleSearchInputChange(e.target.value)}
              onKeyDown={handleSearchKeyDown}
              placeholder="면접 기록 검색 (키워드 입력 후 Enter)"
              aria-label="면접 기록 검색"
              className="input-brutal w-full pl-10 pr-10"
            />
            {searchKeyword && (
              <button
                onClick={clearSearch}
                className="absolute right-3 top-1/2 -translate-y-1/2 text-neutral-400 hover:text-neutral-600"
              >
                <X className="w-5 h-5" />
              </button>
            )}
          </div>
          {isSearching && (
            <div className="flex items-center gap-2 mt-2 text-sm text-neutral-500">
              <Loader2 className="w-4 h-4 animate-spin" />
              검색 중...
            </div>
          )}
          {filteredInterviews !== null && !isSearching && (
            <p className="mt-2 text-sm text-neutral-500">
              검색 결과: {filteredInterviews.length}건
            </p>
          )}
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

        {/* In Progress & Paused Interviews */}
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
              {inProgressInterviews.map((interview) => {
                const isPaused = interview.status.toLowerCase() === 'paused';
                return (
                  <Link
                    key={interview.id}
                    href={`/interview?resume=${interview.id}`}
                  >
                    <Card className={`p-4 transition-colors cursor-pointer ${isPaused ? 'hover:border-purple-500' : 'hover:border-blue-500'}`}>
                      <div className="flex items-center justify-between">
                        <div className="flex items-center gap-4">
                          <div className={`w-12 h-12 border-2 flex items-center justify-center ${isPaused ? 'bg-purple-100 border-purple-500' : 'bg-blue-100 border-blue-500'}`}>
                            <MessageSquare className={`w-6 h-6 ${isPaused ? 'text-purple-600' : 'text-blue-600'}`} />
                          </div>
                          <div>
                            <div className="flex items-center gap-2 mb-1">
                              {isPaused ? (
                                <Tag variant="purple" size="sm">일시정지</Tag>
                              ) : (
                                <Tag variant="blue" size="sm">진행중</Tag>
                              )}
                              <span className="text-sm text-neutral-500">
                                {interview.totalQuestions}개 질문
                              </span>
                            </div>
                            <p className="text-sm text-neutral-600">
                              {formatDate(interview.startedAt)} {formatTime(interview.startedAt)}
                            </p>
                          </div>
                        </div>
                        <div className="flex items-center gap-2">
                          {isPaused && (
                            <span className="text-sm text-purple-600 font-medium">재개하기</span>
                          )}
                          <ChevronRight className="w-5 h-5 text-neutral-400" />
                        </div>
                      </div>
                    </Card>
                  </Link>
                );
              })}
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
              <h3 className="text-lg font-semibold mb-2">
                {filteredInterviews !== null ? '검색 결과가 없습니다' : '아직 완료된 면접이 없습니다'}
              </h3>
              <p className="text-neutral-500 mb-6">
                {filteredInterviews !== null ? '다른 키워드로 검색해보세요' : '첫 모의 면접을 시작해보세요!'}
              </p>
              {filteredInterviews !== null ? (
                <Button onClick={clearSearch}>검색 초기화</Button>
              ) : (
                <Link href="/interview">
                  <Button>면접 시작하기</Button>
                </Link>
              )}
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
