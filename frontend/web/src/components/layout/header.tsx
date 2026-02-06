'use client';

import Link from 'next/link';
import { usePathname } from 'next/navigation';
import { cn } from '@/lib/utils';
import { useAuthStore } from '@/stores/auth';
import { Button } from '@/components/ui';
import { motion } from 'motion/react';
import {
  LayoutDashboard,
  FileText,
  MessageSquare,
  BarChart3,
  LogOut,
  Menu,
  X,
  User,
  History
} from 'lucide-react';
import { useState } from 'react';

const navItems = [
  { href: '/dashboard', label: 'Dashboard', icon: LayoutDashboard },
  { href: '/jd', label: 'JD 분석', icon: FileText },
  { href: '/interview', label: '면접', icon: MessageSquare },
  { href: '/history', label: '기록', icon: History },
  { href: '/statistics', label: '통계', icon: BarChart3 },
];

export function Header() {
  const pathname = usePathname();
  const { user, isAuthenticated, logout } = useAuthStore();
  const [mobileMenuOpen, setMobileMenuOpen] = useState(false);

  return (
    <header className="fixed top-0 left-0 right-0 z-50 bg-paper/95 backdrop-blur-sm border-b-2 border-ink">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <div className="flex items-center justify-between h-16">
          {/* Logo */}
          <Link href="/" className="flex items-center gap-3 group">
            <div className="w-10 h-10 bg-ink flex items-center justify-center">
              <span className="text-accent-lime font-display text-xl italic">i</span>
            </div>
            <span className="font-display text-xl hidden sm:block">
              Interview<span className="text-accent-coral italic">Coach</span>
            </span>
          </Link>

          {/* Desktop Navigation */}
          {isAuthenticated && (
            <nav className="hidden md:flex items-center gap-1" aria-label="메인 내비게이션">
              {navItems.map((item) => {
                const isActive = pathname.startsWith(item.href);
                const Icon = item.icon;
                return (
                  <Link
                    key={item.href}
                    href={item.href}
                    aria-current={isActive ? 'page' : undefined}
                    className={cn(
                      'relative px-4 py-2 font-sans text-sm font-medium uppercase tracking-wide',
                      'transition-colors duration-150',
                      isActive ? 'text-ink' : 'text-neutral-500 hover:text-ink'
                    )}
                  >
                    <span className="flex items-center gap-2">
                      <Icon className="w-4 h-4" />
                      {item.label}
                    </span>
                    {isActive && (
                      <motion.div
                        layoutId="nav-underline"
                        className="absolute bottom-0 left-0 right-0 h-0.5 bg-accent-coral"
                      />
                    )}
                  </Link>
                );
              })}
            </nav>
          )}

          {/* User Menu */}
          <div className="flex items-center gap-4">
            {isAuthenticated ? (
              <>
                <Link href="/profile" className="hidden sm:flex items-center gap-3 hover:opacity-80 transition-opacity">
                  <div className="w-8 h-8 bg-accent-lime border-2 border-ink flex items-center justify-center">
                    <span className="font-mono text-xs font-bold">
                      {user?.nickname?.charAt(0).toUpperCase() || 'U'}
                    </span>
                  </div>
                  <span className="font-sans text-sm font-medium">
                    {user?.nickname}
                  </span>
                </Link>
                <Button
                  variant="ghost"
                  size="sm"
                  onClick={logout}
                  className="hidden sm:flex"
                  aria-label="로그아웃"
                >
                  <LogOut className="w-4 h-4" />
                </Button>
                {/* Mobile menu button */}
                <button
                  onClick={() => setMobileMenuOpen(!mobileMenuOpen)}
                  className="md:hidden p-2"
                  aria-label={mobileMenuOpen ? '메뉴 닫기' : '메뉴 열기'}
                  aria-expanded={mobileMenuOpen}
                >
                  {mobileMenuOpen ? <X className="w-6 h-6" /> : <Menu className="w-6 h-6" />}
                </button>
              </>
            ) : (
              <div className="flex items-center gap-3">
                <Link href="/login">
                  <Button variant="secondary" size="sm">로그인</Button>
                </Link>
                <Link href="/signup" className="hidden sm:block">
                  <Button variant="primary" size="sm">시작하기</Button>
                </Link>
              </div>
            )}
          </div>
        </div>
      </div>

      {/* Mobile Menu */}
      {mobileMenuOpen && isAuthenticated && (
        <motion.div
          initial={{ opacity: 0, y: -10 }}
          animate={{ opacity: 1, y: 0 }}
          exit={{ opacity: 0, y: -10 }}
          className="md:hidden bg-white border-b-2 border-ink"
        >
          <nav className="px-4 py-4 space-y-2" aria-label="모바일 내비게이션">
            {navItems.map((item) => {
              const isActive = pathname.startsWith(item.href);
              const Icon = item.icon;
              return (
                <Link
                  key={item.href}
                  href={item.href}
                  onClick={() => setMobileMenuOpen(false)}
                  className={cn(
                    'flex items-center gap-3 px-4 py-3 font-sans text-sm font-medium',
                    isActive
                      ? 'bg-accent-lime text-ink border-2 border-ink'
                      : 'text-neutral-600 hover:bg-neutral-100'
                  )}
                >
                  <Icon className="w-5 h-5" />
                  {item.label}
                </Link>
              );
            })}
            <Link
              href="/profile"
              onClick={() => setMobileMenuOpen(false)}
              className="flex items-center gap-3 px-4 py-3 font-sans text-sm font-medium text-neutral-600 hover:bg-neutral-100"
            >
              <User className="w-5 h-5" />
              프로필
            </Link>
            <button
              onClick={() => {
                logout();
                setMobileMenuOpen(false);
              }}
              className="flex items-center gap-3 px-4 py-3 w-full text-left font-sans text-sm font-medium text-neutral-600 hover:bg-neutral-100"
            >
              <LogOut className="w-5 h-5" />
              로그아웃
            </button>
          </nav>
        </motion.div>
      )}
    </header>
  );
}
