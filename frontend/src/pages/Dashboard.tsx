import { useState, useEffect } from 'react';
import { 
  TrendingUp, Wallet, Activity, ArrowUpRight, 
  ArrowDownRight, LineChart, Eye, RefreshCw, ChevronRight,
  ArrowRight
} from 'lucide-react';
import { Card } from '../components/ui/Card';
import { Button } from '../components/ui/Button';
import api from '../lib/axios';

// Mock Indices Data
const marketIndices = [
  { name: 'NIFTY 50', value: '23,458.90', change: '+142.15', pct: '+0.61%', isPositive: true },
  { name: 'NIFTY BANK', value: '49,842.30', change: '-198.50', pct: '-0.40%', isPositive: false },
  { name: 'SENSEX', value: '77,156.40', change: '+384.20', pct: '+0.50%', isPositive: true },
  { name: 'NIFTY IT', value: '38,212.10', change: '+412.30', pct: '+1.09%', isPositive: true },
];

// Mock Watchlist
const watchlist = [
  { symbol: 'RELIANCE', name: 'Reliance Industries', ltp: '2,488.50', change: '+32.10', pct: '+1.31%', isPositive: true },
  { symbol: 'HDFCBANK', name: 'HDFC Bank Ltd', ltp: '1,422.30', change: '-8.40', pct: '-0.59%', isPositive: false },
  { symbol: 'SBIN', name: 'State Bank of India', ltp: '782.10', change: '+12.45', pct: '+1.62%', isPositive: true },
  { symbol: 'TCS', name: 'Tata Consultancy Services', ltp: '3,845.20', change: '+42.50', pct: '+1.12%', isPositive: true },
  { symbol: 'INFY', name: 'Infosys Ltd', ltp: '1,498.40', change: '-14.30', pct: '-0.95%', isPositive: false },
];

