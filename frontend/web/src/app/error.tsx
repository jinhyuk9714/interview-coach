'use client';

import { useEffect } from 'react';

export default function GlobalError({
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
    <div className="min-h-screen flex items-center justify-center bg-cream p-4">
      <div className="card-brutal p-8 bg-white max-w-md w-full text-center">
        <div className="w-16 h-16 bg-accent-coral border-2 border-ink flex items-center justify-center mx-auto mb-6">
          <span className="text-white font-display text-2xl">!</span>
        </div>
        <h1 className="text-display-sm font-display mb-2">
          문제가 발생했습니다
        </h1>
        <p className="text-neutral-600 text-sm mb-6">
          예기치 않은 오류가 발생했습니다. 다시 시도해주세요.
        </p>
        <button
          onClick={reset}
          className="px-6 py-3 bg-ink text-paper font-semibold border-2 border-ink hover:bg-neutral-800 transition-colors"
        >
          다시 시도
        </button>
      </div>
    </div>
  );
}
