'use client';

import { useState } from 'react';
import Link from 'next/link';
import { useRouter } from 'next/navigation';
import { motion } from 'motion/react';
import { Button, Input } from '@/components/ui';
import { authApi } from '@/lib/api';
import { ArrowRight, Mail, Lock, User, Sparkles } from 'lucide-react';

export default function SignupPage() {
  const router = useRouter();
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState('');
  const [formData, setFormData] = useState({
    email: '',
    password: '',
    confirmPassword: '',
    nickname: '',
  });

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError('');

    if (formData.password !== formData.confirmPassword) {
      setError('비밀번호가 일치하지 않습니다.');
      return;
    }

    if (formData.password.length < 8) {
      setError('비밀번호는 8자 이상이어야 합니다.');
      return;
    }

    setIsLoading(true);

    try {
      await authApi.signup({
        email: formData.email,
        password: formData.password,
        nickname: formData.nickname,
      });

      router.push('/login?registered=true');
    } catch (err) {
      setError('회원가입에 실패했습니다. 다시 시도해주세요.');
      console.error('Signup error:', err);
    } finally {
      setIsLoading(false);
    }
  };

  return (
    <motion.div
      initial={{ opacity: 0, y: 20 }}
      animate={{ opacity: 1, y: 0 }}
      className="w-full max-w-md"
    >
      <div className="card-brutal p-8 bg-white">
        <div className="text-center mb-8">
          <motion.div
            initial={{ scale: 0 }}
            animate={{ scale: 1 }}
            transition={{ type: 'spring', bounce: 0.5 }}
            className="inline-flex items-center justify-center w-16 h-16 bg-accent-lime border-2 border-ink mb-4"
          >
            <Sparkles className="w-8 h-8" />
          </motion.div>
          <h1 className="text-display-sm font-display mb-2">시작해볼까요?</h1>
          <p className="text-neutral-600 text-sm">
            무료 계정을 만들고 AI 면접 코칭을 경험하세요
          </p>
        </div>

        {error && (
          <motion.div
            initial={{ opacity: 0, height: 0 }}
            animate={{ opacity: 1, height: 'auto' }}
            className="bg-accent-coral/10 border-2 border-accent-coral p-4 mb-6"
          >
            <p className="text-sm text-accent-coral font-medium">{error}</p>
          </motion.div>
        )}

        <form onSubmit={handleSubmit} className="space-y-5">
          <div className="relative">
            <User className="absolute left-4 top-1/2 -translate-y-1/2 w-5 h-5 text-neutral-400 z-10" />
            <Input
              type="text"
              placeholder="닉네임"
              value={formData.nickname}
              onChange={(e) => setFormData({ ...formData, nickname: e.target.value })}
              className="pl-12"
              required
            />
          </div>

          <div className="relative">
            <Mail className="absolute left-4 top-1/2 -translate-y-1/2 w-5 h-5 text-neutral-400 z-10" />
            <Input
              type="email"
              placeholder="이메일"
              value={formData.email}
              onChange={(e) => setFormData({ ...formData, email: e.target.value })}
              className="pl-12"
              required
            />
          </div>

          <div className="relative">
            <Lock className="absolute left-4 top-1/2 -translate-y-1/2 w-5 h-5 text-neutral-400 z-10" />
            <Input
              type="password"
              placeholder="비밀번호 (8자 이상)"
              value={formData.password}
              onChange={(e) => setFormData({ ...formData, password: e.target.value })}
              className="pl-12"
              required
              minLength={8}
            />
          </div>

          <div className="relative">
            <Lock className="absolute left-4 top-1/2 -translate-y-1/2 w-5 h-5 text-neutral-400 z-10" />
            <Input
              type="password"
              placeholder="비밀번호 확인"
              value={formData.confirmPassword}
              onChange={(e) => setFormData({ ...formData, confirmPassword: e.target.value })}
              className="pl-12"
              required
            />
          </div>

          <Button
            type="submit"
            className="w-full"
            size="lg"
            isLoading={isLoading}
            rightIcon={<ArrowRight className="w-5 h-5" />}
          >
            회원가입
          </Button>
        </form>

        <p className="mt-6 text-xs text-neutral-500 text-center">
          가입 시{' '}
          <Link href="#" className="underline">이용약관</Link>
          {' '}및{' '}
          <Link href="#" className="underline">개인정보처리방침</Link>에 동의합니다.
        </p>

        <div className="mt-8 pt-6 border-t-2 border-dashed border-neutral-200 text-center">
          <p className="text-sm text-neutral-600">
            이미 계정이 있으신가요?{' '}
            <Link
              href="/login"
              className="font-semibold text-ink underline decoration-accent-lime decoration-2 underline-offset-4 hover:text-accent-coral transition-colors"
            >
              로그인
            </Link>
          </p>
        </div>
      </div>
    </motion.div>
  );
}
