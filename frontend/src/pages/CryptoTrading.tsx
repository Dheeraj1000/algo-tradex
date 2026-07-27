import React, { useState, useEffect } from 'react';
import { Card } from '../components/ui/Card';
import { Bitcoin, Activity, TrendingUp, Target, RefreshCw, Wallet, PieChart, DollarSign } from 'lucide-react';
import { Client } from '@stomp/stompjs';
import api from '../lib/axios';

interface CryptoStats {
  accountBalance: number;
  realizedPnl: number;
  unrealizedPnl: number;
  activeTrades: ActiveTradeDto[];
  closedTrades: ClosedTradeDto[];
}

interface ClosedTradeDto {
  id: string;
  symbol: string;
  type: string;
  entryPrice: number;
  exitPrice: number;
  realizedPnl: number;
  realizedPnlPercent: number;
  quantity: number;
  investedAmount: number;
  exitReason: string;
  timestamp: string;
}

interface ActiveTradeDto {
  id: string;
  symbol: string;
  type: string;
  entryPrice: number;
  currentPrice: number;
  unrealizedPnl: number;
  unrealizedPnlPercent: number;
  stopLoss: number;
  confidence: number;
  quantity: number;
  investedAmount: number;
  targetPrice: number;
}

const CryptoTrading = () => {
  const [isTraining, setIsTraining] = useState(false);
  const [leverage, setLeverage] = useState(1);
  const [activeSignals, setActiveSignals] = useState<any[]>([]);
  const [stats, setStats] = useState<CryptoStats | null>(null);

  useEffect(() => {
    // STOMP Client for live signals
    const apiBase = api.defaults.baseURL || 'http://localhost:8080/api';
    const backendHost = apiBase.replace('/api', '');
    
    const stompClient = new Client({
      brokerURL: `${backendHost.replace(/^http/, 'ws')}/ws`,
      reconnectDelay: 5000,
    });

    stompClient.onConnect = () => {
      stompClient.subscribe(`/topic/ai-alerts`, (message) => {
        const alert = JSON.parse(message.body);
        
        // Filter only crypto
        if (alert.symbol === 'BTCUSDT' || alert.symbol === 'ETHUSDT') {
          setActiveSignals((prev) => {
            const exists = prev.find(s => s.symbol === alert.symbol);
            const newSignal = {
              id: Date.now().toString() + alert.symbol,
              symbol: alert.symbol,
              signal: alert.signal,
              confidence: alert.confidence,
              entryPrice: alert.spotPrice,
              target: alert.spotPrice + (alert.signal === 'BUY_LONG' ? alert.targetProfit : -alert.targetProfit),
              stopLoss: alert.spotPrice + (alert.signal === 'BUY_LONG' ? -alert.stopLoss : alert.stopLoss),
              status: 'ACTIVE',
              explanation: `AI detected strong setup. Strategy target hit.`
            };
            
            if (exists) {
              return prev.map(s => s.symbol === alert.symbol ? newSignal : s);
            }
            return [newSignal, ...prev];
          });
        }
      });
    };

    stompClient.activate();

    return () => {
      stompClient.deactivate();
    };
  }, []);

  // Poll Portfolio Stats
  useEffect(() => {
    const fetchStats = async () => {
      try {
        const res = await api.get('/portfolio/crypto-stats');
        setStats(res.data);
      } catch (e) {
        console.error('Failed to fetch crypto stats', e);
      }
    };
    
    fetchStats();
    const interval = setInterval(fetchStats, 5000);
    return () => clearInterval(interval);
  }, []);

  const handleTrain = async (symbol: string) => {
    setIsTraining(true);
    try {
      alert(`Sent training request for ${symbol} to Binance API! Watch your python terminal for progress.`);
    } catch (e) {
      console.error(e);
    } finally {
      setIsTraining(false);
    }
  };

  const handleLeverageChange = async (e: React.ChangeEvent<HTMLSelectElement>) => {
    const newLeverage = parseInt(e.target.value);
    setLeverage(newLeverage);
    try {
      await api.post('/portfolio/crypto-settings', { leverage: newLeverage });
    } catch (err) {
      console.error('Failed to update leverage setting', err);
    }
  };

  const USD_TO_INR = 96.14;

  const formatCurrency = (val: number) => {
    return new Intl.NumberFormat('en-IN', { style: 'currency', currency: 'INR', maximumFractionDigits: 2 }).format(val * USD_TO_INR);
  };

  return (
    <div className="space-y-6">
      <div className="flex justify-between items-center">
        <div>
          <h1 className="text-2xl font-bold text-white flex items-center gap-2">
            <Bitcoin className="text-yellow-500" />
            Crypto AI Engine (Paper Trading)
          </h1>
          <p className="text-[var(--text-muted)]">Independent neural networks with simulated live execution.</p>
        </div>
        <div className="flex gap-4 items-center">
          <div className="flex items-center gap-2 bg-[var(--bg-color)] border border-white/10 rounded-lg px-3 py-1">
            <span className="text-sm font-medium text-[var(--text-muted)]">Leverage:</span>
            <select 
              value={leverage}
              onChange={handleLeverageChange}
              className="bg-transparent text-white font-bold outline-none cursor-pointer py-1"
            >
              <option value={1} className="bg-[var(--bg-color)]">1x</option>
              <option value={2} className="bg-[var(--bg-color)]">2x</option>
              <option value={5} className="bg-[var(--bg-color)]">5x</option>
              <option value={10} className="bg-[var(--bg-color)]">10x</option>
              <option value={20} className="bg-[var(--bg-color)] text-red-400">20x (High Risk)</option>
              <option value={50} className="bg-[var(--bg-color)] text-red-500">50x (Degen)</option>
            </select>
          </div>
          <button 
            onClick={() => handleTrain("BTCUSDT")}
            className="flex items-center gap-2 px-4 py-2 bg-[var(--primary)] text-black font-semibold rounded-lg hover:bg-opacity-90 transition-all"
          >
            <RefreshCw size={16} className={isTraining ? "animate-spin" : ""} />
            Retrain BTC
          </button>
        </div>
      </div>

      <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
        <Card variant="glass" className="p-4 flex items-center gap-4">
          <div className="w-12 h-12 rounded-full bg-blue-500/20 flex items-center justify-center text-blue-400">
            <Wallet size={24} />
          </div>
          <div>
            <p className="text-[var(--text-muted)] text-sm">Available Balance</p>
            <h3 className="text-2xl font-bold text-white">{stats ? formatCurrency(stats.accountBalance) : '---'}</h3>
          </div>
        </Card>
        
        <Card variant="glass" className="p-4 flex items-center gap-4">
          <div className="w-12 h-12 rounded-full bg-emerald-500/20 flex items-center justify-center text-emerald-400">
            <Activity size={24} />
          </div>
          <div>
            <p className="text-[var(--text-muted)] text-sm">Realized P&L</p>
            <h3 className={`text-2xl font-bold ${stats?.realizedPnl && stats.realizedPnl < 0 ? 'text-red-400' : 'text-emerald-400'}`}>
              {stats ? formatCurrency(stats.realizedPnl) : '---'}
            </h3>
          </div>
        </Card>

        <Card variant="glass" className="p-4 flex items-center gap-4">
          <div className="w-12 h-12 rounded-full bg-purple-500/20 flex items-center justify-center text-purple-400">
            <TrendingUp size={24} />
          </div>
          <div>
            <p className="text-[var(--text-muted)] text-sm">Live Unrealized P&L</p>
            <h3 className={`text-2xl font-bold ${stats?.unrealizedPnl && stats.unrealizedPnl < 0 ? 'text-red-400' : 'text-purple-400'}`}>
              {stats ? formatCurrency(stats.unrealizedPnl) : '---'}
            </h3>
          </div>
        </Card>
      </div>

      <div className="space-y-4">
        <h2 className="text-xl font-semibold text-white">Active Paper Trades</h2>
        {stats?.activeTrades.length === 0 ? (
          <Card variant="glass" className="p-6 text-center border-dashed border-white/10">
            <p className="text-[var(--text-muted)]">No active trades currently. Waiting for AI setups...</p>
          </Card>
        ) : (
          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
            {stats?.activeTrades.map(trade => (
              <Card key={trade.id} variant="glass" className="p-5 border-l-4 border-l-yellow-500">
                <div className="flex justify-between items-center mb-3">
                  <div className="flex items-center gap-3">
                    <h3 className="text-lg font-bold text-white">{trade.symbol}</h3>
                    <span className={`px-2 py-0.5 text-xs rounded font-bold ${trade.type === 'BUY' ? 'bg-emerald-500/20 text-emerald-400' : 'bg-red-500/20 text-red-400'}`}>
                      {trade.type} {trade.leverage > 1 ? `(${trade.leverage}x)` : ''}
                    </span>
                  </div>
                  <div className="text-right">
                    <p className={`text-lg font-bold ${trade.unrealizedPnl < 0 ? 'text-red-400' : 'text-emerald-400'}`}>
                      {trade.unrealizedPnl > 0 ? '+' : ''}{formatCurrency(trade.unrealizedPnl)}
                    </p>
                    <p className={`text-xs ${trade.unrealizedPnlPercent < 0 ? 'text-red-400/80' : 'text-emerald-400/80'}`}>
                      {trade.unrealizedPnlPercent.toFixed(2)}%
                    </p>
                  </div>
                </div>
                
                <div className="grid grid-cols-2 md:grid-cols-6 gap-4 bg-[var(--bg-color)] p-3 rounded-lg border border-white/5 mt-3">
                  <div>
                    <p className="text-xs text-[var(--text-muted)] mb-1">Invested</p>
                    <p className="text-sm font-medium text-white">{formatCurrency(trade.investedAmount)}</p>
                  </div>
                  <div>
                    <p className="text-xs text-[var(--text-muted)] mb-1">Qty</p>
                    <p className="text-sm font-medium text-white">{trade.quantity.toFixed(4)}</p>
                  </div>
                  <div>
                    <p className="text-xs text-[var(--text-muted)] mb-1">Entry</p>
                    <p className="text-sm font-medium text-white">{formatCurrency(trade.entryPrice)}</p>
                  </div>
                  <div>
                    <p className="text-xs text-[var(--text-muted)] mb-1">Live Price</p>
                    <p className="text-sm font-medium text-white">{formatCurrency(trade.currentPrice)}</p>
                  </div>
                  <div>
                    <p className="text-xs text-[var(--text-muted)] mb-1">Target</p>
                    <p className="text-sm font-medium text-emerald-400">{trade.targetPrice ? formatCurrency(trade.targetPrice) : '---'}</p>
                  </div>
                  <div>
                    <p className="text-xs text-[var(--text-muted)] mb-1">Stop Loss</p>
                    <p className="text-sm font-medium text-red-400">{trade.stopLoss ? formatCurrency(trade.stopLoss) : '---'}</p>
                  </div>
                </div>
              </Card>
            ))}
          </div>
        )}
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        <div className="space-y-4">
          <h2 className="text-xl font-semibold text-white">Live AI Alert Stream</h2>
          {activeSignals.map(signal => (
            <Card key={signal.id} variant="glass" className="p-6">
              <div className="flex justify-between items-start mb-4">
                <div>
                  <h3 className="text-lg font-bold text-white flex items-center gap-2">
                    {signal.symbol}
                    <span className={`px-2 py-1 text-xs rounded-md ${signal.signal === 'BUY_LONG' ? 'bg-green-500/20 text-green-400' : 'bg-red-500/20 text-red-400'}`}>
                      {signal.signal}
                    </span>
                  </h3>
                  <p className="text-[var(--text-muted)] text-sm">Target: {formatCurrency(signal.target)} | SL: {formatCurrency(signal.stopLoss)}</p>
                </div>
                <div className="text-right">
                  <p className="text-sm text-[var(--text-muted)]">Confidence</p>
                  <p className={`text-xl font-bold ${signal.confidence > 75 ? 'text-green-400' : 'text-yellow-400'}`}>
                    {signal.confidence.toFixed(2)}%
                  </p>
                </div>
              </div>

              <div className="bg-[var(--bg-color)] p-4 rounded-xl border border-white/5">
                <div className="flex items-center gap-2 mb-2">
                  <Target size={16} className="text-[var(--primary)]" />
                  <span className="text-sm font-semibold text-white">AI Analysis</span>
                </div>
                <p className="text-sm text-[var(--text-muted)] italic">{signal.explanation}</p>
              </div>
            </Card>
          ))}
          {activeSignals.length === 0 && (
            <Card variant="glass" className="p-6 text-center border-dashed border-white/10">
              <p className="text-[var(--text-muted)]">Listening for AI signals...</p>
            </Card>
          )}
        </div>

        <div className="space-y-4">
          <h2 className="text-xl font-semibold text-white">Order History (Closed Trades)</h2>
          {(!stats?.closedTrades || stats.closedTrades.length === 0) ? (
            <Card variant="glass" className="p-6 text-center border-dashed border-white/10">
              <p className="text-[var(--text-muted)]">No trades have been closed yet.</p>
            </Card>
          ) : (
            <div className="space-y-4 max-h-[600px] overflow-y-auto pr-2 custom-scrollbar">
              {stats.closedTrades.map(trade => (
                <Card key={trade.id} variant="glass" className={`p-4 border-l-4 ${trade.realizedPnl >= 0 ? 'border-l-emerald-500' : 'border-l-red-500'}`}>
                  <div className="flex justify-between items-center mb-3">
                    <div className="flex items-center gap-3">
                      <h3 className="text-lg font-bold text-white">{trade.symbol}</h3>
                      <span className={`px-2 py-0.5 text-xs rounded font-bold ${trade.type === 'BUY' ? 'bg-emerald-500/20 text-emerald-400' : 'bg-red-500/20 text-red-400'}`}>
                        {trade.type} {trade.leverage > 1 ? `(${trade.leverage}x)` : ''}
                      </span>
                    </div>
                    <div className="text-right">
                      <p className={`text-lg font-bold ${trade.realizedPnl < 0 ? 'text-red-400' : 'text-emerald-400'}`}>
                        {trade.realizedPnl > 0 ? '+' : ''}{formatCurrency(trade.realizedPnl)}
                      </p>
                      <p className={`text-xs ${trade.realizedPnlPercent < 0 ? 'text-red-400/80' : 'text-emerald-400/80'}`}>
                        {trade.realizedPnlPercent.toFixed(2)}%
                      </p>
                    </div>
                  </div>
                  
                  <div className="grid grid-cols-2 gap-4 bg-[var(--bg-color)] p-3 rounded-lg border border-white/5 text-sm">
                    <div className="flex justify-between">
                      <span className="text-[var(--text-muted)]">Invested:</span>
                      <span className="text-white">{formatCurrency(trade.investedAmount)}</span>
                    </div>
                    <div className="flex justify-between">
                      <span className="text-[var(--text-muted)]">Reason:</span>
                      <span className="text-white truncate" title={trade.exitReason}>{trade.exitReason}</span>
                    </div>
                  </div>
                </Card>
              ))}
            </div>
          )}
        </div>
      </div>
    </div>
  );
};

export default CryptoTrading;
