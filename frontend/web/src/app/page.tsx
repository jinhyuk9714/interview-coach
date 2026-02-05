'use client';

import Link from 'next/link';
import { motion } from 'motion/react';
import { Button } from '@/components/ui';
import { Header, Footer } from '@/components/layout';
import {
  FileSearch,
  MessageCircle,
  TrendingUp,
  Sparkles,
  ArrowRight,
  Zap,
  Target,
  Brain
} from 'lucide-react';

const features = [
  {
    icon: FileSearch,
    title: 'JD 분석',
    description: '채용공고를 붙여넣으면 AI가 핵심 역량과 기술 스택을 자동으로 추출합니다.',
    color: 'bg-accent-lime',
  },
  {
    icon: Brain,
    title: '맞춤 질문 생성',
    description: '분석된 JD를 바탕으로 면접에서 실제 나올 법한 질문을 생성합니다.',
    color: 'bg-accent-coral',
  },
  {
    icon: MessageCircle,
    title: 'AI 모의 면접',
    description: 'AI 면접관과 실시간으로 대화하며 실전처럼 연습할 수 있습니다.',
    color: 'bg-accent-blue',
  },
  {
    icon: TrendingUp,
    title: '실시간 피드백',
    description: '답변 직후 강점, 개선점, 팁을 스트리밍으로 즉시 확인합니다.',
    color: 'bg-accent-purple',
  },
];

const stats = [
  { value: '5+', label: '마이크로서비스' },
  { value: 'SSE', label: '실시간 스트리밍' },
  { value: 'RAG', label: '벡터 검색 지원' },
  { value: 'JWT', label: '보안 인증' },
];

