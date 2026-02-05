'use client';

import { cn } from '@/lib/utils';
import { forwardRef, HTMLAttributes, ReactNode } from 'react';

export interface CardProps extends HTMLAttributes<HTMLDivElement> {
  variant?: 'default' | 'brutal' | 'ghost';
  hover?: boolean;
}

const variants = {
  default: 'bg-white border border-neutral-200',
  brutal: 'card-brutal',
  ghost: 'bg-transparent',
};

export const Card = forwardRef<HTMLDivElement, CardProps>(
  ({ className, variant = 'brutal', hover = true, children, ...props }, ref) => {
    return (
      <div
        ref={ref}
        className={cn(
          'p-6',
          variants[variant],
          !hover && 'hover:shadow-brutal hover:translate-x-0 hover:translate-y-0',
          className
        )}
        {...props}
      >
        {children}
      </div>
    );
  }
);

Card.displayName = 'Card';

export function CardHeader({ className, children }: { className?: string; children: ReactNode }) {
  return <div className={cn('mb-4', className)}>{children}</div>;
}

export function CardTitle({ className, children }: { className?: string; children: ReactNode }) {
  return <h3 className={cn('text-xl font-display', className)}>{children}</h3>;
}

export function CardDescription({ className, children }: { className?: string; children: ReactNode }) {
  return <p className={cn('text-sm text-neutral-500 mt-1', className)}>{children}</p>;
}

export function CardContent({ className, children }: { className?: string; children: ReactNode }) {
  return <div className={cn('', className)}>{children}</div>;
}

export function CardFooter({ className, children }: { className?: string; children: ReactNode }) {
  return <div className={cn('mt-4 pt-4 border-t-2 border-dashed border-neutral-200', className)}>{children}</div>;
}
