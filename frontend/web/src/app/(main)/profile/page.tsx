'use client';

import { useState, useEffect } from 'react';
import { motion } from 'motion/react';
import { Button, Card, Input } from '@/components/ui';
import { useAuthStore } from '@/stores/auth';
import { userApi } from '@/lib/api';
import {
  User,
  Mail,
  Briefcase,
  Calendar,
  Save,
  Loader2,
  CheckCircle2,
  AlertCircle
} from 'lucide-react';

export default function ProfilePage() {
  const { user, setUser } = useAuthStore();

  const [nickname, setNickname] = useState('');
  const [targetPosition, setTargetPosition] = useState('');
  const [experienceYears, setExperienceYears] = useState<number | ''>('');
  const [isLoading, setIsLoading] = useState(true);
  const [isSaving, setIsSaving] = useState(false);
  const [success, setSuccess] = useState(false);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    const fetchProfile = async () => {
      setIsLoading(true);
      try {
        const response = await userApi.getProfile();
        const data = response.data;
        setNickname(data.nickname || '');
        setTargetPosition(data.targetPosition || '');
        setExperienceYears(data.experienceYears ?? '');
      } catch (err) {
        console.error('Failed to fetch profile:', err);
        // Fallback to local state
        if (user) {
          setNickname(user.nickname || '');
          setTargetPosition(user.targetPosition || '');
          setExperienceYears(user.experienceYears ?? '');
        }
      } finally {
        setIsLoading(false);
      }
    };

    fetchProfile();
  }, [user]);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setIsSaving(true);
    setError(null);
    setSuccess(false);

    try {
      const response = await userApi.updateProfile({
        nickname: nickname || undefined,
        targetPosition: targetPosition || undefined,
        experienceYears: experienceYears !== '' ? experienceYears : undefined,
      });

      // Update local user state
      setUser({
        ...user!,
        nickname: response.data.nickname,
        targetPosition: response.data.targetPosition,
        experienceYears: response.data.experienceYears,
      });

      setSuccess(true);
      setTimeout(() => setSuccess(false), 3000);
    } catch (err) {
      console.error('Failed to update profile:', err);
      setError('프로필 업데이트에 실패했습니다. 다시 시도해주세요.');
    } finally {
      setIsSaving(false);
    }
  };

  if (isLoading) {
    return (
      <div className="py-8">
        <div className="max-w-2xl mx-auto px-4 flex items-center justify-center py-20">
          <Loader2 className="w-8 h-8 animate-spin text-neutral-400" />
          <p className="ml-3 text-neutral-500">프로필을 불러오는 중...</p>
        </div>
      </div>
    );
  }

  return (
    <div className="py-8">
      <div className="max-w-2xl mx-auto px-4">
        {/* Header */}
        <motion.div
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          className="mb-8"
        >
          <h1 className="text-display-md font-display mb-2">내 프로필</h1>
          <p className="text-neutral-600">
            프로필 정보를 확인하고 수정하세요
          </p>
        </motion.div>

        {/* Success Message */}
        {success && (
          <motion.div
            initial={{ opacity: 0, y: -10 }}
            animate={{ opacity: 1, y: 0 }}
            exit={{ opacity: 0 }}
            className="mb-6 p-4 bg-green-50 border-2 border-green-500 flex items-center gap-3"
          >
            <CheckCircle2 className="w-5 h-5 text-green-500 shrink-0" />
            <p className="text-sm text-green-700">프로필이 성공적으로 업데이트되었습니다.</p>
          </motion.div>
        )}

        {/* Error Message */}
        {error && (
          <motion.div
            initial={{ opacity: 0, y: -10 }}
            animate={{ opacity: 1, y: 0 }}
            className="mb-6 p-4 bg-accent-coral/10 border-2 border-accent-coral flex items-center gap-3"
          >
            <AlertCircle className="w-5 h-5 text-accent-coral shrink-0" />
            <p className="text-sm text-accent-coral">{error}</p>
          </motion.div>
        )}

        {/* Profile Card */}
        <motion.div
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ delay: 0.1 }}
        >
          <Card className="p-6">
            {/* Avatar & Email */}
            <div className="flex items-center gap-4 mb-8 pb-6 border-b-2 border-dashed border-neutral-200">
              <div className="w-20 h-20 bg-accent-lime border-2 border-ink flex items-center justify-center">
                <span className="font-display text-3xl">
                  {(nickname || user?.nickname || 'U').charAt(0).toUpperCase()}
                </span>
              </div>
              <div>
                <h2 className="font-display text-xl">{nickname || user?.nickname || '사용자'}</h2>
                <div className="flex items-center gap-2 text-neutral-500">
                  <Mail className="w-4 h-4" />
                  <span className="text-sm">{user?.email}</span>
                </div>
                <div className="flex items-center gap-2 text-neutral-400 mt-1">
                  <Calendar className="w-4 h-4" />
                  <span className="text-xs">
                    {user?.createdAt
                      ? `${new Date(user.createdAt).toLocaleDateString('ko-KR')} 가입`
                      : '가입일 정보 없음'}
                  </span>
                </div>
              </div>
            </div>

            {/* Edit Form */}
            <form onSubmit={handleSubmit} className="space-y-6">
              <div>
                <label className="block text-sm font-semibold mb-2 flex items-center gap-2">
                  <User className="w-4 h-4 text-neutral-400" />
                  닉네임
                </label>
                <Input
                  value={nickname}
                  onChange={(e) => setNickname(e.target.value)}
                  placeholder="닉네임을 입력하세요"
                  maxLength={50}
                />
                <p className="text-xs text-neutral-400 mt-1">최대 50자</p>
              </div>

              <div>
                <label className="block text-sm font-semibold mb-2 flex items-center gap-2">
                  <Briefcase className="w-4 h-4 text-neutral-400" />
                  목표 포지션
                </label>
                <Input
                  value={targetPosition}
                  onChange={(e) => setTargetPosition(e.target.value)}
                  placeholder="예: Backend Developer, Frontend Engineer"
                  maxLength={100}
                />
                <p className="text-xs text-neutral-400 mt-1">어떤 포지션을 목표로 하시나요?</p>
              </div>

              <div>
                <label className="block text-sm font-semibold mb-2 flex items-center gap-2">
                  <Calendar className="w-4 h-4 text-neutral-400" />
                  경력 연차
                </label>
                <Input
                  type="number"
                  value={experienceYears}
                  onChange={(e) => setExperienceYears(e.target.value ? parseInt(e.target.value) : '')}
                  placeholder="0"
                  min={0}
                  max={50}
                />
                <p className="text-xs text-neutral-400 mt-1">총 경력 연수 (신입은 0)</p>
              </div>

              <div className="pt-4 border-t-2 border-neutral-200">
                <Button
                  type="submit"
                  isLoading={isSaving}
                  leftIcon={<Save className="w-4 h-4" />}
                  className="w-full"
                >
                  변경 사항 저장
                </Button>
              </div>
            </form>
          </Card>
        </motion.div>

        {/* Account Info */}
        <motion.div
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ delay: 0.2 }}
          className="mt-6"
        >
          <Card className="p-6 bg-neutral-50">
            <h3 className="font-display text-lg mb-4">계정 정보</h3>
            <div className="space-y-3 text-sm">
              <div className="flex justify-between">
                <span className="text-neutral-500">이메일</span>
                <span className="font-mono">{user?.email}</span>
              </div>
              <div className="flex justify-between">
                <span className="text-neutral-500">계정 ID</span>
                <span className="font-mono text-neutral-400">#{user?.id}</span>
              </div>
            </div>
            <p className="text-xs text-neutral-400 mt-4">
              이메일 변경이 필요한 경우 고객센터로 문의해주세요.
            </p>
          </Card>
        </motion.div>
      </div>
    </div>
  );
}