const Dashboard = () => {
  const [isRefreshing, setIsRefreshing] = useState(false);
  const [stats, setStats] = useState<{accountBalance: number, realizedPnl: number, unrealizedPnl: number} | null>(null);
  const [recentOrders, setRecentOrders] = useState<any[]>([]);

  const fetchStats = async () => {
    try {
      const res = await api.get('/portfolio/indian-stats');
      setStats(res.data);
      
      const ordersRes = await api.get('/orders');
      if (ordersRes.data && Array.isArray(ordersRes.data)) {
        // Sort by newest first
        const sorted = ordersRes.data.sort((a: any, b: any) => new Date(b.placedAt).getTime() - new Date(a.placedAt).getTime());
        const mappedOrders = sorted.slice(0, 10).map((o: any) => ({
          id: o.id,
          symbol: o.instrument?.tradingSymbol || 'UNKNOWN',
          type: o.side,
          price: o.price ? `₹${o.price.toFixed(2)}` : 'MKT',
          qty: o.quantity || 1,
          status: o.status,
          time: new Date(o.placedAt).toLocaleTimeString([], {hour: '2-digit', minute:'2-digit', second:'2-digit'})
        }));
        setRecentOrders(mappedOrders);
      }
    } catch (e) {
      console.error(e);
    }
  };

  useEffect(() => {
    fetchStats();
  }, []);

  const triggerRefresh = () => {
    setIsRefreshing(true);
    fetchStats();
    setTimeout(() => setIsRefreshing(false), 800);
  };

  return (
    <div className="space-y-8">
      {/* Top Header Section */}
      <div className="flex justify-between items-center">
        <div>
          <h1 className="text-3xl font-extrabold text-white tracking-tight">Market Dashboard</h1>
          <p className="text-[var(--text-muted)] text-base mt-1">Real-time status of indices, watchlist, and linked brokers.</p>
        </div>
        <button 
          onClick={triggerRefresh}
          className={`p-3 rounded-xl bg-[var(--bg-surface)] border border-[var(--border)] text-[var(--text-muted)] hover:text-white transition-all shadow-md ${isRefreshing ? 'animate-spin' : ''}`}
        >
          <RefreshCw size={20} />
        </button>
      </div>

      {/* Live Market Indices Bar */}
      <div className="grid grid-cols-2 md:grid-cols-4 gap-6">
        {marketIndices.map((index) => (
          <Card key={index.name} className="p-6 border border-[var(--border)]/60 bg-gradient-to-br from-[var(--bg-surface)] to-[var(--bg-surface)]/80 relative overflow-hidden group shadow-lg">
            <div className="flex justify-between items-center mb-2">
              <span className="text-sm font-semibold text-[var(--text-muted)] tracking-wider">{index.name}</span>
              <span className={`text-xs font-bold px-2 py-0.5 rounded ${index.isPositive ? 'bg-green-500/10 text-green-400' : 'bg-red-500/10 text-red-400'}`}>
                {index.pct}
              </span>
            </div>
            <div className="flex items-baseline gap-2.5">
              <span className="text-2xl font-bold text-white tracking-tight">{index.value}</span>
              <span className={`text-sm font-semibold ${index.isPositive ? 'text-green-400' : 'text-red-400'}`}>
                {index.change}
              </span>
            </div>
            <div className={`absolute bottom-0 left-0 right-0 h-0.5 bg-gradient-to-r ${index.isPositive ? 'from-green-500/30' : 'from-red-500/30'} to-transparent`} />
          </Card>
        ))}
      </div>

      {/* Main Grid: Left (Broker Summary & Charts) vs Right (Watchlist) */}
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-8">
        
        {/* Left Side: Summary & Performance Chart */}
        <div className="lg:col-span-2 space-y-8">
          
          {/* Professional Funds & Margin Widget */}
          <Card className="p-8 bg-gradient-to-br from-[var(--bg-surface)] to-[var(--bg-surface)]/90 border border-[var(--border)] shadow-xl">
            <div className="flex justify-between items-center mb-6">
              <h2 className="text-lg font-semibold text-white flex items-center gap-2.5">
                <Wallet size={20} className="text-[var(--primary)]" />
                Capital & Margin Summary
              </h2>
              <span className="px-3 py-1 bg-green-500/15 text-green-400 text-xs font-bold rounded-full border border-green-500/20">LIVE FUNDS</span>
            </div>

            <div className="grid grid-cols-2 md:grid-cols-4 gap-8">
              <div>
                <p className="text-sm text-[var(--text-muted)] mb-1.5">Total Net Worth</p>
                <p className="text-2xl font-extrabold text-white tracking-tight">
                  ₹{stats ? (stats.accountBalance + stats.unrealizedPnl).toLocaleString('en-IN', {minimumFractionDigits:2, maximumFractionDigits:2}) : '1,00,000.00'}
                </p>
              </div>
              <div className="border-l border-[var(--border)]/50 pl-6 md:pl-8">
                <p className="text-sm text-[var(--text-muted)] mb-1.5">Available Margin</p>
                <p className="text-2xl font-extrabold text-white tracking-tight">
                  ₹{stats ? stats.accountBalance.toLocaleString('en-IN', {minimumFractionDigits:2, maximumFractionDigits:2}) : '1,00,000.00'}
                </p>
              </div>
              <div className="border-l border-[var(--border)]/50 pl-6 md:pl-8">
                <p className="text-sm text-[var(--text-muted)] mb-1.5">Realized P&L</p>
                <p className={`text-2xl font-extrabold tracking-tight ${stats && stats.realizedPnl >= 0 ? 'text-green-400' : 'text-red-400'}`}>
                  ₹{stats ? stats.realizedPnl.toLocaleString('en-IN', {minimumFractionDigits:2, maximumFractionDigits:2}) : '0.00'}
                </p>
              </div>
              <div className="border-l border-[var(--border)]/50 pl-6 md:pl-8">
                <p className="text-sm text-[var(--text-muted)] mb-1.5">MTM P&L</p>
                <p className={`text-2xl font-extrabold flex items-center gap-1.5 tracking-tight ${stats && stats.unrealizedPnl >= 0 ? 'text-green-400' : 'text-red-400'}`}>
                  {stats && stats.unrealizedPnl >= 0 ? '+' : ''}₹{stats ? stats.unrealizedPnl.toLocaleString('en-IN', {minimumFractionDigits:2, maximumFractionDigits:2}) : '0.00'}
                  <TrendingUp size={20} />
                </p>
              </div>
            </div>
          </Card>

          {/* Portfolio Performance Chart Card */}
          <Card className="p-8 min-h-[420px] flex flex-col border border-[var(--border)] shadow-xl">
            <div className="flex justify-between items-center mb-8">
              <div>
                <h2 className="text-lg font-semibold text-white">Portfolio Curve</h2>
                <p className="text-sm text-[var(--text-muted)] mt-1">Historical growth curve of consolidated linked broker accounts.</p>
              </div>
              <div className="flex gap-2 bg-[var(--bg-surface-hover)] p-1.5 rounded-xl border border-[var(--border)]">
                {['1D', '1W', '1M', '3M', '1Y'].map((tf) => (
                  <button 
                    key={tf} 
                    className={`px-3 py-1.5 text-sm rounded-lg font-semibold transition-colors ${tf === '1M' ? 'bg-[var(--bg-color)] text-white shadow-sm' : 'text-[var(--text-muted)] hover:text-white'}`}
                  >
                    {tf}
                  </button>
                ))}
              </div>
            </div>
            
            <div className="flex-1 flex items-center justify-center border border-dashed border-[var(--border)] rounded-xl relative overflow-hidden group min-h-[250px] bg-black/10">
              <div className="absolute inset-0 bg-gradient-to-b from-[var(--primary)]/5 to-transparent pointer-events-none"></div>
              <div className="text-center p-8">
                <LineChart size={48} className="mx-auto mb-4 text-[var(--primary)] opacity-50 group-hover:scale-110 transition-transform duration-300" />
                <p className="text-base font-semibold text-white">Equity Curve Visualization</p>
                <p className="text-sm text-[var(--text-muted)] mt-2">Interactive performance charts will unlock in Phase 4</p>
              </div>
            </div>
          </Card>

          {/* Recent Orders Widget */}
          <Card className="p-8 border border-[var(--border)] shadow-xl">
            <div className="flex justify-between items-center mb-6">
              <h2 className="text-lg font-semibold text-white flex items-center gap-2.5">
                <Activity size={20} className="text-[var(--primary)]" />
                Recent Orders
              </h2>
              <a href="/history" className="text-sm text-[var(--primary)] hover:underline flex items-center gap-1 font-semibold">
                View History <ChevronRight size={16} />
              </a>
            </div>

            <div className="overflow-x-auto">
              <table className="w-full text-left border-collapse">
                <thead>
                  <tr className="border-b border-white/10 text-sm text-[var(--text-muted)] uppercase tracking-wider font-semibold">
                    <th className="pb-4 font-semibold">Time</th>
                    <th className="pb-4 font-semibold">Symbol</th>
                    <th className="pb-4 font-semibold">Side</th>
                    <th className="pb-4 font-semibold text-right">Price</th>
                    <th className="pb-4 font-semibold text-right">Qty</th>
                    <th className="pb-4 font-semibold text-center">Status</th>
                  </tr>
                </thead>
                <tbody className="text-sm">
                  {recentOrders.length === 0 ? (
                    <tr>
                      <td colSpan={6} className="py-8 text-center text-[var(--text-muted)] italic">No recent orders found.</td>
                    </tr>
                  ) : (
                    recentOrders.map((order) => (
                      <tr key={order.id} className="border-b border-white/5 last:border-b-0 text-sm">
                        <td className="py-4 text-[var(--text-muted)]">{order.time}</td>
                        <td className="py-4 font-bold text-white">{order.symbol}</td>
                        <td className="py-4">
                          <span className={`px-2 py-0.5 rounded text-xs font-bold ${order.type === 'BUY' ? 'bg-green-500/10 text-green-500' : 'bg-red-500/10 text-red-500'}`}>
                            {order.type}
                          </span>
                        </td>
                        <td className="py-4 text-right font-mono font-semibold text-white">{order.price}</td>
                        <td className="py-4 text-right font-mono text-gray-300">{order.qty}</td>
                        <td className="py-4 text-center">
                          <span className={`px-3 py-1 rounded-md text-[10px] font-bold tracking-wider ${
                            order.status === 'COMPLETE' ? 'bg-green-500/10 text-green-400 border border-green-500/20' : 
                            order.status === 'PENDING' ? 'bg-blue-500/10 text-blue-400 border border-blue-500/20' : 
                            'bg-red-500/10 text-red-400 border border-red-500/20'
                          }`}>
                            {order.status}
                          </span>
                        </td>
                      </tr>
                    ))
                  )}
                </tbody>
              </table>
            </div>
          </Card>
        </div>

        {/* Right Side: Watchlist & Actions */}
        <div className="space-y-8">
          
          {/* Premium Market Watchlist */}
          <Card className="p-8 border border-[var(--border)] bg-gradient-to-br from-[var(--bg-surface)] to-[var(--bg-surface)]/95 shadow-xl">
            <div className="flex justify-between items-center mb-6">
              <h2 className="text-lg font-semibold text-white flex items-center gap-2.5">
                <Eye size={20} className="text-[var(--primary)]" />
                Live Watchlist
              </h2>
              <span className="text-xs px-2.5 py-1 bg-white/5 border border-white/5 rounded-lg text-[var(--text-muted)] font-mono font-semibold">NSE</span>
            </div>

            <div className="space-y-4">
              {watchlist.map((stock) => (
                <div key={stock.symbol} className="p-4 bg-[var(--bg-surface-hover)] rounded-xl border border-white/5 hover:border-[var(--primary)]/20 transition-all flex items-center justify-between shadow-sm">
                  <div>
                    <h4 className="font-extrabold text-white text-sm tracking-tight">{stock.symbol}</h4>
                    <p className="text-xs text-[var(--text-muted)] mt-1">{stock.name}</p>
                  </div>
                  <div className="text-right">
                    <p className="font-mono font-bold text-white text-sm">₹{stock.ltp}</p>
                    <span className={`text-xs font-bold flex items-center justify-end gap-0.5 mt-1 ${stock.isPositive ? 'text-green-400' : 'text-red-400'}`}>
                      {stock.isPositive ? <ArrowUpRight size={12} /> : <ArrowDownRight size={12} />}
                      {stock.pct}
                    </span>
                  </div>
                </div>
              ))}
            </div>
          </Card>

          {/* Quick Action Navigation */}
          <Card className="p-8 border border-[var(--border)] bg-gradient-to-br from-[var(--bg-surface)] to-[var(--bg-surface)]/80 shadow-xl">
            <h3 className="text-base font-semibold text-white mb-4">Quick Links</h3>
            <div className="space-y-4">
              <a href="/trading" className="w-full block">
                <Button size="lg" className="w-full text-sm font-bold flex justify-between items-center py-3.5 rounded-xl">
                  <span>Open Order Ticket</span>
                  <ArrowRight size={16} />
                </Button>
              </a>
            </div>
          </Card>
        </div>

      </div>
    </div>
  );
};

export default Dashboard;
