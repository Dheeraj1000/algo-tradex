import { useState, useEffect } from 'react';
import { Card } from '../components/ui/Card';
import { Button } from '../components/ui/Button';
import { Input } from '../components/ui/Input';
import { RealtimeChart } from '../components/RealtimeChart';
import { LineChart } from 'lucide-react';
import { Client } from '@stomp/stompjs';
import api from '../lib/axios';

const Trading = () => {
  const [brokerAccounts, setBrokerAccounts] = useState<any[]>([]);
  const [selectedBroker, setSelectedBroker] = useState('');
  
  // Order Form State
  const [symbol, setSymbol] = useState('');
  const [side, setSide] = useState<'BUY'|'SELL'>('BUY');
  const [productType, setProductType] = useState('INTRADAY'); // MIS/CNC mapping
  const [orderType, setOrderType] = useState('MARKET');
  const [quantity, setQuantity] = useState('1');
  const [price, setPrice] = useState('');
  
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [message, setMessage] = useState<{type: 'success'|'error', text: string} | null>(null);

  // Search autocomplete state
  const [selectedInstrument, setSelectedInstrument] = useState<any>(null);
  const [searchResults, setSearchResults] = useState<any[]>([]);
  const [showDropdown, setShowDropdown] = useState(false);

  // Search API call with debounce
  useEffect(() => {
    if (symbol.trim().length < 2) {
      setSearchResults([]);
      setShowDropdown(false);
      return;
    }

    // Check if the current value matches the selected instrument's symbol to avoid searching again
    if (selectedInstrument && selectedInstrument.tradingSymbol === symbol) {
      return;
    }

    const delayDebounce = setTimeout(async () => {
      try {
        const response = await api.get(`/instruments/search?query=${symbol}`);
        setSearchResults(response.data);
        setShowDropdown(true);
      } catch (err) {
        console.error("Failed to search instruments", err);
      }
    }, 300);

    return () => clearTimeout(delayDebounce);
  }, [symbol, selectedInstrument]);

  const [ltp, setLtp] = useState<number | null>(null);
  const [prevLtp, setPrevLtp] = useState<number | null>(null);
  const [ltpChangeClass, setLtpChangeClass] = useState('');

  // WebSocket connection for flashing LTP badge in order ticket
  useEffect(() => {
    if (!selectedInstrument) {
      setLtp(null);
      setPrevLtp(null);
      return;
    }

    const apiBase = api.defaults.baseURL || 'http://localhost:8080/api';
    const backendHost = apiBase.replace('/api', '');
    
    const stompClient = new Client({
      brokerURL: `${backendHost.replace(/^http/, 'ws')}/ws`,
      reconnectDelay: 5000,
    });

    stompClient.onConnect = () => {
      stompClient.subscribe(`/topic/ticks/${selectedInstrument.tradingSymbol}`, (message) => {
        const tick = JSON.parse(message.body);
        const tickPrice = parseFloat(tick.lastPrice);
        
        setLtp((currentLtp) => {
          if (currentLtp !== null) {
            setPrevLtp(currentLtp);
            if (tickPrice > currentLtp) {
              setLtpChangeClass('text-green-400 bg-green-500/10 border border-green-500/20');
            } else if (tickPrice < currentLtp) {
              setLtpChangeClass('text-red-400 bg-red-500/10 border border-red-500/20');
            } else {
              setLtpChangeClass('text-gray-300 bg-white/5 border border-white/5');
            }
          } else {
            setLtpChangeClass('text-gray-300 bg-white/5 border border-white/5');
          }
          return tickPrice;
        });
      });
    };

    stompClient.activate();

    return () => {
      stompClient.deactivate();
    };
  }, [selectedInstrument]);

  useEffect(() => {
    const fetchBrokers = async () => {
      try {
        const response = await api.get('/brokers');
        setBrokerAccounts(response.data);
        if (response.data.length > 0) {
          setSelectedBroker(response.data[0].id);
        }
      } catch (err) {
        console.error("Failed to fetch brokers", err);
      }
    };
    fetchBrokers();
  }, []);

  const handlePlaceOrder = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!selectedBroker) {
      setMessage({ type: 'error', text: 'Please link and select a broker account first.' });
      return;
    }
    if (!selectedInstrument) {
      setMessage({ type: 'error', text: 'Please search and select a stock from the dropdown suggestions.' });
      return;
    }
    
    setIsSubmitting(true);
    setMessage(null);

    const broker = brokerAccounts.find(b => b.id === selectedBroker);
    const tradingMode = broker?.brokerType === 'MOCK' ? 'PAPER' : 'LIVE';

    const payload = {
      brokerAccount: { id: selectedBroker },
      instrument: { id: selectedInstrument.id },
      tradingMode: tradingMode,
      side: side,
      orderType: orderType,
      productType: productType,
      quantity: parseInt(quantity, 10),
      price: price ? parseFloat(price) : null,
      status: 'PENDING'
    };

    try {
      const response = await api.post('/orders', payload);
      setMessage({ type: 'success', text: `Order placed successfully! Order ID: ${response.data.brokerOrderId || response.data.id}` });
    } catch (err: any) {
      setMessage({ type: 'error', text: err.response?.data?.message || 'Failed to place order.' });
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-2xl font-bold text-white">Manual Trading</h1>
        <p className="text-[var(--text-muted)]">Place manual orders into the Indian equity & derivatives market.</p>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        <Card variant="glass" className="p-6 h-full flex flex-col">
          <div className="flex justify-between items-center mb-6">
            <h2 className="text-xl font-semibold text-white">Order Ticket</h2>
            {selectedInstrument && ltp !== null && (
              <div className={`px-3 py-1 rounded-lg text-xs font-bold font-mono transition-all duration-300 ${ltpChangeClass}`}>
                LTP: ₹{ltp.toFixed(2)}
              </div>
            )}
          </div>
          
          {message && (
            <div className={`p-4 rounded-lg mb-6 text-sm ${message.type === 'success' ? 'bg-green-500/10 text-green-400 border border-green-500/20' : 'bg-red-500/10 text-red-400 border border-red-500/20'}`}>
              {message.text}
            </div>
          )}

          <form onSubmit={handlePlaceOrder} className="space-y-5">
            <div>
              <label className="block text-sm font-medium text-[var(--text-muted)] mb-1.5">Broker Account</label>
              <div className="relative">
                <select 
                  className="w-full appearance-none bg-[var(--bg-surface)] border border-[var(--border)] rounded-lg px-4 py-2.5 text-white focus:outline-none focus:ring-2 focus:ring-[var(--primary)]/50 focus:border-[var(--primary)] transition-all cursor-pointer"
                  value={selectedBroker}
                  onChange={(e) => setSelectedBroker(e.target.value)}
                  required
                >
                  <option value="" disabled className="bg-[var(--bg-color)] text-gray-500">Select Broker...</option>
                  {brokerAccounts.map(broker => (
                    <option key={broker.id} value={broker.id} className="bg-[var(--bg-color)] text-white">{broker.brokerType} ({broker.clientId})</option>
                  ))}
                </select>
                <div className="pointer-events-none absolute inset-y-0 right-0 flex items-center px-4 text-gray-400">
                  <svg className="h-4 w-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M19 9l-7 7-7-7"></path></svg>
                </div>
              </div>
            </div>

            <div className="relative">
              <Input 
                label="Trading Symbol (NSE/BSE)" 
                placeholder="Type to search e.g. ^NSEI, RELIANCE..."
                value={symbol}
                onChange={(e) => {
                  setSymbol(e.target.value);
                  setSelectedInstrument(null); // Clear selected if they edit
                }}
                required
                onFocus={() => {
                  if (searchResults.length > 0) setShowDropdown(true);
                }}
                onBlur={() => {
                  // Delay closing to allow clicking a suggestion
                  setTimeout(() => setShowDropdown(false), 200);
                }}
              />
              {showDropdown && searchResults.length > 0 && (
                <div className="absolute z-50 w-full mt-1 bg-[var(--bg-surface)] border border-[var(--border)] rounded-lg shadow-2xl max-h-60 overflow-y-auto">
                  {searchResults.map((instrument) => (
                    <button
                      key={instrument.id}
                      type="button"
                      className="w-full text-left px-4 py-3 hover:bg-white/5 transition-colors border-b border-white/5 last:border-b-0 flex justify-between items-center"
                      onClick={() => {
                        setSymbol(instrument.tradingSymbol);
                        setSelectedInstrument(instrument);
                        setShowDropdown(false);
                      }}
                    >
                      <div>
                        <div className="font-bold text-white text-sm">{instrument.tradingSymbol}</div>
                        <div className="text-xs text-[var(--text-muted)] mt-0.5">{instrument.name}</div>
                      </div>
                      <span className="text-xs px-2 py-0.5 bg-white/10 rounded font-semibold text-gray-300">
                        {instrument.exchange}
                      </span>
                    </button>
                  ))}
                </div>
              )}
            </div>

            <div>
              <label className="block text-sm font-medium text-[var(--text-muted)] mb-1.5">Transaction Type</label>
              <div className="flex gap-2 p-1 bg-[var(--bg-surface)] rounded-xl border border-[var(--border)] shadow-inner">
                <button 
                  type="button"
                  onClick={() => setSide('BUY')}
                  className={`flex-1 py-2.5 rounded-lg font-bold text-sm tracking-wide transition-all duration-300 ${side === 'BUY' ? 'bg-gradient-to-b from-green-500 to-green-600 text-white shadow-[0_2px_10px_rgba(34,197,94,0.3)]' : 'bg-green-500/10 text-green-500 hover:bg-green-500/20'}`}
                >
                  BUY
                </button>
                <button 
                  type="button"
                  onClick={() => setSide('SELL')}
                  className={`flex-1 py-2.5 rounded-lg font-bold text-sm tracking-wide transition-all duration-300 ${side === 'SELL' ? 'bg-gradient-to-b from-red-500 to-red-600 text-white shadow-[0_2px_10px_rgba(239,68,68,0.3)]' : 'bg-red-500/10 text-red-500 hover:bg-red-500/20'}`}
                >
                  SELL
                </button>
              </div>
            </div>

            <div className="grid grid-cols-1 sm:grid-cols-2 gap-5">
              <div>
                <label className="block text-sm font-medium text-[var(--text-muted)] mb-1.5">Product Type</label>
                <div className="relative">
                  <select 
                    className="w-full appearance-none bg-[var(--bg-surface)] border border-[var(--border)] rounded-lg px-4 py-2.5 text-white focus:outline-none focus:ring-2 focus:ring-[var(--primary)]/50 focus:border-[var(--primary)] transition-all cursor-pointer"
                    value={productType}
                    onChange={(e) => setProductType(e.target.value)}
                  >
                    <option value="INTRADAY" className="bg-[var(--bg-color)]">MIS (Intraday)</option>
                    <option value="DELIVERY" className="bg-[var(--bg-color)]">CNC (Delivery)</option>
                    <option value="CARRYFORWARD" className="bg-[var(--bg-color)]">NRML (Carry Forward)</option>
                  </select>
                  <div className="pointer-events-none absolute inset-y-0 right-0 flex items-center px-4 text-gray-400">
                    <svg className="h-4 w-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M19 9l-7 7-7-7"></path></svg>
                  </div>
                </div>
              </div>
              <div>
                <label className="block text-sm font-medium text-[var(--text-muted)] mb-1.5">Order Type</label>
                <div className="relative">
                  <select 
                    className="w-full appearance-none bg-[var(--bg-surface)] border border-[var(--border)] rounded-lg px-4 py-2.5 text-white focus:outline-none focus:ring-2 focus:ring-[var(--primary)]/50 focus:border-[var(--primary)] transition-all cursor-pointer"
                    value={orderType}
                    onChange={(e) => setOrderType(e.target.value)}
                  >
                    <option value="MARKET" className="bg-[var(--bg-color)]">Market</option>
                    <option value="LIMIT" className="bg-[var(--bg-color)]">Limit</option>
                  </select>
                  <div className="pointer-events-none absolute inset-y-0 right-0 flex items-center px-4 text-gray-400">
                    <svg className="h-4 w-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M19 9l-7 7-7-7"></path></svg>
                  </div>
                </div>
              </div>
            </div>

            <div className="grid grid-cols-1 sm:grid-cols-2 gap-5">
              <Input 
                label="Quantity" 
                type="number"
                min="1"
                value={quantity}
                onChange={(e) => setQuantity(e.target.value)}
                required
              />
              <Input 
                label="Price" 
                type="number"
                step="0.05"
                placeholder="0.00"
                value={price}
                onChange={(e) => setPrice(e.target.value)}
                disabled={orderType === 'MARKET'}
                required={orderType === 'LIMIT'}
              />
            </div>

            <div className="pt-2">
              <Button 
                type="submit" 
                fullWidth 
                size="lg"
                isLoading={isSubmitting}
                className={`transition-all duration-300 transform hover:-translate-y-0.5 shadow-xl ${side === 'BUY' ? 'bg-gradient-to-r from-green-600 to-green-500 hover:from-green-500 hover:to-green-400 text-white shadow-green-500/25 border-none' : 'bg-gradient-to-r from-red-600 to-red-500 hover:from-red-500 hover:to-red-400 text-white shadow-red-500/25 border-none'}`}
              >
                {side === 'BUY' ? 'PLACE BUY ORDER' : 'PLACE SELL ORDER'}
              </Button>
            </div>
          </form>
        </Card>
        
        {/* Right side panel (Live Chart) */}
        <div className="hidden lg:block">
          <Card variant="glass" className="p-6 h-full flex flex-col min-h-[480px]">
            {selectedInstrument ? (
              <div className="flex-1 flex flex-col">
                <div className="flex justify-between items-center mb-4">
                  <div>
                    <h3 className="font-bold text-white text-base">{selectedInstrument.tradingSymbol}</h3>
                    <p className="text-xs text-[var(--text-muted)]">{selectedInstrument.name}</p>
                  </div>
                  <span className="text-xs px-2.5 py-1 bg-white/5 border border-white/5 rounded-lg text-[var(--text-muted)] font-mono font-semibold">
                    5 SEC CHART
                  </span>
                </div>
                <div className="flex-grow flex flex-col min-h-[380px]">
                  <RealtimeChart symbol={selectedInstrument.tradingSymbol} />
                </div>
              </div>
            ) : (
              <div className="flex-1 flex flex-col items-center justify-center text-center p-6 border border-dashed border-white/10 rounded-xl bg-black/5">
                <LineChart size={48} className="text-[var(--primary)] opacity-40 mb-4" />
                <h3 className="text-base font-semibold text-white">No Stock Selected</h3>
                <p className="text-xs text-[var(--text-muted)] max-w-xs mt-1.5">
                  Search and select a stock from the symbol dropdown to load the real-time interactive candlestick chart.
                </p>
              </div>
            )}
          </Card>
        </div>
      </div>
    </div>
  );
};

export default Trading;
