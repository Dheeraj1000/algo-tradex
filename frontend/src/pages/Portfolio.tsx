import React, { useState, useEffect } from 'react';
import { Card } from '../components/ui/Card';
import { TrendingUp, PieChart, ArrowUpRight, ArrowDownRight, IndianRupee } from 'lucide-react';
import api from '../lib/axios';

interface Position {
  id: string;
  instrument: {
    symbol: string;
    tradingSymbol: string;
  };
  side: string;
  quantity: number;
  avgEntryPrice: number;
  currentPrice: number | null;
  unrealizedPnl: number;
  productType: string;
}
interface PortfolioStats {
  accountBalance: number;
  realizedPnl: number;
  unrealizedPnl: number;
}

const Portfolio = () => {
  const [positions, setPositions] = useState<Position[]>([]);
  const [stats, setStats] = useState<PortfolioStats | null>(null);
  const [isLoading, setIsLoading] = useState(true);

  useEffect(() => {
    const fetchData = async () => {
      try {
        const [posRes, statsRes] = await Promise.all([
          api.get('/portfolio/positions'),
          api.get('/portfolio/indian-stats')
        ]);
        setPositions(posRes.data);
        setStats(statsRes.data);
      } catch (error) {
        console.error('Failed to fetch portfolio data', error);
      } finally {
        setIsLoading(false);
      }
    };

    fetchData();
  }, []);

  const totalUnrealizedPnl = positions.reduce((sum, pos) => sum + (pos.unrealizedPnl || 0), 0);
  const isPositive = totalUnrealizedPnl >= 0;

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-2xl font-bold text-white">Portfolio</h1>
        <p className="text-[var(--text-muted)]">Monitor your active positions and holdings across Indian markets.</p>
      </div>

      {/* Summary Cards */}
      <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
        <Card variant="glass" className="p-6">
          <div className="flex items-center gap-4 mb-2">
            <div className="p-3 bg-[var(--primary)]/10 text-[var(--primary)] rounded-lg">
              <IndianRupee size={24} />
            </div>
            <div>
              <p className="text-sm text-[var(--text-muted)]">Total MTM P&L</p>
              <h3 className={`text-2xl font-bold ${isPositive ? 'text-green-400' : 'text-red-400'} flex items-center gap-2`}>
                ₹{Math.abs(totalUnrealizedPnl).toLocaleString('en-IN', { minimumFractionDigits: 2 })}
                {isPositive ? <ArrowUpRight size={20} /> : <ArrowDownRight size={20} />}
              </h3>
            </div>
          </div>
        </Card>
        
        <Card variant="glass" className="p-6">
          <div className="flex items-center gap-4 mb-2">
            <div className="p-3 bg-blue-500/10 text-blue-400 rounded-lg">
              <PieChart size={24} />
            </div>
            <div>
              <p className="text-sm text-[var(--text-muted)]">Available Margin</p>
              <h3 className="text-2xl font-bold text-white">
                ₹{stats?.accountBalance ? stats.accountBalance.toLocaleString('en-IN', { minimumFractionDigits: 2, maximumFractionDigits: 2 }) : '1,00,000.00'}
              </h3>
            </div>
          </div>
        </Card>

        <Card variant="glass" className="p-6">
          <div className="flex items-center gap-4 mb-2">
            <div className="p-3 bg-purple-500/10 text-purple-400 rounded-lg">
              <TrendingUp size={24} />
            </div>
            <div>
              <p className="text-sm text-[var(--text-muted)]">Active Positions</p>
              <h3 className="text-2xl font-bold text-white">{positions.length}</h3>
            </div>
          </div>
        </Card>
      </div>

      {/* Positions Table */}
      <Card variant="glass" className="p-6 overflow-hidden">
        <h2 className="text-xl font-semibold text-white mb-6">Open Positions</h2>
        
        <div className="overflow-x-auto">
          <table className="w-full text-left border-collapse">
            <thead>
              <tr className="border-b border-white/10 text-[var(--text-muted)] text-sm">
                <th className="pb-3 font-medium px-4">Instrument</th>
                <th className="pb-3 font-medium px-4">Product</th>
                <th className="pb-3 font-medium px-4">Side</th>
                <th className="pb-3 font-medium px-4 text-right">Qty</th>
                <th className="pb-3 font-medium px-4 text-right">Avg Price</th>
                <th className="pb-3 font-medium px-4 text-right">LTP</th>
                <th className="pb-3 font-medium px-4 text-right">P&L</th>
              </tr>
            </thead>
            <tbody className="text-sm">
              {isLoading ? (
                <tr>
                  <td colSpan={7} className="text-center py-8 text-[var(--text-muted)]">Loading positions...</td>
                </tr>
              ) : positions.length === 0 ? (
                <tr>
                  <td colSpan={7} className="text-center py-8 text-[var(--text-muted)]">No active positions.</td>
                </tr>
              ) : (
                positions.map((pos) => {
                  const pnl = pos.unrealizedPnl || 0;
                  return (
                    <tr key={pos.id} className="border-b border-white/5 hover:bg-white/[0.02] transition-colors">
                      <td className="py-4 px-4">
                        <div className="font-medium text-white">{pos.instrument?.tradingSymbol || 'N/A'}</div>
                      </td>
                      <td className="py-4 px-4">
                        <span className="px-2 py-1 bg-white/5 rounded text-xs">{pos.productType}</span>
                      </td>
                      <td className="py-4 px-4">
                        {(() => {
                          const isIndex = pos.instrument?.tradingSymbol?.startsWith('^');
                          let displaySide = pos.side;
                          if (isIndex) {
                            displaySide = pos.side === 'BUY' ? 'LONG (CALL)' : 'SHORT (PUT)';
                          }
                          return (
                            <span className={`px-2 py-1 rounded text-xs font-medium ${pos.side === 'BUY' ? 'bg-green-500/20 text-green-400' : 'bg-red-500/20 text-red-400'}`}>
                              {displaySide}
                            </span>
                          );
                        })()}
                      </td>
                      <td className="py-4 px-4 text-right font-medium text-white">{pos.quantity}</td>
                      <td className="py-4 px-4 text-right text-[var(--text-muted)]">₹{pos.avgEntryPrice?.toFixed(2) || '0.00'}</td>
                      <td className="py-4 px-4 text-right text-[var(--text-muted)]">₹{pos.currentPrice?.toFixed(2) || '---'}</td>
                      <td className={`py-4 px-4 text-right font-medium ${pnl >= 0 ? 'text-green-400' : 'text-red-400'}`}>
                        {pnl >= 0 ? '+' : ''}₹{pnl.toFixed(2)}
                      </td>
                    </tr>
                  );
                })
              )}
            </tbody>
          </table>
        </div>
      </Card>
    </div>
  );
};

export default Portfolio;
