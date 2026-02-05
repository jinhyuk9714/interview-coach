'use client';

import { cn } from '@/lib/utils';
import { ReactNode } from 'react';

export interface TagProps {
  children: ReactNode;
  variant?: 'default' | 'lime' | 'coral' | 'blue' | 'purple';
  size?: 'sm' | 'md';
  className?: string;
}

const variants = {
  default: 'tag',
  lime: 'tag tag-lime',
  coral: 'tag tag-coral',
  blue: 'tag tag-blue',
  purple: 'tag bg-accent-purple text-white',
};

const sizes = {
  sm: 'text-[10px] px-2 py-0.5',
  md: 'text-xs px-3 py-1',
};

export function Tag({ children, variant = 'default', size = 'md', className }: TagProps) {
  return (
    <span className={cn(variants[variant], sizes[size], className)}>
      {children}
    </span>
  );
}
