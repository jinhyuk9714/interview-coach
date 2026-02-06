'use client';

import { useState } from 'react';
import Link from 'next/link';
import { useRouter } from 'next/navigation';
import { motion } from 'motion/react';
import { Button, Input } from '@/components/ui';
import { useAuthStore } from '@/stores/auth';
import { authApi } from '@/lib/api';
import { ArrowRight, Mail, Lock } from 'lucide-react';
import { toast } from 'sonner';

export default function LoginPage() {
  const router = useRouter();
  const { login } = useAuthStore();
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState('');
  const [formData, setFormData] = useState({
    email: '',
    password: '',
  });

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError('');
    setIsLoading(true);

    try {
      const response = await authApi.login(formData);
      const { accessToken, refreshToken } = response.data;

      // Decode user info from JWT payload
      const base64Url = accessToken.split('.')[1];
      const base64 = base64Url.replace(/-/g, '+').replace(/_/g, '/');
      const payload = JSON.parse(decodeURIComponent(
        atob(base64).split('').map(c => '%' + ('00' + c.charCodeAt(0).toString(16)).slice(-2)).join('')
      ));

      login(
        {
          id: payload.sub || payload.userId,
          email: payload.email || formData.email,
          nickname: payload.nickname || formData.email.split('@')[0],
          createdAt: new Date().toISOString(),
        },
        accessToken,
        refreshToken
      );

      router.push('/dashboard');
    } catch {
      setError('이메일 또는 비밀번호가 올바르지 않습니다.');
      toast.error('로그인에 실패했습니다.');
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
          <h1 className="text-display-sm font-display mb-2">다시 만나서 반가워요</h1>
          <p className="text-neutral-600 text-sm">
            로그인하고 면접 준비를 계속하세요
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

        <form onSubmit={handleSubmit} className="space-y-6" aria-label="로그인 폼">
          <div className="relative">
            <Mail className="absolute left-4 top-1/2 -translate-y-1/2 w-5 h-5 text-neutral-400 z-10" />
            <Input
              type="email"
              placeholder="이메일"
              aria-label="이메일"
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
              placeholder="비밀번호"
              aria-label="비밀번호"
              value={formData.password}
              onChange={(e) => setFormData({ ...formData, password: e.target.value })}
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
            로그인
          </Button>
        </form>

        <div className="mt-8 pt-6 border-t-2 border-dashed border-neutral-200 text-center">
          <p className="text-sm text-neutral-600">
            계정이 없으신가요?{' '}
            <Link
              href="/signup"
              className="font-semibold text-ink underline decoration-accent-coral decoration-2 underline-offset-4 hover:text-accent-coral transition-colors"
            >
              회원가입
            </Link>
          </p>
        </div>
      </div>

      {/* Decorative elements */}
      <div className="mt-8 flex justify-center gap-2">
        {[...Array(5)].map((_, i) => (
          <motion.div
            key={i}
            initial={{ scale: 0 }}
            animate={{ scale: 1 }}
            transition={{ delay: 0.3 + i * 0.1 }}
            className="w-2 h-2 bg-ink"
          />
        ))}
      </div>
    </motion.div>
  );
}
