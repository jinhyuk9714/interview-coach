'use client';

import { useEffect } from 'react';
import Link from 'next/link';

export default function MainError({
  error,
  reset,
}: {
  error: Error & { digest?: string };
  reset: () => void;
}) {
  useEffect(() => {
    // Optionally log to an error reporting service
  }, [error]);

  return (
    <div className="py-8">
      <div className="max-w-2xl mx-auto px-4">
        <div className="card-brutal p-8 bg-white text-center">
          <div className="w-16 h-16 bg-accent-coral border-2 border-ink flex items-center justify-center mx-auto mb-6">
            <span className="text-white font-display text-2xl">!</span>
          </div>
          <h2 className="text-display-sm font-display mb-2">
            페이지 오류
          </h2>
          <p className="text-neutral-600 text-sm mb-6">
            이 페이지에서 오류가 발생했습니다. 다시 시도하거나 대시보드로 이동해주세요.
          </p>
          <div className="flex gap-3 justify-center">
            <button
              onClick={reset}
              className="px-6 py-3 bg-ink text-paper font-semibold border-2 border-ink hover:bg-neutral-800 transition-colors"
            >
              다시 시도
            </button>
            <Link
              href="/dashboard"
              className="px-6 py-3 bg-white text-ink font-semibold border-2 border-ink hover:bg-neutral-50 transition-colors"
            >
              대시보드로 이동
            </Link>
          </div>
        </div>
      </div>
    </div>
  );
}
