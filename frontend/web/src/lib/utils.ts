import { clsx, type ClassValue } from 'clsx';

export function cn(...inputs: ClassValue[]) {
  return clsx(inputs);
}

export function formatDate(dateString: string): string {
  const date = new Date(dateString);
  return new Intl.DateTimeFormat('ko-KR', {
    year: 'numeric',
    month: 'long',
    day: 'numeric',
  }).format(date);
}

export function formatDateTime(dateString: string): string {
  const date = new Date(dateString);
  return new Intl.DateTimeFormat('ko-KR', {
    year: 'numeric',
    month: 'short',
    day: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
  }).format(date);
}

export function formatRelativeTime(dateString: string): string {
  const date = new Date(dateString);
  const now = new Date();
  const diff = now.getTime() - date.getTime();

  const minutes = Math.floor(diff / 60000);
  const hours = Math.floor(diff / 3600000);
  const days = Math.floor(diff / 86400000);

  if (minutes < 1) return '방금 전';
  if (minutes < 60) return `${minutes}분 전`;
  if (hours < 24) return `${hours}시간 전`;
  if (days < 7) return `${days}일 전`;

  return formatDate(dateString);
}

export function getDifficultyLabel(difficulty: number): string {
  const labels: Record<number, string> = {
    1: '입문',
    2: '기초',
    3: '중급',
    4: '고급',
    5: '심화',
  };
  return labels[difficulty] || '중급';
}

export function getDifficultyColor(difficulty: number): string {
  const colors: Record<number, string> = {
    1: 'bg-green-100 text-green-800 border-green-300',
    2: 'bg-blue-100 text-blue-800 border-blue-300',
    3: 'bg-yellow-100 text-yellow-800 border-yellow-300',
    4: 'bg-orange-100 text-orange-800 border-orange-300',
    5: 'bg-red-100 text-red-800 border-red-300',
  };
  return colors[difficulty] || colors[3];
}

export function getQuestionTypeLabel(type: string): string {
  const labels: Record<string, string> = {
    technical: '기술',
    behavioral: '인성',
    mixed: '종합',
    follow_up: '꼬리질문',
  };
  return labels[type] || type;
}

export function getScoreColor(score: number): string {
  if (score >= 80) return 'stroke-green-500';
  if (score >= 60) return 'stroke-yellow-500';
  if (score >= 40) return 'stroke-orange-500';
  return 'stroke-red-500';
}

export function truncateText(text: string, maxLength: number): string {
  if (text.length <= maxLength) return text;
  return text.slice(0, maxLength) + '...';
}

export function generateId(): string {
  return Math.random().toString(36).substring(2, 9);
}

export function debounce<T extends (...args: unknown[]) => unknown>(
  func: T,
  wait: number
): (...args: Parameters<T>) => void {
  let timeout: NodeJS.Timeout | null = null;

  return function (...args: Parameters<T>) {
    if (timeout) clearTimeout(timeout);
    timeout = setTimeout(() => func(...args), wait);
  };
}
