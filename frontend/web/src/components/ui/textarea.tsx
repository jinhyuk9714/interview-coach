'use client';

import { cn } from '@/lib/utils';
import { forwardRef, TextareaHTMLAttributes } from 'react';

export interface TextareaProps extends TextareaHTMLAttributes<HTMLTextAreaElement> {
  label?: string;
  error?: string;
  hint?: string;
}

export const Textarea = forwardRef<HTMLTextAreaElement, TextareaProps>(
  ({ className, label, error, hint, id, ...props }, ref) => {
    const textareaId = id || props.name;

    return (
      <div className="w-full">
        {label && (
          <label
            htmlFor={textareaId}
            className="block mb-2 font-sans text-sm font-semibold uppercase tracking-wide text-ink"
          >
            {label}
          </label>
        )}
        <textarea
          ref={ref}
          id={textareaId}
          className={cn(
            'textarea-brutal min-h-[120px]',
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

Textarea.displayName = 'Textarea';
