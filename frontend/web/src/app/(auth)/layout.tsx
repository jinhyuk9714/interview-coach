'use client';

import { useEffect } from 'react';
import { useRouter } from 'next/navigation';
import Link from 'next/link';
import { useAuthStore } from '@/stores/auth';

export default function AuthLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  const router = useRouter();
  const { isAuthenticated, _hasHydrated } = useAuthStore();

  useEffect(() => {
    // hydration 완료 후 이미 로그인된 경우 대시보드로 이동
    if (_hasHydrated && isAuthenticated) {
      router.push('/dashboard');
    }
  }, [isAuthenticated, _hasHydrated, router]);

  // hydration 대기 중 로딩 표시
  if (!_hasHydrated) {
    return (
      <div className="min-h-screen flex items-center justify-center">
        <div className="w-8 h-8 border-4 border-ink border-t-transparent rounded-full animate-spin" />
      </div>
    );
  }

  // 이미 로그인된 경우 리다이렉트 중 로딩 표시
  if (isAuthenticated) {
    return (
      <div className="min-h-screen flex items-center justify-center">
        <div className="w-8 h-8 border-4 border-ink border-t-transparent rounded-full animate-spin" />
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-paper flex flex-col">
      {/* Simple Header */}
      <header className="p-6">
        <Link href="/" className="flex items-center gap-3 group w-fit">
          <div className="w-10 h-10 bg-ink flex items-center justify-center">
            <span className="text-accent-lime font-display text-xl italic">i</span>
          </div>
          <span className="font-display text-xl">
            Interview<span className="text-accent-coral italic">Coach</span>
          </span>
        </Link>
      </header>

      {/* Content */}
      <main className="flex-1 flex items-center justify-center px-4 py-12">
        {children}
      </main>

      {/* Simple Footer */}
      <footer className="p-6 text-center">
        <p className="text-xs text-neutral-500 font-mono">
          © 2024 Interview Coach. All rights reserved.
        </p>
      </footer>
    </div>
  );
}
