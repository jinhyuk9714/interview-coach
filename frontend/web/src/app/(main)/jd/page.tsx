'use client';

import { useState, useEffect } from 'react';
import Link from 'next/link';
import { motion, AnimatePresence } from 'motion/react';
import { Button, Card, Input, Textarea, Tag } from '@/components/ui';
import { jdApi, questionApi } from '@/lib/api';
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
  Trash2
} from 'lucide-react';
import type { JobDescription, JdAnalysis, GeneratedQuestion } from '@/types';

export default function JdPage() {
  const [jdList, setJdList] = useState<JobDescription[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
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
  const [isCreating, setIsCreating] = useState(false);
  const [createError, setCreateError] = useState<string | null>(null);
  const [deleteTarget, setDeleteTarget] = useState<JobDescription | null>(null);
  const [isDeleting, setIsDeleting] = useState(false);

  // Load JD list on mount
  useEffect(() => {
    const fetchJdList = async () => {
      try {
        setIsLoading(true);
        setError(null);
        const response = await jdApi.list();
        setJdList(response.data || []);
      } catch (err) {
        console.error('Failed to fetch JD list:', err);
        setError('JD 목록을 불러오는데 실패했습니다.');
      } finally {
        setIsLoading(false);
      }
    };
    fetchJdList();
  }, []);

  const handleCreateJd = async (e: React.FormEvent) => {
    e.preventDefault();
    setIsCreating(true);
    setCreateError(null);

    try {
      const response = await jdApi.create(newJd);
      setJdList([response.data, ...jdList]);
      setShowCreateModal(false);
      setNewJd({ companyName: '', position: '', originalText: '', originalUrl: '' });
    } catch (err) {
      console.error('Failed to create JD:', err);
      setCreateError('JD 등록에 실패했습니다. 다시 시도해주세요.');
    } finally {
      setIsCreating(false);
    }
  };

  const handleAnalyze = async (jd: JobDescription) => {
    setSelectedJd(jd);
    setIsAnalyzing(true);
    setAnalysis(null);
    setQuestions([]);

    try {
      const response = await jdApi.analyze(jd.id);
      setAnalysis(response.data);
    } catch (err) {
      console.error('Failed to analyze JD:', err);
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

  const handleGenerateQuestions = async () => {
    if (!selectedJd) return;

    setIsGenerating(true);
    setError(null);

    try {
      const response = await questionApi.generate({
        jdId: selectedJd.id,
        questionType: 'mixed',
        count: 5,
        difficulty: 3,
      });
      setQuestions(response.data.questions || response.data);
    } catch (err) {
      console.error('Failed to generate questions:', err);
      setError('질문 생성에 실패했습니다. 다시 시도해주세요.');
    } finally {
      setIsGenerating(false);
    }
  };

  const handleDeleteJd = async () => {
    if (!deleteTarget) return;

    setIsDeleting(true);
    try {
      await jdApi.delete(deleteTarget.id);
      setJdList(jdList.filter(jd => jd.id !== deleteTarget.id));
      if (selectedJd?.id === deleteTarget.id) {
        setSelectedJd(null);
        setAnalysis(null);
        setQuestions([]);
      }
      setDeleteTarget(null);
    } catch (err) {
      console.error('Failed to delete JD:', err);
      setError('JD 삭제에 실패했습니다. 다시 시도해주세요.');
    } finally {
      setIsDeleting(false);
    }
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
                        onClick={handleGenerateQuestions}
                        isLoading={isGenerating}
                        className="w-full"
                        rightIcon={<ArrowRight className="w-4 h-4" />}
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
                    disabled={isDeleting}
                  >
                    취소
                  </Button>
                  <Button
                    onClick={handleDeleteJd}
                    isLoading={isDeleting}
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
                      disabled={isCreating}
                    >
                      취소
                    </Button>
                    <Button type="submit" isLoading={isCreating}>
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
