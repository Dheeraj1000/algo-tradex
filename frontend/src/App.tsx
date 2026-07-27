import React from 'react';
import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';

import Login from './pages/Login';
import Register from './pages/Register';
import Dashboard from './pages/Dashboard';
import Trading from './pages/Trading';
import Portfolio from './pages/Portfolio';
import Settings from './pages/Settings';
import Strategies from './pages/Strategies';
import ProtectedRoute from './components/ProtectedRoute';
import { DashboardLayout } from './components/layout/DashboardLayout';

const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      refetchOnWindowFocus: false,
      retry: 1,
    },
  },
});

import History from './pages/History';
import TradeManagementDashboard from './pages/TradeManagementDashboard';
import CryptoTrading from './pages/CryptoTrading';

function App() {
  return (
    <QueryClientProvider client={queryClient}>
      <BrowserRouter>
        <Routes>
          {/* Public Routes */}
          <Route path="/login" element={<Login />} />
          <Route path="/register" element={<Register />} />
          
          {/* Protected Routes */}
          <Route element={<ProtectedRoute />}>
            <Route element={<DashboardLayout />}>
              <Route path="/dashboard" element={<Dashboard />} />
              <Route path="/trading" element={<Trading />} />
              <Route path="/portfolio" element={<Portfolio />} />
              <Route path="/history" element={<History />} />
              <Route path="/settings" element={<Settings />} />
              <Route path="/strategies" element={<Strategies />} />
              <Route path="/trade-management" element={<TradeManagementDashboard />} />
              <Route path="/crypto" element={<CryptoTrading />} />
            </Route>
          </Route>

          {/* Fallback */}
          <Route path="/" element={<Navigate to="/dashboard" replace />} />
          <Route path="*" element={<Navigate to="/dashboard" replace />} />
        </Routes>
      </BrowserRouter>
    </QueryClientProvider>
  );
}

export default App;
