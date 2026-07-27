import { useState, useEffect } from 'react';
import { Card } from '../components/ui/Card';
import { Button } from '../components/ui/Button';
import { Input } from '../components/ui/Input';
import { Cpu, Power, Trash2, Plus, Play, Pause, AlertCircle, RefreshCw, CheckCircle2, Activity } from 'lucide-react';
import api from '../lib/axios';
import BacktestWizard from '../components/BacktestWizard';

interface Strategy {
  id: string;
  name: string;
  description: string;
  status: 'DRAFT' | 'ACTIVE' | 'PAUSED';
  maxDailyLoss: number;
  maxExposure: number;
  aiThreshold: number;
  isPaperTrading: boolean;
  config: {
    tradingSymbol?: string;
    quantity?: number;
    brokerAccountId?: string;
    indicatorType?: string;
    shortPeriod?: number;
    longPeriod?: number;
    rsiPeriod?: number;
    rsiOversold?: number;
    rsiOverbought?: number;
  };
}

interface LogEntry {
  time: string;
  type: 'INFO' | 'SUCCESS' | 'WARNING' | 'ERROR';
  message: string;
}

export default function Strategies() {
  const [strategies, setStrategies] = useState<Strategy[]>([]);
  const [brokerAccounts, setBrokerAccounts] = useState<any[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [logs, setLogs] = useState<LogEntry[]>([]);

  // Form State
  const [name, setName] = useState('');
  const [description, setDescription] = useState('');
  const [isPaperTrading, setIsPaperTrading] = useState(true);
  const [indicatorType, setIndicatorType] = useState('SMA_CROSSOVER');
  const [quantity, setQuantity] = useState('1');
  const [selectedBroker, setSelectedBroker] = useState('');
  const [maxDailyLoss, setMaxDailyLoss] = useState('1000');
  const [aiThreshold, setAiThreshold] = useState('75');

  // Autocomplete Stock State
  const [symbol, setSymbol] = useState('');
  const [selectedInstrument, setSelectedInstrument] = useState<any>(null);
  const [searchResults, setSearchResults] = useState<any[]>([]);
  const [showDropdown, setShowDropdown] = useState(false);

  // Indicator Parameters
  const [shortPeriod, setShortPeriod] = useState('9');
  const [longPeriod, setLongPeriod] = useState('21');
  const [rsiPeriod, setRsiPeriod] = useState('14');
  const [rsiOversold, setRsiOversold] = useState('30');
  const [rsiOverbought, setRsiOverbought] = useState('70');

  const [formError, setFormError] = useState<string | null>(null);
  const [formSuccess, setFormSuccess] = useState<string | null>(null);

  // Backtest Wizard State
  const [backtestWizardOpen, setBacktestWizardOpen] = useState(false);
  const [selectedStrategyForBacktest, setSelectedStrategyForBacktest] = useState<{id: string, name: string} | null>(null);

  // Fetch Strategies & Brokers
  const fetchData = async () => {
    setIsLoading(true);
    try {
      const stratRes = await api.get('/strategies');
      setStrategies(stratRes.data);

      const brokerRes = await api.get('/brokers');
      setBrokerAccounts(brokerRes.data);
      if (brokerRes.data.length > 0) {
        setSelectedBroker(brokerRes.data[0].id);
      }
    } catch (err) {
      console.error('Failed to load data', err);
    } finally {
      setIsLoading(false);
    }
  };

  useEffect(() => {
    fetchData();

    // Populate initial dummy execution logs
    setLogs([]);
  }, []);

  // Symbol search autocomplete with debounce
  useEffect(() => {
    if (symbol.trim().length < 2) {
      setSearchResults([]);
      setShowDropdown(false);
      return;
    }

    if (selectedInstrument && selectedInstrument.tradingSymbol === symbol) return;

    const delayDebounce = setTimeout(async () => {
      try {
        const response = await api.get(`/instruments/search?query=${symbol}`);
        setSearchResults(response.data);
        setShowDropdown(true);
      } catch (err) {
        console.error('Failed to search instruments', err);
      }
    }, 300);

    return () => clearTimeout(delayDebounce);
  }, [symbol, selectedInstrument]);

  // Handle Strategy Creation
  const handleCreateStrategy = async (e: React.FormEvent) => {
    e.preventDefault();
    setFormError(null);
    setFormSuccess(null);

    if (indicatorType !== 'CRYPTO_AI' && !selectedInstrument) {
      setFormError('Please search and select a target stock symbol.');
      return;
    }

    if (indicatorType === 'CRYPTO_AI') {
      if (symbol !== 'BTCUSDT' && symbol !== 'ETHUSDT') {
        setFormError('Please enter BTCUSDT or ETHUSDT for Crypto AI.');
        return;
      }
      if (!isPaperTrading) {
        setFormError('Live trading is not supported for Crypto yet. Please use Paper Trading.');
        return;
      }
    }

    if (!isPaperTrading && !selectedBroker) {
      setFormError('Please link and select a broker account for live trading.');
      return;
    }

    // Build Strategy Payload
    const config: any = {
      tradingSymbol: indicatorType === 'CRYPTO_AI' ? symbol : selectedInstrument.tradingSymbol,
      quantity: parseInt(quantity),
      indicatorType: indicatorType,
    };

    if (!isPaperTrading) {
      config.brokerAccountId = selectedBroker;
    }

    if (indicatorType === 'SMA_CROSSOVER') {
      config.shortPeriod = parseInt(shortPeriod);
      config.longPeriod = parseInt(longPeriod);
    } else if (indicatorType === 'RSI_REVERSAL') {
      config.rsiPeriod = parseInt(rsiPeriod);
      config.rsiOversold = parseInt(rsiOversold);
      config.rsiOverbought = parseInt(rsiOverbought);
    }

    const payload = {
      name,
      description,
      status: 'DRAFT',
      maxDailyLoss: parseFloat(maxDailyLoss),
      maxExposure: parseFloat(quantity) * 2000, 
      aiThreshold: (indicatorType === 'AI_DRIVEN' || indicatorType === 'CRYPTO_AI') ? parseFloat(aiThreshold) : null,
      isPaperTrading,
      config,
    };

    try {
      const response = await api.post('/strategies', payload);
      setStrategies([...strategies, response.data]);
      
      // Reset form
      setName('');
      setDescription('');
      setSymbol('');
      setSelectedInstrument(null);
      setQuantity('1');
      setFormSuccess('Strategy created successfully as DRAFT.');

      // Add to logs
      setLogs((prev) => [
        {
          time: new Date().toLocaleTimeString(),
          type: 'INFO',
          message: `Created new strategy: "${payload.name}" in DRAFT mode.`,
        },
        ...prev,
      ]);
    } catch (err: any) {
      setFormError(err.response?.data?.message || 'Failed to create strategy.');
    }
  };

  // Toggle Strategy Status (ACTIVE / PAUSED)
  const handleToggleStatus = async (id: string) => {
    try {
      const response = await api.post(`/strategies/${id}/toggle`);
      setStrategies(strategies.map((s) => (s.id === id ? response.data : s)));
      
      const newStatus = response.data.status;
      setLogs((prev) => [
        {
          time: new Date().toLocaleTimeString(),
          type: newStatus === 'ACTIVE' ? 'SUCCESS' : 'WARNING',
          message: `Strategy "${response.data.name}" is now ${newStatus}.`,
        },
        ...prev,
      ]);
    } catch (err) {
      console.error('Failed to toggle status', err);
    }
  };

  // Delete Strategy
  const handleDeleteStrategy = async (id: string) => {
    try {
      await api.delete(`/strategies/${id}`);
      const deleted = strategies.find((s) => s.id === id);
      setStrategies(strategies.filter((s) => s.id !== id));

      if (deleted) {
        setLogs((prev) => [
          {
            time: new Date().toLocaleTimeString(),
            type: 'ERROR',
            message: `Deleted strategy "${deleted.name}".`,
          },
          ...prev,
        ]);
      }
    } catch (err) {
      console.error('Failed to delete strategy', err);
    }
  };

  return (
    <div className="space-y-8">
      <div>
        <h1 className="text-2xl font-bold text-white">Algo Strategies</h1>
        <p className="text-[var(--text-muted)]">Deploy, configure, and monitor automated trading algorithms.</p>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-3 gap-8">
        
        {/* Left/Middle Column: List of Strategies */}
        <div className="lg:col-span-2 space-y-6">
          <Card variant="glass" className="p-6">
            <div className="flex justify-between items-center mb-6">
              <h2 className="text-xl font-bold text-white">Deployed Strategies</h2>
              <button 
                onClick={fetchData} 
                className="p-2 bg-white/5 border border-white/5 rounded-xl hover:bg-white/10 text-gray-300 transition-colors"
                title="Refresh Table"
              >
                <RefreshCw size={16} />
              </button>
            </div>

            {isLoading ? (
              <div className="text-center py-12 text-[var(--text-muted)]">Loading strategies...</div>
            ) : strategies.length === 0 ? (
              <div className="text-center py-12 border border-dashed border-white/10 rounded-2xl bg-black/5">
                <Cpu size={48} className="mx-auto text-[var(--primary)] opacity-40 mb-4" />
                <h3 className="text-lg font-bold text-white">No Deployed Strategies</h3>
                <p className="text-sm text-[var(--text-muted)] mt-1.5">
                  Use the wizard on the right to configure and deploy your first algorithmic strategy.
                </p>
              </div>
            ) : (
              <div className="overflow-x-auto">
                <table className="w-full text-left border-collapse">
                  <thead>
                    <tr className="border-b border-white/5 text-xs text-[var(--text-muted)] uppercase tracking-wider">
                      <th className="py-4 px-4 font-semibold">Strategy</th>
                      <th className="py-4 px-4 font-semibold">Stock</th>
                      <th className="py-4 px-4 font-semibold">Rule / Setup</th>
                      <th className="py-4 px-4 font-semibold">Mode</th>
                      <th className="py-4 px-4 font-semibold text-center">Status</th>
                      <th className="py-4 px-4 font-semibold text-right">Actions</th>
                    </tr>
                  </thead>
                  <tbody>
                    {strategies.map((strategy) => {
                      const isSma = strategy.config.indicatorType === 'SMA_CROSSOVER';
                      const isRsi = strategy.config.indicatorType === 'RSI_REVERSAL';
                      
                      return (
                        <tr key={strategy.id} className="border-b border-white/5 text-sm hover:bg-white/2 transition-colors">
                          <td className="py-4 px-4">
                            <div className="font-bold text-white">{strategy.name}</div>
                            <div className="text-xs text-[var(--text-muted)] mt-0.5 max-w-[180px] truncate">{strategy.description || 'No description'}</div>
                          </td>
                          <td className="py-4 px-4 font-semibold font-mono text-gray-300">
                            {strategy.config.tradingSymbol || 'N/A'}
                          </td>
                          <td className="py-4 px-4">
                            {isSma && (
                              <span className="text-xs px-2 py-1 bg-blue-500/10 border border-blue-500/20 text-blue-400 rounded-lg font-mono font-semibold">
                                SMA {strategy.config.shortPeriod}/{strategy.config.longPeriod}
                              </span>
                            )}
                            {isRsi && (
                              <span className="text-xs px-2 py-1 bg-purple-500/10 border border-purple-500/20 text-purple-400 rounded-lg font-mono font-semibold">
                                RSI {strategy.config.rsiOversold}/{strategy.config.rsiOverbought}
                              </span>
                            )}
                            {strategy.config.indicatorType === 'AI_DRIVEN' && (
                              <span className="text-xs px-2 py-1 bg-emerald-500/10 border border-emerald-500/20 text-emerald-400 rounded-lg font-mono font-semibold">
                                AI &gt; {strategy.aiThreshold}%
                              </span>
                            )}
                            {strategy.config.indicatorType === 'CRYPTO_AI' && (
                              <span className="text-xs px-2 py-1 bg-yellow-500/10 border border-yellow-500/20 text-yellow-400 rounded-lg font-mono font-semibold">
                                CRYPTO AI &gt; {strategy.aiThreshold}%
                              </span>
                            )}
                          </td>
                          <td className="py-4 px-4">
                            <span className={`text-xs px-2.5 py-0.5 rounded-full font-bold tracking-wide ${strategy.isPaperTrading ? 'bg-amber-500/10 border border-amber-500/20 text-amber-400' : 'bg-rose-500/10 border border-rose-500/20 text-rose-400'}`}>
                              {strategy.isPaperTrading ? 'PAPER' : 'LIVE'}
                            </span>
                          </td>
                          <td className="py-4 px-4 text-center">
                            <button
                              onClick={() => handleToggleStatus(strategy.id)}
                              className={`mx-auto p-1.5 rounded-lg flex items-center gap-1.5 transition-all text-xs font-bold ${
                                strategy.status === 'ACTIVE' 
                                  ? 'bg-green-500/10 hover:bg-green-500/20 text-green-400 border border-green-500/20' 
                                  : 'bg-white/5 hover:bg-white/10 text-gray-400 border border-white/10'
                              }`}
                            >
                              {strategy.status === 'ACTIVE' ? <Play size={12} fill="currentColor" /> : <Pause size={12} fill="currentColor" />}
                              {strategy.status}
                            </button>
                          </td>
                          <td className="py-4 px-4 text-right flex justify-end gap-2">
                            <button
                              onClick={() => {
                                setSelectedStrategyForBacktest({ id: strategy.id, name: strategy.name });
                                setBacktestWizardOpen(true);
                              }}
                              className="p-1.5 text-blue-400 hover:bg-blue-500/10 rounded-lg transition-all"
                              title="Run Backtest"
                            >
                              <Activity size={16} />
                            </button>
                            <button
                              onClick={() => handleDeleteStrategy(strategy.id)}
                              className="p-1.5 text-gray-400 hover:text-red-400 hover:bg-red-500/10 rounded-lg transition-all"
                              title="Delete Strategy"
                            >
                              <Trash2 size={16} />
                            </button>
                          </td>
                        </tr>
                      );
                    })}
                  </tbody>
                </table>
              </div>
            )}
          </Card>

          {/* Engine Logs Dashboard Widget */}
          <Card variant="glass" className="p-6">
            <h3 className="text-lg font-bold text-white mb-4 flex items-center gap-2">
              <Power size={18} className="text-green-400" />
              Live Execution Feed
            </h3>
            <div className="space-y-3 max-h-48 overflow-y-auto font-mono text-xs border border-white/5 rounded-xl p-4 bg-black/20">
              {logs.map((log, idx) => (
                <div key={idx} className="flex gap-2 items-start leading-relaxed">
                  <span className="text-[var(--text-muted)] shrink-0">[{log.time}]</span>
                  <span className={`shrink-0 font-bold ${
                    log.type === 'SUCCESS' ? 'text-green-400' :
                    log.type === 'WARNING' ? 'text-amber-400' :
                    log.type === 'ERROR' ? 'text-red-400' : 'text-blue-400'
                  }`}>[{log.type}]</span>
                  <span className="text-gray-300">{log.message}</span>
                </div>
              ))}
            </div>
          </Card>
        </div>

        {/* Right Column: Create Strategy Wizard */}
        <div className="space-y-6">
          <Card variant="glass" className="p-6">
            <h2 className="text-xl font-bold text-white mb-6 flex items-center gap-2">
              <Plus size={20} className="text-[var(--primary)]" />
              Configure Algo
            </h2>

            {formError && (
              <div className="p-4 bg-red-500/10 text-red-400 border border-red-500/20 rounded-xl mb-5 text-sm flex gap-2 items-center">
                <AlertCircle size={16} className="shrink-0" />
                {formError}
              </div>
            )}

            {formSuccess && (
              <div className="p-4 bg-green-500/10 text-green-400 border border-green-500/20 rounded-xl mb-5 text-sm flex gap-2 items-center">
                <CheckCircle2 size={16} className="shrink-0" />
                {formSuccess}
              </div>
            )}

            <form onSubmit={handleCreateStrategy} className="space-y-5">
              <Input
                label="Strategy Name"
                placeholder="e.g. Reliance Golden Cross"
                value={name}
                onChange={(e) => setName(e.target.value)}
                required
              />

              <Input
                label="Description"
                placeholder="Brief summary of entry/exit rules"
                value={description}
                onChange={(e) => setDescription(e.target.value)}
              />

              {/* Mode Selection */}
              <div>
                <label className="block text-sm font-semibold text-[var(--text-muted)] mb-2">Execution Mode</label>
                <div className="grid grid-cols-2 gap-2 p-1 bg-[var(--bg-surface)] rounded-xl border border-[var(--border)]">
                  <button
                    type="button"
                    onClick={() => setIsPaperTrading(true)}
                    className={`py-2 rounded-lg font-bold text-xs transition-all ${isPaperTrading ? 'bg-amber-500/15 text-amber-400 border border-amber-500/20 shadow-sm' : 'text-gray-400 hover:text-white'}`}
                  >
                    PAPER TRADING
                  </button>
                  <button
                    type="button"
                    onClick={() => setIsPaperTrading(false)}
                    className={`py-2 rounded-lg font-bold text-xs transition-all ${!isPaperTrading ? 'bg-rose-500/15 text-rose-400 border border-rose-500/20 shadow-sm' : 'text-gray-400 hover:text-white'}`}
                  >
                    LIVE TRADING
                  </button>
                </div>
              </div>

              {/* Autocomplete Stock Search */}
              <div className="relative">
                <Input
                  label="Target Stock Symbol"
                  placeholder="Search ^NSEI, RELIANCE..."
                  value={symbol}
                  onChange={(e) => {
                    setSymbol(e.target.value);
                    setSelectedInstrument(null);
                  }}
                  required
                  onFocus={() => { if (searchResults.length > 0) setShowDropdown(true); }}
                  onBlur={() => setTimeout(() => setShowDropdown(false), 200)}
                />
                {showDropdown && searchResults.length > 0 && (
                  <div className="absolute z-50 w-full mt-1 bg-[var(--bg-surface)] border border-[var(--border)] rounded-lg shadow-2xl max-h-48 overflow-y-auto">
                    {searchResults.map((inst) => (
                      <button
                        key={inst.id}
                        type="button"
                        className="w-full text-left px-4 py-2.5 hover:bg-white/5 text-xs text-white border-b border-white/5 last:border-b-0 flex justify-between items-center"
                        onClick={() => {
                          setSymbol(inst.tradingSymbol);
                          setSelectedInstrument(inst);
                          setShowDropdown(false);
                        }}
                      >
                        <span className="font-bold">{inst.tradingSymbol}</span>
                        <span className="text-[var(--text-muted)] text-[10px]">{inst.exchange}</span>
                      </button>
                    ))}
                  </div>
                )}
              </div>

              {/* Indicator Dropdown */}
              <div>
                <label className="block text-sm font-semibold text-[var(--text-muted)] mb-1.5">Technical Setup</label>
                <select
                  className="w-full bg-[var(--bg-surface)] border border-[var(--border)] rounded-lg px-4 py-2.5 text-sm text-white focus:outline-none focus:ring-2 focus:ring-[var(--primary)]/50 cursor-pointer"
                  value={indicatorType}
                  onChange={(e) => setIndicatorType(e.target.value)}
                >
                  <option value="SMA_CROSSOVER" className="bg-[var(--bg-color)]">SMA Crossover</option>
                  <option value="RSI_REVERSAL" className="bg-[var(--bg-color)]">RSI Reversal</option>
                  <option value="AI_DRIVEN" className="bg-[var(--bg-color)]">AI Confidence Model (Indian)</option>
                  <option value="CRYPTO_AI" className="bg-[var(--bg-color)]">Crypto AI Model (Binance)</option>
                </select>
              </div>

              {/* Dynamic Parameter Settings */}
              {indicatorType === 'SMA_CROSSOVER' && (
                <div className="grid grid-cols-2 gap-4">
                  <Input
                    label="Short SMA"
                    type="number"
                    min="3"
                    value={shortPeriod}
                    onChange={(e) => setShortPeriod(e.target.value)}
                    required
                  />
                  <Input
                    label="Long SMA"
                    type="number"
                    min="10"
                    value={longPeriod}
                    onChange={(e) => setLongPeriod(e.target.value)}
                    required
                  />
                </div>
              )}

              {indicatorType === 'RSI_REVERSAL' && (
                <div className="grid grid-cols-3 gap-2">
                  <Input
                    label="Period"
                    type="number"
                    min="5"
                    value={rsiPeriod}
                    onChange={(e) => setRsiPeriod(e.target.value)}
                    required
                  />
                  <Input
                    label="Oversold"
                    type="number"
                    min="10"
                    value={rsiOversold}
                    onChange={(e) => setRsiOversold(e.target.value)}
                    required
                  />
                  <Input
                    label="Overbought"
                    type="number"
                    min="50"
                    value={rsiOverbought}
                    onChange={(e) => setRsiOverbought(e.target.value)}
                    required
                  />
                </div>
              )}

              {(indicatorType === 'AI_DRIVEN' || indicatorType === 'CRYPTO_AI') && (
                <Input
                  label="Min Confidence Score (%)"
                  type="number"
                  min="50"
                  max="105"
                  value={aiThreshold}
                  onChange={(e) => setAiThreshold(e.target.value)}
                  required
                />
              )}

              <div className="grid grid-cols-2 gap-4">
                <Input
                  label="Order Quantity"
                  type="number"
                  min="1"
                  value={quantity}
                  onChange={(e) => setQuantity(e.target.value)}
                  required
                />
                <Input
                  label="Max Daily Loss (₹)"
                  type="number"
                  min="1"
                  value={maxDailyLoss}
                  onChange={(e) => setMaxDailyLoss(e.target.value)}
                  required
                />
              </div>

              {/* Broker Select if Live */}
              {!isPaperTrading && (
                <div>
                  <label className="block text-sm font-semibold text-[var(--text-muted)] mb-1.5">Broker Account</label>
                  <select
                    className="w-full bg-[var(--bg-surface)] border border-[var(--border)] rounded-lg px-4 py-2.5 text-sm text-white focus:outline-none focus:ring-2 focus:ring-[var(--primary)]/50 cursor-pointer"
                    value={selectedBroker}
                    onChange={(e) => setSelectedBroker(e.target.value)}
                    required
                  >
                    {brokerAccounts.map((b) => (
                      <option key={b.id} value={b.id} className="bg-[var(--bg-color)]">
                        {b.brokerType} ({b.clientId})
                      </option>
                    ))}
                  </select>
                </div>
              )}

              <Button type="submit" fullWidth className="bg-gradient-to-r from-[var(--primary)] to-[var(--secondary)] border-none mt-2 shadow-lg shadow-[var(--primary)]/20">
                DEPLOY STRATEGY
              </Button>
            </form>
          </Card>
        </div>

      </div>

      {selectedStrategyForBacktest && (
        <BacktestWizard 
          open={backtestWizardOpen} 
          onClose={() => setBacktestWizardOpen(false)} 
          strategyId={selectedStrategyForBacktest.id} 
          strategyName={selectedStrategyForBacktest.name} 
        />
      )}
    </div>
  );
}
