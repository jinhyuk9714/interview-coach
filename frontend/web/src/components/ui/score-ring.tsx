'use client';

import { cn, getScoreColor } from '@/lib/utils';
import { motion } from 'motion/react';

interface ScoreRingProps {
  score: number;
  size?: 'sm' | 'md' | 'lg';
  className?: string;
  showLabel?: boolean;
}

const sizes = {
  sm: { width: 48, strokeWidth: 4, fontSize: 'text-sm' },
  md: { width: 80, strokeWidth: 6, fontSize: 'text-xl' },
  lg: { width: 120, strokeWidth: 8, fontSize: 'text-3xl' },
};

export function ScoreRing({ score, size = 'md', className, showLabel = true }: ScoreRingProps) {
  const { width, strokeWidth, fontSize } = sizes[size];
  const radius = (width - strokeWidth) / 2;
  const circumference = radius * 2 * Math.PI;
  const offset = circumference - (score / 100) * circumference;

  return (
    <div className={cn('relative inline-flex items-center justify-center', className)}>
      <svg width={width} height={width} className="-rotate-90">
        {/* Background track */}
        <circle
          cx={width / 2}
          cy={width / 2}
          r={radius}
          fill="none"
          stroke="currentColor"
          strokeWidth={strokeWidth}
          className="text-neutral-200"
        />
        {/* Progress */}
        <motion.circle
          cx={width / 2}
          cy={width / 2}
          r={radius}
          fill="none"
          strokeWidth={strokeWidth}
          strokeLinecap="round"
          className={getScoreColor(score)}
          strokeDasharray={circumference}
          initial={{ strokeDashoffset: circumference }}
          animate={{ strokeDashoffset: offset }}
          transition={{ duration: 1, ease: 'easeOut' }}
        />
      </svg>
      {showLabel && (
        <span className={cn('absolute font-mono font-bold', fontSize)}>
          {score}
        </span>
      )}
    </div>
  );
}