export default function Home() {
  return (
    <div className="min-h-screen flex flex-col">
      <Header />

      {/* Hero Section */}
      <section className="relative pt-32 pb-20 lg:pt-40 lg:pb-32 overflow-hidden">
        {/* Background decoration */}
        <div className="absolute inset-0 bg-grid-pattern opacity-50" />
        <div className="absolute top-20 right-0 w-96 h-96 bg-accent-lime/20 rounded-full blur-3xl" />
        <div className="absolute bottom-0 left-0 w-80 h-80 bg-accent-coral/10 rounded-full blur-3xl" />

        <div className="relative max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
          <div className="grid lg:grid-cols-2 gap-12 items-center">
            {/* Text Content */}
            <motion.div
              initial={{ opacity: 0, y: 30 }}
              animate={{ opacity: 1, y: 0 }}
              transition={{ duration: 0.6 }}
            >
              {/* Badge */}
              <motion.div
                initial={{ opacity: 0, scale: 0.9 }}
                animate={{ opacity: 1, scale: 1 }}
                transition={{ delay: 0.1 }}
                className="inline-flex items-center gap-2 px-4 py-2 bg-white border-2 border-ink shadow-brutal-sm mb-8"
              >
                <Sparkles className="w-4 h-4 text-accent-coral" />
                <span className="font-mono text-xs uppercase tracking-wider">
                  Powered by Claude AI
                </span>
              </motion.div>

              <h1 className="text-display-xl font-display mb-6">
                면접,{' '}
                <span className="relative inline-block">
                  <span className="relative z-10">AI</span>
                  <span className="absolute -bottom-2 left-0 right-0 h-4 bg-accent-lime -z-0" />
                </span>
                와 함께<br />
                <span className="italic text-accent-coral">준비</span>하세요
              </h1>

              <p className="text-lg text-neutral-600 mb-8 max-w-md leading-relaxed">
                채용공고만 붙여넣으면, AI가 예상 질문을 생성하고
                모의 면접을 진행해 드립니다. 실시간 피드백으로 빠르게 성장하세요.
              </p>

              <div className="flex flex-wrap gap-4">
                <Link href="/signup">
                  <Button size="lg" rightIcon={<ArrowRight className="w-5 h-5" />}>
                    무료로 시작하기
                  </Button>
                </Link>
                <Link href="/login">
                  <Button variant="secondary" size="lg">
                    로그인
                  </Button>
                </Link>
              </div>

              {/* Stats */}
              <div className="mt-12 pt-8 border-t-2 border-dashed border-neutral-300">
                <div className="grid grid-cols-4 gap-4">
                  {stats.map((stat, index) => (
                    <motion.div
                      key={stat.label}
                      initial={{ opacity: 0, y: 20 }}
                      animate={{ opacity: 1, y: 0 }}
                      transition={{ delay: 0.3 + index * 0.1 }}
                      className="text-center"
                    >
                      <div className="font-mono text-2xl font-bold text-ink">
                        {stat.value}
                      </div>
                      <div className="text-xs text-neutral-500 uppercase tracking-wider mt-1">
                        {stat.label}
                      </div>
                    </motion.div>
                  ))}
                </div>
              </div>
            </motion.div>

            {/* Hero Illustration */}
            <motion.div
              initial={{ opacity: 0, x: 30 }}
              animate={{ opacity: 1, x: 0 }}
              transition={{ duration: 0.6, delay: 0.2 }}
              className="relative hidden lg:block"
            >
              {/* Interview Preview Card */}
              <div className="relative">
                <div className="card-brutal p-6 bg-white transform rotate-1">
                  {/* Header */}
                  <div className="flex items-center justify-between mb-6">
                    <div className="flex items-center gap-3">
                      <div className="w-10 h-10 bg-accent-blue flex items-center justify-center">
                        <Brain className="w-5 h-5 text-white" />
                      </div>
                      <div>
                        <div className="font-sans text-sm font-semibold">AI 면접관</div>
                        <div className="text-xs text-neutral-500">기술 면접 모드</div>
                      </div>
                    </div>
                    <span className="tag tag-lime">진행중</span>
                  </div>

                  {/* Question */}
                  <div className="bg-neutral-100 p-4 mb-4 border-l-4 border-ink">
                    <p className="font-display text-lg italic">
                      &ldquo;마이크로서비스 아키텍처에서 서비스 간 통신 방식을
                      설명해주세요.&rdquo;
                    </p>
                  </div>

                  {/* Answer Area */}
                  <div className="bg-white border-2 border-dashed border-neutral-300 p-4 min-h-[100px]">
                    <p className="text-neutral-400 text-sm">
                      답변을 입력하세요...
                      <span className="animate-blink text-accent-coral">|</span>
                    </p>
                  </div>

                  {/* Progress */}
                  <div className="mt-4 flex items-center justify-between text-xs font-mono">
                    <span className="text-neutral-500">질문 3 / 5</span>
                    <div className="flex gap-1">
                      {[1, 2, 3, 4, 5].map((i) => (
                        <div
                          key={i}
                          className={`w-8 h-1 ${i <= 3 ? 'bg-accent-lime' : 'bg-neutral-200'}`}
                        />
                      ))}
                    </div>
                  </div>
                </div>

                {/* Floating elements */}
                <motion.div
                  animate={{ y: [0, -10, 0] }}
                  transition={{ duration: 3, repeat: Infinity, ease: 'easeInOut' }}
                  className="absolute -top-6 -left-6 bg-accent-coral text-white p-3 border-2 border-ink shadow-brutal-sm"
                >
                  <Zap className="w-6 h-6" />
                </motion.div>

                <motion.div
                  animate={{ y: [0, 10, 0] }}
                  transition={{ duration: 4, repeat: Infinity, ease: 'easeInOut' }}
                  className="absolute -bottom-4 -right-4 bg-accent-lime p-3 border-2 border-ink shadow-brutal-sm"
                >
                  <Target className="w-6 h-6" />
                </motion.div>
              </div>
            </motion.div>
          </div>
        </div>
      </section>

      {/* Marquee */}
      <div className="bg-ink py-4 overflow-hidden border-y-2 border-ink">
        <div className="marquee-container">
          <div className="marquee-content">
            {[...Array(2)].map((_, i) => (
              <div key={i} className="flex items-center gap-8 px-4">
                {['Java', 'Spring Boot', 'LangChain4j', 'Claude API', 'PostgreSQL', 'Redis', 'Next.js', 'TypeScript'].map((tech) => (
                  <span key={tech} className="font-mono text-sm text-paper/80 whitespace-nowrap flex items-center gap-2">
                    <span className="w-1.5 h-1.5 bg-accent-lime" />
                    {tech}
                  </span>
                ))}
              </div>
            ))}
          </div>
        </div>
      </div>

      {/* Features Section */}
      <section className="py-20 lg:py-32 bg-cream">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
          <motion.div
            initial={{ opacity: 0, y: 20 }}
            whileInView={{ opacity: 1, y: 0 }}
            viewport={{ once: true }}
            className="text-center mb-16"
          >
            <h2 className="text-display-md font-display mb-4">
              어떻게 <span className="italic text-accent-coral">작동</span>하나요?
            </h2>
            <p className="text-neutral-600 max-w-2xl mx-auto">
              네 단계로 면접 준비를 완성합니다. 각 단계는 AI가 도와드립니다.
            </p>
          </motion.div>

          <div className="grid md:grid-cols-2 lg:grid-cols-4 gap-6">
            {features.map((feature, index) => {
              const Icon = feature.icon;
              return (
                <motion.div
                  key={feature.title}
                  initial={{ opacity: 0, y: 30 }}
                  whileInView={{ opacity: 1, y: 0 }}
                  viewport={{ once: true }}
                  transition={{ delay: index * 0.1 }}
                  className="card-brutal p-6 bg-white group"
                >
                  <div
                    className={`w-14 h-14 ${feature.color} border-2 border-ink flex items-center justify-center mb-6 group-hover:rotate-6 transition-transform`}
                  >
                    <Icon className="w-7 h-7 text-ink" />
                  </div>
                  <div className="font-mono text-xs text-neutral-400 mb-2">
                    STEP {String(index + 1).padStart(2, '0')}
                  </div>
                  <h3 className="font-display text-xl mb-3">{feature.title}</h3>
                  <p className="text-sm text-neutral-600 leading-relaxed">
                    {feature.description}
                  </p>
                </motion.div>
              );
            })}
          </div>
        </div>
      </section>

      {/* CTA Section */}
      <section className="py-20 lg:py-32 relative overflow-hidden">
        <div className="absolute inset-0 bg-dots-pattern" />
        <div className="relative max-w-4xl mx-auto px-4 sm:px-6 lg:px-8 text-center">
          <motion.div
            initial={{ opacity: 0, scale: 0.95 }}
            whileInView={{ opacity: 1, scale: 1 }}
            viewport={{ once: true }}
          >
            <h2 className="text-display-md font-display mb-6">
              지금 바로{' '}
              <span className="relative inline-block">
                <span className="relative z-10">시작</span>
                <span className="absolute -bottom-1 left-0 right-0 h-3 bg-accent-coral/30 -z-0" />
              </span>
              하세요
            </h2>
            <p className="text-lg text-neutral-600 mb-8 max-w-2xl mx-auto">
              무료로 가입하고 첫 번째 모의 면접을 경험해보세요.
              AI가 여러분의 면접 준비를 도와드립니다.
            </p>
            <Link href="/signup">
              <Button size="lg" rightIcon={<ArrowRight className="w-5 h-5" />}>
                무료로 시작하기
              </Button>
            </Link>
          </motion.div>
        </div>
      </section>

      <Footer />
    </div>
  );
}
