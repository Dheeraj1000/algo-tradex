import React, { useState } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import { motion } from 'framer-motion';
import { Mail, Lock, ArrowRight, TrendingUp } from 'lucide-react';
import { Button } from '../components/ui/Button';
import { Input } from '../components/ui/Input';
import { Card } from '../components/ui/Card';
import { useAuthStore } from '../store/authStore';
import api from '../lib/axios';

const Login = () => {
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState('');
  const [isLoading, setIsLoading] = useState(false);
  const navigate = useNavigate();
  const { setAuth } = useAuthStore();

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError('');
    setIsLoading(true);

    try {
      // Mock login for now, as we don't have the backend running yet, 
      // but let's try calling it anyway in case it is
      try {
        const response = await api.post('/auth/login', { email, password });
        const { accessToken, refreshToken, user } = response.data.data;
        setAuth(user, accessToken, refreshToken);
        navigate('/dashboard');
      } catch (err: any) {
        // Fallback for demo purposes if backend isn't available
        console.warn('Backend login failed, using mock data for demo', err);
        setAuth(
          { id: '1', email, name: 'Demo User', role: 'USER' },
          'mock-access-token', 
          'mock-refresh-token'
        );
        navigate('/dashboard');
      }
    } catch (err: any) {
      setError(err.response?.data?.message || 'Failed to login. Please try again.');
    } finally {
      setIsLoading(false);
    }
  };

  return (
    <div className="min-h-screen flex items-center justify-center relative overflow-hidden bg-[var(--bg-color)]">
      {/* Background decorations */}
      <div className="absolute top-[-10%] left-[-10%] w-[40%] h-[40%] rounded-full bg-[var(--primary)] opacity-10 blur-[120px]" />
      <div className="absolute bottom-[-10%] right-[-10%] w-[40%] h-[40%] rounded-full bg-[var(--secondary)] opacity-10 blur-[120px]" />

      <motion.div 
        initial={{ opacity: 0, y: 20 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ duration: 0.5 }}
        className="w-full max-w-[400px] p-4 z-10"
      >
        <div className="flex flex-col items-center mb-8">
          <div className="w-12 h-12 bg-gradient-to-tr from-[var(--primary)] to-[var(--secondary)] rounded-xl flex items-center justify-center shadow-glow mb-4">
            <TrendingUp size={28} className="text-white" />
          </div>
          <h1 className="text-3xl font-bold text-white mb-2">AlgoTradeX</h1>
          <p className="text-[var(--text-muted)] text-center">Enter your credentials to access your trading dashboard</p>
        </div>

        <Card variant="glass" className="p-6 md:p-8 shadow-xl">
          <form onSubmit={handleSubmit} className="space-y-4">
            <Input
              label="Email Address"
              type="email"
              placeholder="you@example.com"
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              leftIcon={<Mail size={18} />}
              required
            />
            
            <Input
              label="Password"
              type="password"
              placeholder="••••••••"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              leftIcon={<Lock size={18} />}
              required
            />

            {error && (
              <div className="p-3 rounded-lg bg-[var(--danger-bg)] border border-[var(--danger)]/20 text-[var(--danger)] text-sm">
                {error}
              </div>
            )}

            <div className="flex justify-between items-center text-sm">
              <label className="flex items-center gap-2 cursor-pointer">
                <input type="checkbox" className="rounded border-gray-600 bg-gray-800 text-[var(--primary)] focus:ring-[var(--primary)]" />
                <span className="text-[var(--text-muted)] hover:text-white transition-colors">Remember me</span>
              </label>
              <a href="#" className="text-[var(--primary)] hover:text-[var(--primary-hover)] transition-colors">
                Forgot password?
              </a>
            </div>

            <Button 
              type="submit" 
              fullWidth 
              size="lg"
              isLoading={isLoading}
              className="group"
            >
              Sign In
              <ArrowRight size={18} className="ml-2 group-hover:translate-x-1 transition-transform" />
            </Button>
          </form>

          <div className="mt-6 text-center text-sm text-[var(--text-muted)]">
            Don't have an account?{' '}
            <Link to="/register" className="text-[var(--primary)] hover:text-[var(--primary-hover)] font-medium transition-colors">
              Create one
            </Link>
          </div>
        </Card>
      </motion.div>
    </div>
  );
};

export default Login;
