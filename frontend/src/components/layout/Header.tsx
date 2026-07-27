import React from 'react';
import { Bell, Search, Menu } from 'lucide-react';
import { useAuthStore } from '../../store/authStore';
import { Input } from '../ui/Input';

export const Header = ({ onMenuClick }: { onMenuClick?: () => void }) => {
  const { user } = useAuthStore();

  return (
    <header className="h-20 bg-[var(--bg-color)]/80 backdrop-blur-md border-b border-[var(--border)] sticky top-0 z-30 flex items-center justify-between px-4 md:px-6">
      <div className="flex items-center gap-4">
        <button 
          onClick={onMenuClick}
          className="md:hidden p-2 rounded-lg bg-[var(--bg-surface)] text-[var(--text-muted)] hover:text-white"
        >
          <Menu size={24} />
        </button>
        
        <div className="hidden md:block w-96">
          <Input 
            placeholder="Search markets, pairs, or orders..." 
            leftIcon={<Search size={18} />}
            className="mb-0"
          />
        </div>
      </div>

      <div className="flex items-center gap-4">
        <button className="relative p-2 rounded-lg bg-[var(--bg-surface)] text-[var(--text-muted)] hover:text-white transition-colors">
          <Bell size={20} />
          <span className="absolute top-2 right-2 w-2 h-2 rounded-full bg-[var(--primary)] border border-[var(--bg-surface)]"></span>
        </button>

        <div className="flex items-center gap-3 pl-4 border-l border-[var(--border)]">
          <div className="text-right hidden sm:block">
            <div className="text-sm font-medium text-white">{user?.name || 'Trader'}</div>
            <div className="text-xs text-[var(--text-muted)]">{user?.role || 'Pro Member'}</div>
          </div>
          <div className="w-10 h-10 rounded-full bg-gradient-to-br from-[var(--primary)] to-[var(--secondary)] flex items-center justify-center text-white font-bold shadow-glow">
            {user?.name ? user.name.charAt(0).toUpperCase() : 'T'}
          </div>
        </div>
      </div>
    </header>
  );
};
