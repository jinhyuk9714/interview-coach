'use client';

import { useState } from 'react';
import Link from 'next/link';
import { motion, AnimatePresence } from 'motion/react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { Button, Card, Input, Textarea, Tag } from '@/components/ui';
import { jdApi, questionApi, statisticsApi } from '@/lib/api';
import {
  Plus,
  FileText,
  Sparkles,
  Building2,
  Briefcase,
  Link as LinkIcon,
  ChevronRight,
  Loader2,
  Check,
  ArrowRight,
  X,
  AlertCircle,
  Trash2,
  Settings2,
  AlertTriangle
} from 'lucide-react';
import type { JobDescription, JdAnalysis, GeneratedQuestion } from '@/types';
import { toast } from 'sonner';

export default function JdPage() {
  const queryClient = useQueryClient();

  const { data: jdList = [], isLoading, error: fetchError } = useQuery<JobDescription[]>({
    queryKey: ['jds'],
    queryFn: () => jdApi.list().then(res => res.data || []),
  });

  const [error, setError] = useState<string | null>(fetchError ? 'JD 목록을 불러오는데 실패했습니다.' : null);
  const [showCreateModal, setShowCreateModal] = useState(false);
  const [selectedJd, setSelectedJd] = useState<JobDescription | null>(null);
  const [analysis, setAnalysis] = useState<JdAnalysis | null>(null);
  const [questions, setQuestions] = useState<GeneratedQuestion[]>([]);
  const [isAnalyzing, setIsAnalyzing] = useState(false);
  const [isGenerating, setIsGenerating] = useState(false);

  const [newJd, setNewJd] = useState({
    companyName: '',
    position: '',
    originalText: '',
    originalUrl: '',
  });
  const [createError, setCreateError] = useState<string | null>(null);
  const [deleteTarget, setDeleteTarget] = useState<JobDescription | null>(null);

  // Generate modal states
  const [showGenerateModal, setShowGenerateModal] = useState(false);
  const [generateSettings, setGenerateSettings] = useState({
    count: 5,
    difficulty: 3,
    questionType: 'mixed' as 'mixed' | 'technical' | 'behavioral',
    prioritizeWeakAreas: false,
  });
  const [weakPoints, setWeakPoints] = useState<Array<{ category: string; score: number }>>([]);
  const [isLoadingStats, setIsLoadingStats] = useState(false);

  const createMutation = useMutation({
    mutationFn: (data: { companyName: string; position: string; originalText: string; originalUrl: string }) =>
      jdApi.create(data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['jds'] });
      setShowCreateModal(false);
      setNewJd({ companyName: '', position: '', originalText: '', originalUrl: '' });
      setCreateError(null);
      toast.success('JD가 등록되었습니다.');
    },
    onError: () => {
      setCreateError('JD 등록에 실패했습니다. 다시 시도해주세요.');
      toast.error('JD 등록에 실패했습니다.');
    },
  });

  const handleCreateJd = async (e: React.FormEvent) => {
    e.preventDefault();
    setCreateError(null);
    createMutation.mutate(newJd);
  };

  const handleAnalyze = async (jd: JobDescription) => {
    setSelectedJd(jd);
    setIsAnalyzing(true);
    setAnalysis(null);
    setQuestions([]);

    try {
      const response = await jdApi.analyze(jd.id);
      setAnalysis(response.data);
      toast.success('JD 분석이 완료되었습니다.');
    } catch {
      // Use existing parsed data if available
      if (jd.parsedSkills?.length > 0 || jd.parsedRequirements?.length > 0) {
        setAnalysis({
          jdId: jd.id,
          skills: jd.parsedSkills || [],
          requirements: jd.parsedRequirements || [],
          summary: `${jd.companyName}의 ${jd.position} 포지션입니다.`,
        });
      } else {
        setError('JD 분석에 실패했습니다. 다시 시도해주세요.');
      }
    } finally {
      setIsAnalyzing(false);
    }
  };

  const handleOpenGenerateModal = async () => {
    setShowGenerateModal(true);
    setIsLoadingStats(true);
    setWeakPoints([]);

    try {
      const response = await statisticsApi.get();
      const stats = response.data;

      // 70% 미만인 카테고리 추출 (categoryStats 배열에서)
      if (stats?.categoryStats && Array.isArray(stats.categoryStats)) {
        const weak = stats.categoryStats
          .filter((cat: { category: string; score: number }) => cat.score < 70)
          .map((cat: { category: string; score: number }) => ({ category: cat.category, score: cat.score }))
          .sort((a: { score: number }, b: { score: number }) => a.score - b.score);
        setWeakPoints(weak);
      }
    } catch {
      // Statistics load failure is non-critical
      // 통계 로드 실패해도 모달은 표시
    } finally {
      setIsLoadingStats(false);
    }
  };

  const handleConfirmGenerate = async () => {
    if (!selectedJd) return;

    setShowGenerateModal(false);
    setIsGenerating(true);
    setError(null);

    try {
      const requestData: {
        jdId: number;
        questionType: string;
        count: number;
        difficulty: number;
        weakCategories?: Array<{ category: string; score: number }>;
      } = {
        jdId: selectedJd.id,
        questionType: generateSettings.questionType,
        count: generateSettings.count,
        difficulty: generateSettings.difficulty,
      };

      // 취약 분야 우선 반영이 체크되어 있고 취약 분야가 있으면 추가
      if (generateSettings.prioritizeWeakAreas && weakPoints.length > 0) {
        requestData.weakCategories = weakPoints;
      }

      const response = await questionApi.generate(requestData);
      setQuestions(response.data.questions || response.data);
      toast.success('질문이 생성되었습니다!');
    } catch {
      setError('질문 생성에 실패했습니다. 다시 시도해주세요.');
      toast.error('질문 생성에 실패했습니다.');
    } finally {
      setIsGenerating(false);
    }
  };

  const deleteMutation = useMutation({
    mutationFn: (id: number) => jdApi.delete(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['jds'] });
      if (selectedJd?.id === deleteTarget?.id) {
        setSelectedJd(null);
        setAnalysis(null);
        setQuestions([]);
      }
      setDeleteTarget(null);
      toast.success('JD가 삭제되었습니다.');
    },
    onError: () => {
      setError('JD 삭제에 실패했습니다. 다시 시도해주세요.');
      toast.error('JD 삭제에 실패했습니다.');
    },
  });

  const handleDeleteJd = async () => {
    if (!deleteTarget) return;
    deleteMutation.mutate(deleteTarget.id);
  };

  return (
    <div className="py-8">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        {/* Header */}
        <motion.div
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          className="flex flex-col md:flex-row md:items-end justify-between gap-4 mb-8"
        >
          <div>
            <h1 className="text-display-md font-display mb-2">JD 분석</h1>
            <p className="text-neutral-600">
              채용공고를 등록하고 AI가 분석한 예상 질문을 받아보세요
            </p>
          </div>
          <Button
            onClick={() => setShowCreateModal(true)}
            leftIcon={<Plus className="w-4 h-4" />}
          >
            새 JD 등록
          </Button>
        </motion.div>

        {/* Error Alert */}
        {error && (
          <motion.div
            initial={{ opacity: 0, y: -10 }}
            animate={{ opacity: 1, y: 0 }}
            className="mb-6 p-4 bg-accent-coral/10 border-2 border-accent-coral flex items-center gap-3"
          >
            <AlertCircle className="w-5 h-5 text-accent-coral shrink-0" />
            <p className="text-sm text-accent-coral">{error}</p>
            <button
              onClick={() => setError(null)}
              className="ml-auto p-1 hover:bg-accent-coral/20 rounded"
            >
              <X className="w-4 h-4 text-accent-coral" />
            </button>
          </motion.div>
        )}

        <div className="grid lg:grid-cols-3 gap-8">
          {/* JD List */}
          <motion.div
            initial={{ opacity: 0, x: -20 }}
            animate={{ opacity: 1, x: 0 }}
            transition={{ delay: 0.1 }}
            className="lg:col-span-1"
          >
            <Card className="p-0 overflow-hidden">
              <div className="p-4 bg-ink text-paper">
                <h2 className="font-sans font-semibold flex items-center gap-2">
                  <FileText className="w-4 h-4" />
                  등록된 JD ({jdList.length})
                </h2>
              </div>
              <div className="divide-y-2 divide-neutral-100 max-h-[600px] overflow-y-auto">
                {isLoading ? (
                  <div className="p-8 text-center">
                    <Loader2 className="w-6 h-6 animate-spin mx-auto text-neutral-400" />
                    <p className="text-sm text-neutral-500 mt-2">불러오는 중...</p>
                  </div>
                ) : jdList.length === 0 ? (
                  <div className="p-8 text-center">
                    <FileText className="w-8 h-8 mx-auto text-neutral-300 mb-2" />
                    <p className="text-sm text-neutral-500">등록된 JD가 없습니다</p>
                    <Button
                      size="sm"
                      variant="ghost"
                      className="mt-2"
                      onClick={() => setShowCreateModal(true)}
                    >
                      새 JD 등록하기
                    </Button>
                  </div>
                ) : (
                  jdList.map((jd) => (
                    <div
                      key={jd.id}
                      className={`w-full p-4 text-left transition-colors ${
                        selectedJd?.id === jd.id
                          ? 'bg-accent-lime/20'
                          : 'hover:bg-neutral-50'
                      }`}
                    >
                      <div className="flex items-start justify-between">
                        <motion.button
                          onClick={() => handleAnalyze(jd)}
                          className="flex-1 text-left"
                          whileHover={{ x: 4 }}
                        >
                          <h3 className="font-sans font-semibold">{jd.companyName}</h3>
                          <p className="text-sm text-neutral-500">{jd.position}</p>
                          {jd.parsedSkills && jd.parsedSkills.length > 0 && (
                            <div className="flex flex-wrap gap-1 mt-2">
                              {jd.parsedSkills.slice(0, 3).map((skill) => (
                                <Tag key={skill} size="sm">{skill}</Tag>
                              ))}
                              {jd.parsedSkills.length > 3 && (
                                <Tag size="sm" variant="default">+{jd.parsedSkills.length - 3}</Tag>
                              )}
                            </div>
                          )}
                        </motion.button>
                        <div className="flex items-center gap-2">
                          <button
                            onClick={(e) => {
                              e.stopPropagation();
                              setDeleteTarget(jd);
                            }}
                            className="p-1.5 text-neutral-400 hover:text-accent-coral hover:bg-accent-coral/10 rounded transition-colors"
                            title="삭제"
                          >
                            <Trash2 className="w-4 h-4" />
                          </button>
                          <ChevronRight className="w-5 h-5 text-neutral-400" />
                        </div>
                      </div>
                    </div>
                  ))
                )}
              </div>
            </Card>
          </motion.div>

          {/* Analysis & Questions */}
          <motion.div
            initial={{ opacity: 0, x: 20 }}
            animate={{ opacity: 1, x: 0 }}
            transition={{ delay: 0.2 }}
            className="lg:col-span-2 space-y-6"
          >
            {!selectedJd ? (
              <Card className="p-12 text-center">
                <div className="w-20 h-20 bg-neutral-100 border-2 border-dashed border-neutral-300 flex items-center justify-center mx-auto mb-6">
                  <FileText className="w-10 h-10 text-neutral-400" />
                </div>
                <h3 className="text-xl font-display mb-2">JD를 선택하세요</h3>
                <p className="text-neutral-500">
                  왼쪽 목록에서 분석할 JD를 선택하거나 새 JD를 등록하세요
                </p>
              </Card>
            ) : (
              <>
                {/* Analysis Result */}
                <Card className="p-6">
                  <div className="flex items-center justify-between mb-6">
                    <div className="flex items-center gap-3">
                      <div className="w-12 h-12 bg-accent-lime border-2 border-ink flex items-center justify-center font-display text-xl">
                        {selectedJd.companyName.charAt(0)}
                      </div>
                      <div>
                        <h2 className="text-xl font-display">{selectedJd.companyName}</h2>
                        <p className="text-neutral-500">{selectedJd.position}</p>
                      </div>
                    </div>
                    {isAnalyzing && (
                      <div className="flex items-center gap-2 text-accent-coral">
                        <Loader2 className="w-4 h-4 animate-spin" />
                        <span className="text-sm font-mono">분석 중...</span>
                      </div>
                    )}
                  </div>

                  {analysis && (
                    <motion.div
                      initial={{ opacity: 0, y: 10 }}
                      animate={{ opacity: 1, y: 0 }}
                      className="space-y-6"
                    >
                      {/* Summary */}
                      <div className="p-4 bg-neutral-50 border-l-4 border-accent-blue">
                        <p className="text-sm leading-relaxed">{analysis.summary}</p>
                      </div>

                      {/* Skills */}
                      <div>
                        <h4 className="font-sans font-semibold text-sm uppercase tracking-wider mb-3 flex items-center gap-2">
                          <Sparkles className="w-4 h-4 text-accent-coral" />
                          핵심 기술 스택
                        </h4>
                        <div className="flex flex-wrap gap-2">
                          {analysis.skills.map((skill) => (
                            <Tag key={skill} variant="lime">{skill}</Tag>
                          ))}
                        </div>
                      </div>

                      {/* Requirements */}
                      <div>
                        <h4 className="font-sans font-semibold text-sm uppercase tracking-wider mb-3">
                          요구 사항
                        </h4>
                        <ul className="space-y-2">
                          {analysis.requirements.map((req, i) => (
                            <li key={i} className="flex items-start gap-2 text-sm">
                              <Check className="w-4 h-4 text-accent-blue mt-0.5 shrink-0" />
                              {req}
                            </li>
                          ))}
                        </ul>
                      </div>

                      <Button
                        onClick={handleOpenGenerateModal}
                        isLoading={isGenerating}
                        className="w-full"
                        rightIcon={<Settings2 className="w-4 h-4" />}
                      >
                        예상 질문 생성하기
                      </Button>
                    </motion.div>
                  )}
                </Card>

                {/* Generated Questions */}
                <AnimatePresence>
                  {questions.length > 0 && (
                    <motion.div
                      initial={{ opacity: 0, y: 20 }}
                      animate={{ opacity: 1, y: 0 }}
                      exit={{ opacity: 0, y: -20 }}
                    >
                      <Card className="p-6">
                        <div className="flex items-center justify-between mb-6">
                          <h3 className="text-xl font-display">
                            예상 질문 <span className="text-accent-coral">({questions.length})</span>
                          </h3>
                          <Link href={`/interview?jdId=${selectedJd.id}`}>
                            <Button size="sm" variant="accent">
                              이 질문으로 면접 시작
                            </Button>
                          </Link>
                        </div>

                        <div className="space-y-4">
                          {questions.map((q, index) => (
                            <motion.div
                              key={q.id}
                              initial={{ opacity: 0, x: -20 }}
                              animate={{ opacity: 1, x: 0 }}
                              transition={{ delay: index * 0.1 }}
                              className="p-4 bg-cream border-2 border-transparent hover:border-ink transition-colors"
                            >
                              <div className="flex items-start gap-4">
                                <span className="font-mono text-2xl font-bold text-neutral-300">
                                  {String(index + 1).padStart(2, '0')}
                                </span>
                                <div className="flex-1">
                                  <div className="flex items-center gap-2 mb-2">
                                    <Tag
                                      variant={q.questionType === 'technical' ? 'blue' : 'coral'}
                                      size="sm"
                                    >
                                      {q.questionType === 'technical' ? '기술' : '인성'}
                                    </Tag>
                                    <Tag size="sm">{q.skillCategory}</Tag>
                                    <span className="text-xs font-mono text-neutral-400">
                                      난이도 {q.difficulty}/5
                                    </span>
                                  </div>
                                  <p className="font-display text-lg">{q.questionText}</p>
                                  {q.hint && (
                                    <p className="text-sm text-neutral-500 mt-2">
                                      <span className="font-semibold">힌트:</span> {q.hint}
                                    </p>
                                  )}
                                </div>
                              </div>
                            </motion.div>
                          ))}
                        </div>
                      </Card>
                    </motion.div>
                  )}
                </AnimatePresence>
              </>
            )}
          </motion.div>
        </div>
      </div>

      {/* Delete Confirmation Modal */}
      <AnimatePresence>
        {deleteTarget && (
          <motion.div
            initial={{ opacity: 0 }}
            animate={{ opacity: 1 }}
            exit={{ opacity: 0 }}
            className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-ink/50"
            onClick={() => setDeleteTarget(null)}
          >
            <motion.div
              initial={{ opacity: 0, scale: 0.95, y: 20 }}
              animate={{ opacity: 1, scale: 1, y: 0 }}
              exit={{ opacity: 0, scale: 0.95, y: 20 }}
              onClick={(e) => e.stopPropagation()}
              className="w-full max-w-md"
            >
              <Card className="p-6">
                <div className="text-center mb-6">
                  <div className="w-16 h-16 bg-accent-coral/10 border-2 border-accent-coral flex items-center justify-center mx-auto mb-4">
                    <Trash2 className="w-8 h-8 text-accent-coral" />
                  </div>
                  <h3 className="text-xl font-display mb-2">JD를 삭제하시겠습니까?</h3>
                  <p className="text-neutral-500 text-sm">
                    <span className="font-semibold">{deleteTarget.companyName}</span>의{' '}
                    <span className="font-semibold">{deleteTarget.position}</span> 포지션을
                    삭제합니다. 이 작업은 되돌릴 수 없습니다.
                  </p>
                </div>
                <div className="flex gap-3 justify-center">
                  <Button
                    variant="secondary"
                    onClick={() => setDeleteTarget(null)}
                    disabled={deleteMutation.isPending}
                  >
                    취소
                  </Button>
                  <Button
                    onClick={handleDeleteJd}
                    isLoading={deleteMutation.isPending}
                    className="bg-accent-coral hover:bg-accent-coral/90"
                  >
                    삭제하기
                  </Button>
                </div>
              </Card>
            </motion.div>
          </motion.div>
        )}
      </AnimatePresence>

      {/* Generate Settings Modal */}
      <AnimatePresence>
        {showGenerateModal && (
          <motion.div
            initial={{ opacity: 0 }}
            animate={{ opacity: 1 }}
            exit={{ opacity: 0 }}
            className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-ink/50"
            onClick={() => setShowGenerateModal(false)}
          >
            <motion.div
              initial={{ opacity: 0, scale: 0.95, y: 20 }}
              animate={{ opacity: 1, scale: 1, y: 0 }}
              exit={{ opacity: 0, scale: 0.95, y: 20 }}
              onClick={(e) => e.stopPropagation()}
              className="w-full max-w-md"
            >
              <Card className="p-0 overflow-hidden">
                <div className="p-4 bg-ink text-paper flex items-center justify-between">
                  <h2 className="font-sans font-semibold flex items-center gap-2">
                    <Settings2 className="w-4 h-4" />
                    질문 생성 설정
                  </h2>
                  <button
                    onClick={() => setShowGenerateModal(false)}
                    className="p-1 hover:bg-white/10 rounded"
                  >
                    <X className="w-5 h-5" />
                  </button>
                </div>

                <div className="p-6 space-y-5">
                  {/* 질문 개수 */}
                  <div>
                    <label className="block text-sm font-semibold mb-2">질문 개수</label>
                    <div className="flex gap-2">
                      {[3, 5, 7, 10].map((num) => (
                        <button
                          key={num}
                          onClick={() => setGenerateSettings({ ...generateSettings, count: num })}
                          className={`flex-1 py-2 px-3 border-2 transition-colors ${
                            generateSettings.count === num
                              ? 'border-ink bg-accent-lime'
                              : 'border-neutral-200 hover:border-neutral-400'
                          }`}
                        >
                          {num}개
                        </button>
                      ))}
                    </div>
                  </div>

                  {/* 난이도 */}
                  <div>
                    <label className="block text-sm font-semibold mb-2">난이도</label>
                    <div className="flex gap-2">
                      {[1, 2, 3, 4, 5].map((level) => (
                        <button
                          key={level}
                          onClick={() => setGenerateSettings({ ...generateSettings, difficulty: level })}
                          className={`flex-1 py-2 px-3 border-2 transition-colors ${
                            generateSettings.difficulty === level
                              ? 'border-ink bg-accent-blue text-white'
                              : 'border-neutral-200 hover:border-neutral-400'
                          }`}
                        >
                          {level}
                        </button>
                      ))}
                    </div>
                    <p className="text-xs text-neutral-500 mt-1">
                      {generateSettings.difficulty === 1 && '매우 쉬움 (신입 레벨)'}
                      {generateSettings.difficulty === 2 && '쉬움 (1-2년차)'}
                      {generateSettings.difficulty === 3 && '보통 (3-5년차)'}
                      {generateSettings.difficulty === 4 && '어려움 (5-7년차)'}
                      {generateSettings.difficulty === 5 && '매우 어려움 (시니어/리드)'}
                    </p>
                  </div>

                  {/* 질문 유형 */}
                  <div>
                    <label className="block text-sm font-semibold mb-2">질문 유형</label>
                    <div className="flex gap-2">
                      {[
                        { value: 'mixed', label: '혼합' },
                        { value: 'technical', label: '기술' },
                        { value: 'behavioral', label: '인성' },
                      ].map((type) => (
                        <button
                          key={type.value}
                          onClick={() => setGenerateSettings({ ...generateSettings, questionType: type.value as 'mixed' | 'technical' | 'behavioral' })}
                          className={`flex-1 py-2 px-3 border-2 transition-colors ${
                            generateSettings.questionType === type.value
                              ? 'border-ink bg-accent-coral text-white'
                              : 'border-neutral-200 hover:border-neutral-400'
                          }`}
                        >
                          {type.label}
                        </button>
                      ))}
                    </div>
                  </div>

                  {/* 취약 분야 우선 반영 */}
                  <div className="border-t-2 border-neutral-100 pt-5">
                    <label className="flex items-start gap-3 cursor-pointer">
                      <input
                        type="checkbox"
                        checked={generateSettings.prioritizeWeakAreas}
                        onChange={(e) => setGenerateSettings({ ...generateSettings, prioritizeWeakAreas: e.target.checked })}
                        className="mt-1 w-5 h-5 border-2 border-ink accent-accent-coral"
                      />
                      <div className="flex-1">
                        <span className="font-semibold flex items-center gap-2">
                          <AlertTriangle className="w-4 h-4 text-accent-coral" />
                          취약 분야 우선 반영
                        </span>
                        <p className="text-sm text-neutral-500 mt-1">
                          70% 미만 카테고리에서 더 많은 질문을 생성합니다
                        </p>
                      </div>
                    </label>

                    {/* 취약 분야 표시 */}
                    {generateSettings.prioritizeWeakAreas && (
                      <div className="mt-3 p-3 bg-neutral-50 border-l-4 border-accent-coral">
                        {isLoadingStats ? (
                          <div className="flex items-center gap-2 text-neutral-500">
                            <Loader2 className="w-4 h-4 animate-spin" />
                            <span className="text-sm">통계 불러오는 중...</span>
                          </div>
                        ) : weakPoints.length > 0 ? (
                          <>
                            <p className="text-sm font-semibold mb-2">취약 분야:</p>
                            <div className="flex flex-wrap gap-2">
                              {weakPoints.map((wp) => (
                                <Tag key={wp.category} variant="coral" size="sm">
                                  {wp.category} {wp.score}%
                                </Tag>
                              ))}
                            </div>
                          </>
                        ) : (
                          <p className="text-sm text-neutral-500">
                            아직 면접 기록이 없어 취약 분야를 분석할 수 없습니다.
                            면접을 진행하면 자동으로 분석됩니다.
                          </p>
                        )}
                      </div>
                    )}
                  </div>

                  {/* 버튼 */}
                  <div className="flex gap-3 justify-end pt-2">
                    <Button
                      variant="secondary"
                      onClick={() => setShowGenerateModal(false)}
                    >
                      취소
                    </Button>
                    <Button
                      onClick={handleConfirmGenerate}
                      rightIcon={<ArrowRight className="w-4 h-4" />}
                    >
                      질문 생성하기
                    </Button>
                  </div>
                </div>
              </Card>
            </motion.div>
          </motion.div>
        )}
      </AnimatePresence>

      {/* Create Modal */}
      <AnimatePresence>
        {showCreateModal && (
          <motion.div
            initial={{ opacity: 0 }}
            animate={{ opacity: 1 }}
            exit={{ opacity: 0 }}
            className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-ink/50"
            onClick={() => setShowCreateModal(false)}
          >
            <motion.div
              initial={{ opacity: 0, scale: 0.95, y: 20 }}
              animate={{ opacity: 1, scale: 1, y: 0 }}
              exit={{ opacity: 0, scale: 0.95, y: 20 }}
              onClick={(e) => e.stopPropagation()}
              className="w-full max-w-xl"
            >
              <Card className="p-0 overflow-hidden">
                <div className="p-4 bg-ink text-paper flex items-center justify-between">
                  <h2 className="font-sans font-semibold flex items-center gap-2">
                    <Plus className="w-4 h-4" />
                    새 JD 등록
                  </h2>
                  <button
                    onClick={() => setShowCreateModal(false)}
                    className="p-1 hover:bg-white/10 rounded"
                  >
                    <X className="w-5 h-5" />
                  </button>
                </div>

                <form onSubmit={handleCreateJd} className="p-6 space-y-5">
                  <div className="grid grid-cols-2 gap-4">
                    <div className="relative">
                      <Building2 className="absolute left-4 top-1/2 -translate-y-1/2 w-5 h-5 text-neutral-400 z-10" />
                      <Input
                        placeholder="회사명"
                        value={newJd.companyName}
                        onChange={(e) => setNewJd({ ...newJd, companyName: e.target.value })}
                        className="pl-12"
                        required
                      />
                    </div>
                    <div className="relative">
                      <Briefcase className="absolute left-4 top-1/2 -translate-y-1/2 w-5 h-5 text-neutral-400 z-10" />
                      <Input
                        placeholder="포지션"
                        value={newJd.position}
                        onChange={(e) => setNewJd({ ...newJd, position: e.target.value })}
                        className="pl-12"
                        required
                      />
                    </div>
                  </div>

                  <div className="relative">
                    <LinkIcon className="absolute left-4 top-4 w-5 h-5 text-neutral-400 z-10" />
                    <Input
                      placeholder="채용공고 URL (선택)"
                      value={newJd.originalUrl}
                      onChange={(e) => setNewJd({ ...newJd, originalUrl: e.target.value })}
                      className="pl-12"
                    />
                  </div>

                  <Textarea
                    placeholder="채용공고 내용을 붙여넣어 주세요..."
                    value={newJd.originalText}
                    onChange={(e) => setNewJd({ ...newJd, originalText: e.target.value })}
                    className="min-h-[200px]"
                    required
                  />

                  {createError && (
                    <div className="p-3 bg-accent-coral/10 border border-accent-coral text-sm text-accent-coral">
                      {createError}
                    </div>
                  )}

                  <div className="flex gap-3 justify-end">
                    <Button
                      type="button"
                      variant="secondary"
                      onClick={() => setShowCreateModal(false)}
                      disabled={createMutation.isPending}
                    >
                      취소
                    </Button>
                    <Button type="submit" isLoading={createMutation.isPending}>
                      등록하기
                    </Button>
                  </div>
                </form>
              </Card>
            </motion.div>
          </motion.div>
        )}
      </AnimatePresence>
    </div>
  );
}
