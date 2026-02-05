'use client';

import { cn } from '@/lib/utils';
import { forwardRef, InputHTMLAttributes } from 'react';

export interface InputProps extends InputHTMLAttributes<HTMLInputElement> {
  label?: string;
  error?: string;
  hint?: string;
}

export const Input = forwardRef<HTMLInputElement, InputProps>(
  ({ className, label, error, hint, id, ...props }, ref) => {
    const inputId = id || props.name;

    return (
      <div className="w-full">
        {label && (
          <label
            htmlFor={inputId}
            className="block mb-2 font-sans text-sm font-semibold uppercase tracking-wide text-ink"
          >
            {label}
          </label>
        )}
        <input
          ref={ref}
          id={inputId}
          className={cn(
            'input-brutal',
            error && 'border-accent-coral focus:border-accent-coral',
            className
          )}
          {...props}
        />
        {hint && !error && (
          <p className="mt-2 text-xs text-neutral-500 font-mono">{hint}</p>
        )}
        {error && (
          <p className="mt-2 text-xs text-accent-coral font-mono">{error}</p>
        )}
      </div>
    );
  }
);

Input.displayName = 'Input';
