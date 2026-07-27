import React, { useState, useEffect } from 'react';
import { Card } from '../components/ui/Card';
import { Activity, ShieldAlert, TrendingUp, Clock, Target } from 'lucide-react';
import api from '../lib/axios';

const TradeManagementDashboard = () => {
  // Mock data for initial UI rendering since endpoints aren't fully wired to frontend yet
  const [activeTrades, setActiveTrades] = useState([
    {
      id: '1',
      symbol: 'NIFTY',
      targetOption: 'NIFTY 24500 CE',
      entryPrice: 120.5,
      currentPrice: 110.2,
      stopLoss: 105.0,
      target: 150.0,
      status: 'OPEN',
      aiAction: 'HOLD',
      recoveryProbability: 72.5,
      explanation: 'Holding position despite dip. Recovery probability is 72.5% based on strong OI support at 24400.'
    },
    {
      id: '2',
      symbol: 'SENSEX',
      targetOption: 'SENSEX 80000 PE',
      entryPrice: 200.0,
      currentPrice: 260.0,
      stopLoss: 240.0, // Trailed SL
      target: 300.0,
      status: 'OPEN',
      aiAction: 'MOVE_STOP_LOSS',
      recoveryProbability: 95.0,
      explanation: 'Price has reached 50% of target. Trailed stop loss to lock in profits.'
    }
  ]);

  return (
    <div className="space-y-6">
      <div className="flex justify-between items-center">
        <div>
          <h1 className="text-2xl font-bold text-white">AI Trade Management</h1>
          <p className="text-[var(--text-muted)]">Live monitoring and dynamic management of open positions.</p>
        </div>
      </div>

      <div className="grid grid-cols-1 md:grid-cols-4 gap-4">
        <Card variant="glass" className="p-4 flex items-center gap-4">
          <div className="w-12 h-12 rounded-full bg-blue-500/20 flex items-center justify-center text-blue-400">
            <Activity size={24} />
          </div>
          <div>
            <p className="text-[var(--text-muted)] text-sm">Active Managed Trades</p>
            <h3 className="text-2xl font-bold text-white">{activeTrades.length}</h3>
          </div>
        </Card>
        
        <Card variant="glass" className="p-4 flex items-center gap-4">
          <div className="w-12 h-12 rounded-full bg-green-500/20 flex items-center justify-center text-green-400">
            <ShieldAlert size={24} />
          </div>
          <div>
            <p className="text-[var(--text-muted)] text-sm">Premature Exits Saved</p>
            <h3 className="text-2xl font-bold text-white">12</h3>
          </div>
        </Card>

        <Card variant="glass" className="p-4 flex items-center gap-4">
          <div className="w-12 h-12 rounded-full bg-purple-500/20 flex items-center justify-center text-purple-400">
            <TrendingUp size={24} />
          </div>
          <div>
            <p className="text-[var(--text-muted)] text-sm">Avg Recovery Acc.</p>
            <h3 className="text-2xl font-bold text-white">88%</h3>
          </div>
        </Card>
      </div>

      <div className="space-y-4">
        <h2 className="text-xl font-semibold text-white">Live Monitoring Stream</h2>
        {activeTrades.map(trade => (
          <Card key={trade.id} variant="glass" className="p-6">
            <div className="flex justify-between items-start mb-4">
              <div>
                <h3 className="text-lg font-bold text-white flex items-center gap-2">
                  {trade.targetOption}
                  <span className="px-2 py-1 bg-green-500/20 text-green-400 text-xs rounded-md">{trade.status}</span>
                </h3>
                <p className="text-[var(--text-muted)] text-sm">Entry: ₹{trade.entryPrice} | CMP: ₹{trade.currentPrice} | Target: ₹{trade.target}</p>
              </div>
              <div className="text-right">
                <p className="text-sm text-[var(--text-muted)]">Recovery Prob.</p>
                <p className={`text-xl font-bold ${trade.recoveryProbability > 50 ? 'text-green-400' : 'text-red-400'}`}>
                  {trade.recoveryProbability}%
                </p>
              </div>
            </div>

            <div className="bg-[var(--bg-color)] p-4 rounded-xl border border-white/5">
              <div className="flex items-center gap-2 mb-2">
                <Target size={16} className="text-[var(--primary)]" />
                <span className="text-sm font-semibold text-white">AI Decision: {trade.aiAction}</span>
              </div>
              <p className="text-sm text-[var(--text-muted)] italic">{trade.explanation}</p>
              <div className="mt-3 flex items-center gap-4 text-xs">
                <span className="text-[var(--primary)]">Current SL: ₹{trade.stopLoss}</span>
              </div>
            </div>
          </Card>
        ))}
      </div>
    </div>
  );
};

export default TradeManagementDashboard;
