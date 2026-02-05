'use client';

import { useState } from 'react';
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
  X
} from 'lucide-react';
import type { JobDescription, JdAnalysis, GeneratedQuestion } from '@/types';

// Mock data for demo
const mockJdList: JobDescription[] = [
  {
    id: 1,
    userId: 1,
    companyName: '카카오',
    position: '백엔드 개발자',
    originalText: 'Java, Spring Boot 기반 백엔드 개발 경력 3년 이상...',
    parsedSkills: ['Java', 'Spring Boot', 'JPA', 'MySQL', 'Redis'],
    parsedRequirements: ['3년 이상 경력', 'MSA 경험', 'AWS 경험'],
    createdAt: '2024-01-15T10:00:00Z',
  },
  {
    id: 2,
    userId: 1,
    companyName: '네이버',
    position: 'Java 개발자',
    originalText: '대규모 트래픽 처리 경험...',
    parsedSkills: ['Java', 'Spring', 'Kafka', 'Kubernetes'],
    parsedRequirements: ['5년 이상 경력', '대용량 트래픽 경험'],
    createdAt: '2024-01-14T09:00:00Z',
  },
];

export default function JdPage() {
  const [jdList, setJdList] = useState<JobDescription[]>(mockJdList);
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

  const handleCreateJd = async (e: React.FormEvent) => {
    e.preventDefault();

    try {
      const response = await jdApi.create(newJd);
      setJdList([response.data, ...jdList]);
      setShowCreateModal(false);
      setNewJd({ companyName: '', position: '', originalText: '', originalUrl: '' });
    } catch {
      // Mock: add to local state
      const mockJd: JobDescription = {
        id: Date.now(),
        userId: 1,
        ...newJd,
        parsedSkills: [],
        parsedRequirements: [],
        createdAt: new Date().toISOString(),
      };
      setJdList([mockJd, ...jdList]);
      setShowCreateModal(false);
      setNewJd({ companyName: '', position: '', originalText: '', originalUrl: '' });
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
    } catch {
      // Mock analysis
      setAnalysis({
        jdId: jd.id,
        skills: jd.parsedSkills.length > 0 ? jd.parsedSkills : ['Java', 'Spring Boot', 'JPA', 'MySQL'],
        requirements: jd.parsedRequirements.length > 0 ? jd.parsedRequirements : ['3년 이상 경력', '협업 능력'],
        summary: `${jd.companyName}의 ${jd.position} 포지션은 백엔드 개발 역량과 시스템 설계 능력을 요구합니다.`,
      });
    } finally {
      setIsAnalyzing(false);
    }
  };

  const handleGenerateQuestions = async () => {
    if (!selectedJd) return;

    setIsGenerating(true);

    try {
      const response = await questionApi.generate({
        jdId: selectedJd.id,
        questionType: 'mixed',
        count: 5,
        difficulty: 3,
      });
      setQuestions(response.data.questions || response.data);
    } catch {
      // Mock questions
      setQuestions([
        {
          id: 1,
          jdId: selectedJd.id,
          questionType: 'technical',
          skillCategory: 'Java',
          questionText: 'Java의 가비지 컬렉션(GC) 동작 원리와 G1 GC의 특징을 설명해주세요.',
          hint: 'Young Generation, Old Generation, Mark-Sweep-Compact',
          difficulty: 3,
          createdAt: new Date().toISOString(),
        },
        {
          id: 2,
          jdId: selectedJd.id,
          questionType: 'technical',
          skillCategory: 'Spring',
          questionText: 'Spring의 @Transactional 어노테이션의 propagation 옵션들에 대해 설명해주세요.',
          hint: 'REQUIRED, REQUIRES_NEW, NESTED 등',
          difficulty: 4,
          createdAt: new Date().toISOString(),
        },
        {
          id: 3,
          jdId: selectedJd.id,
          questionType: 'technical',
          skillCategory: 'JPA',
          questionText: 'JPA N+1 문제가 무엇이며, 어떻게 해결할 수 있나요?',
          hint: 'Fetch Join, @BatchSize, @EntityGraph',
          difficulty: 3,
          createdAt: new Date().toISOString(),
        },
        {
          id: 4,
          jdId: selectedJd.id,
          questionType: 'behavioral',
          skillCategory: '협업',
          questionText: '팀 내에서 의견 충돌이 있었을 때 어떻게 해결하셨나요?',
          difficulty: 2,
          createdAt: new Date().toISOString(),
        },
        {
          id: 5,
          jdId: selectedJd.id,
          questionType: 'technical',
          skillCategory: 'Database',
          questionText: '인덱스의 동작 원리와 B+Tree 구조에 대해 설명해주세요.',
          hint: 'Clustered Index, Non-Clustered Index',
          difficulty: 4,
          createdAt: new Date().toISOString(),
        },
      ]);
    } finally {
      setIsGenerating(false);
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
                {jdList.map((jd) => (
                  <motion.button
                    key={jd.id}
                    onClick={() => handleAnalyze(jd)}
                    className={`w-full p-4 text-left transition-colors ${
                      selectedJd?.id === jd.id
                        ? 'bg-accent-lime/20'
                        : 'hover:bg-neutral-50'
                    }`}
                    whileHover={{ x: 4 }}
                  >
                    <div className="flex items-start justify-between">
                      <div>
                        <h3 className="font-sans font-semibold">{jd.companyName}</h3>
                        <p className="text-sm text-neutral-500">{jd.position}</p>
                        {jd.parsedSkills.length > 0 && (
                          <div className="flex flex-wrap gap-1 mt-2">
                            {jd.parsedSkills.slice(0, 3).map((skill) => (
                              <Tag key={skill} size="sm">{skill}</Tag>
                            ))}
                            {jd.parsedSkills.length > 3 && (
                              <Tag size="sm" variant="default">+{jd.parsedSkills.length - 3}</Tag>
                            )}
                          </div>
                        )}
                      </div>
                      <ChevronRight className="w-5 h-5 text-neutral-400" />
                    </div>
                  </motion.button>
                ))}
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

                  <div className="flex gap-3 justify-end">
                    <Button
                      type="button"
                      variant="secondary"
                      onClick={() => setShowCreateModal(false)}
                    >
                      취소
                    </Button>
                    <Button type="submit">
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
