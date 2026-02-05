'use client';

import Link from 'next/link';

export function Footer() {
  return (
    <footer className="bg-ink text-paper py-12 mt-auto">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <div className="grid grid-cols-1 md:grid-cols-4 gap-8">
          {/* Brand */}
          <div className="md:col-span-2">
            <div className="flex items-center gap-3 mb-4">
              <div className="w-10 h-10 bg-accent-lime flex items-center justify-center">
                <span className="text-ink font-display text-xl italic">i</span>
              </div>
              <span className="font-display text-xl">
                Interview<span className="text-accent-coral italic">Coach</span>
              </span>
            </div>
            <p className="text-neutral-400 text-sm max-w-xs">
              JD 기반 맞춤 질문 생성 + AI 모의 면접 + 실시간 피드백 시스템으로
              취업 준비를 더 스마트하게.
            </p>
          </div>

          {/* Links */}
          <div>
            <h4 className="font-sans font-semibold text-sm uppercase tracking-wider mb-4">
              제품
            </h4>
            <ul className="space-y-2 text-sm text-neutral-400">
              <li>
                <Link href="/jd" className="hover:text-accent-lime transition-colors">
                  JD 분석
                </Link>
              </li>
              <li>
                <Link href="/interview" className="hover:text-accent-lime transition-colors">
                  모의 면접
                </Link>
              </li>
              <li>
                <Link href="/statistics" className="hover:text-accent-lime transition-colors">
                  학습 통계
                </Link>
              </li>
            </ul>
          </div>

          {/* Tech */}
          <div>
            <h4 className="font-sans font-semibold text-sm uppercase tracking-wider mb-4">
              기술 스택
            </h4>
            <ul className="space-y-2 text-sm text-neutral-400">
              <li>Spring Boot</li>
              <li>LangChain4j + Claude</li>
              <li>Next.js</li>
              <li>PostgreSQL</li>
            </ul>
          </div>
        </div>

        {/* Bottom */}
        <div className="mt-12 pt-8 border-t border-neutral-800 flex flex-col sm:flex-row items-center justify-between gap-4">
          <p className="text-xs text-neutral-500 font-mono">
            © 2024 Interview Coach. MIT License.
          </p>
          <div className="flex items-center gap-6 text-xs text-neutral-500">
            <span className="font-mono">v0.0.1-SNAPSHOT</span>
            <span className="w-2 h-2 bg-accent-lime rounded-full animate-pulse" />
            <span className="font-mono">시스템 정상</span>
          </div>
        </div>
      </div>
    </footer>
  );
}
