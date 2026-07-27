import React from 'react';
import { NavLink } from 'react-router-dom';
import { LayoutDashboard, LineChart, History, Settings, LogOut, Wallet, X, Cpu, ShieldAlert, Bitcoin } from 'lucide-react';
import { useAuthStore } from '../../store/authStore';
import clsx from 'clsx';
import { motion } from 'framer-motion';

const navItems = [
  { icon: LayoutDashboard, label: 'Dashboard', path: '/dashboard' },
  { icon: LineChart, label: 'Trading', path: '/trading' },
  { icon: Wallet, label: 'Portfolio', path: '/portfolio' },
  { icon: History, label: 'History', path: '/history' },
  { icon: Cpu, label: 'Strategies', path: '/strategies' },
  { icon: ShieldAlert, label: 'AI Management', path: '/trade-management' },
  { icon: Bitcoin, label: 'Crypto AI', path: '/crypto' },
  { icon: Settings, label: 'Settings', path: '/settings' },
];

export const Sidebar = ({ isOpen, onClose }: { isOpen?: boolean; onClose?: () => void }) => {
  const { logout } = useAuthStore();

  return (
    <div className={clsx(
        "fixed inset-y-0 left-0 z-50 w-64 lg:w-72 xl:w-80 bg-[var(--bg-surface)] border-r border-[var(--border)] flex flex-col transform transition-transform duration-300 ease-in-out md:relative md:translate-x-0",
        isOpen ? "translate-x-0" : "-translate-x-full"
      )}>
        <div className="p-4 md:p-6 lg:p-8 flex items-center justify-between">
          <div className="flex items-center gap-3 lg:gap-4">
            <div className="w-8 h-8 lg:w-11 lg:h-11 rounded-xl bg-gradient-to-tr from-[var(--primary)] to-[var(--secondary)] flex items-center justify-center shadow-glow">
              <LineChart className="text-white w-4 h-4 lg:w-6 lg:h-6" />
            </div>
            <span className="text-xl lg:text-2xl font-extrabold text-white tracking-tight">AlgoTradeX</span>
          </div>
          {/* Mobile Close Button */}
          <button 
            className="md:hidden p-1 text-[var(--text-muted)] hover:text-white"
            onClick={onClose}
          >
            <X size={20} />
          </button>
        </div>

      <nav className="flex-1 px-5 lg:px-7 py-8 lg:py-10 space-y-6 lg:space-y-7 xl:space-y-8">
        {navItems.map((item) => (
          <NavLink
            key={item.path}
            to={item.path}
            className={({ isActive }) => clsx(
              'flex items-center gap-5 lg:gap-6 px-6 py-4.5 lg:px-7 lg:py-5 rounded-2xl transition-all duration-200 group relative overflow-hidden',
              isActive 
                ? 'text-white bg-[var(--primary)]/10 font-bold shadow-sm' 
                : 'text-[var(--text-muted)] hover:text-white hover:bg-[var(--bg-surface-hover)] font-semibold'
            )}
          >
            {({ isActive }) => (
              <>
                {isActive && (
                  <motion.div
                    layoutId="activeTab"
                    className="absolute left-0 top-0 bottom-0 w-1.5 bg-[var(--primary)] rounded-r-full"
                    initial={false}
                    transition={{ type: "spring", stiffness: 300, damping: 30 }}
                  />
                )}
                <item.icon className={clsx(
                  "w-5 h-5 lg:w-6 lg:h-6 transition-colors duration-200",
                  isActive ? 'text-[var(--primary)]' : 'text-[var(--text-muted)] group-hover:text-white'
                )} />
                <span className="text-sm lg:text-base xl:text-lg tracking-wide">{item.label}</span>
              </>
            )}
          </NavLink>
        ))}
      </nav>

      <div className="p-5 lg:p-8 border-t border-[var(--border)]">
        <button
          onClick={() => logout()}
          className="flex items-center gap-5 lg:gap-6 px-6 py-4.5 lg:px-7 lg:py-5 w-full rounded-2xl text-[var(--text-muted)] hover:text-[var(--danger)] hover:bg-[var(--danger-bg)] transition-colors font-bold text-sm lg:text-base xl:text-lg tracking-wide"
        >
          <LogOut className="w-5 h-5 lg:w-6 lg:h-6" />
          <span>Logout</span>
        </button>
      </div>
    </div>
  );
};
