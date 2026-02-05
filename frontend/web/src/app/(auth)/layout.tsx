import Link from 'next/link';

export default function AuthLayout({
  children,
}: {
  children: React.ReactNode;
}) {
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
