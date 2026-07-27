import React, { forwardRef } from 'react';
import type { InputHTMLAttributes, ReactNode } from 'react';
import clsx from 'clsx';
import styles from './Input.module.css';
import { motion, AnimatePresence } from 'framer-motion';

export interface InputProps extends InputHTMLAttributes<HTMLInputElement> {
  label?: string;
  error?: string;
  leftIcon?: ReactNode;
  rightIcon?: ReactNode;
}

export const Input = forwardRef<HTMLInputElement, InputProps>(
  ({ className, label, error, leftIcon, rightIcon, id, ...props }, ref) => {
    
    const inputId = id || `input-${Math.random().toString(36).substring(2, 9)}`;

    return (
      <div className={clsx(styles.container, className)}>
        {label && (
          <label htmlFor={inputId} className={styles.label}>
            {label}
          </label>
        )}
        
        <div className={clsx(
          styles.inputWrapper, 
          leftIcon && styles.iconLeft,
          rightIcon && styles.iconRight
        )}>
          {leftIcon && (
            <div className={clsx(styles.icon, styles.left)}>
              {leftIcon}
            </div>
          )}
          
          <input
            id={inputId}
            ref={ref}
            className={clsx(styles.input, error && styles.error)}
            {...props}
          />
          
          {rightIcon && (
            <div className={clsx(styles.icon, styles.right)}>
              {rightIcon}
            </div>
          )}
        </div>
        
        <AnimatePresence>
          {error && (
            <motion.span 
              initial={{ opacity: 0, y: -5 }}
              animate={{ opacity: 1, y: 0 }}
              exit={{ opacity: 0, y: -5 }}
              className={styles.errorMessage}
            >
              {error}
            </motion.span>
          )}
        </AnimatePresence>
      </div>
    );
  }
);

Input.displayName = 'Input';
