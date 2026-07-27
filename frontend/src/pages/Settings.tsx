import React, { useState, useEffect } from 'react';
import { Card } from '../components/ui/Card';
import { Button } from '../components/ui/Button';
import { Input } from '../components/ui/Input';
import { Plus, Link as LinkIcon, CheckCircle2, AlertCircle } from 'lucide-react';
import api from '../lib/axios';

interface BrokerAccount {
  id: string;
  brokerType: string;
  clientId: string;
  status: string;
  isPrimary: boolean;
}

const Settings = () => {
  const [brokers, setBrokers] = useState<BrokerAccount[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  
  // Form State
  const [showAddForm, setShowAddForm] = useState(false);
  const [brokerType, setBrokerType] = useState('MOCK');
  const [clientId, setClientId] = useState('');
  const [apiKey, setApiKey] = useState('');
  const [apiSecret, setApiSecret] = useState('');
  const [pin, setPin] = useState('');
  const [totpSecret, setTotpSecret] = useState('');
  const [accessToken, setAccessToken] = useState('');
  const [isSubmitting, setIsSubmitting] = useState(false);

  const fetchBrokers = async () => {
    try {
      const response = await api.get('/brokers');
      setBrokers(response.data);
    } catch (error) {
      console.error('Failed to fetch brokers', error);
    } finally {
      setIsLoading(false);
    }
  };

  useEffect(() => {
    fetchBrokers();
  }, []);

  const handleLinkBroker = async (e: React.FormEvent) => {
    e.preventDefault();
    setIsSubmitting(true);
    try {
      await api.post('/brokers', {
        brokerType,
        clientId,
        apiKey,
        apiSecret,
        pin,
        totpSecret,
        accessToken
      });
      setShowAddForm(false);
      setClientId('');
      setApiKey('');
      setApiSecret('');
      setPin('');
      setTotpSecret('');
      setAccessToken('');
      fetchBrokers(); // Refresh list
    } catch (error) {
      console.error('Failed to link broker', error);
      alert('Failed to link broker account');
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <div className="space-y-6">
      <div className="flex justify-between items-center">
        <div>
          <h1 className="text-2xl font-bold text-white">Settings</h1>
          <p className="text-[var(--text-muted)]">Manage your platform preferences and broker integrations.</p>
        </div>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        <div className="lg:col-span-2 space-y-6">
          <Card variant="glass" className="p-6">
            <div className="flex justify-between items-center mb-6">
              <h2 className="text-xl font-semibold text-white flex items-center gap-2">
                <LinkIcon size={20} className="text-[var(--primary)]" />
                Broker Connections
              </h2>
              {!showAddForm && (
                <Button size="sm" onClick={() => setShowAddForm(true)}>
                  <Plus size={16} className="mr-2" /> Add Broker
                </Button>
              )}
            </div>

            {showAddForm && (
              <div className="bg-[var(--bg-color)] p-4 rounded-xl border border-white/5 mb-6">
                <h3 className="text-white font-medium mb-4">Link New Broker Account</h3>
                <form onSubmit={handleLinkBroker} className="space-y-4">
                  <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                    <div>
                      <label className="block text-sm text-[var(--text-muted)] mb-1">Broker</label>
                      <select 
                        className="w-full bg-[var(--bg-surface)] border border-[var(--border)] rounded-lg px-4 py-2.5 text-white focus:outline-none focus:border-[var(--primary)] transition-colors"
                        value={brokerType}
                        onChange={(e) => setBrokerType(e.target.value)}
                      >
                        <option value="MOCK" className="bg-[var(--bg-color)] text-white">Mock Broker (Testing)</option>
                        <option value="ZERODHA" className="bg-[var(--bg-color)] text-white">Zerodha Kite</option>
                        <option value="DHAN" className="bg-[var(--bg-color)] text-white">DhanHQ</option>
                        <option value="UPSTOX" className="bg-[var(--bg-color)] text-white">Upstox</option>
                        <option value="ANGEL_ONE" className="bg-[var(--bg-color)] text-white">Angel One</option>
                      </select>
                    </div>
                    <Input 
                      label="Client ID" 
                      value={clientId}
                      onChange={(e) => setClientId(e.target.value)}
                      required 
                    />
                    {brokerType !== 'DHAN' && (
                      <>
                        <Input 
                          label="API Key" 
                          value={apiKey}
                          onChange={(e) => setApiKey(e.target.value)}
                          required 
                        />
                        <Input 
                          label="API Secret" 
                          type="password"
                          value={apiSecret}
                          onChange={(e) => setApiSecret(e.target.value)}
                          required 
                        />
                      </>
                    )}
                    {brokerType === 'ANGEL_ONE' && (
                      <>
                        <Input 
                          label="MPIN / Password" 
                          type="password"
                          value={pin}
                          onChange={(e) => setPin(e.target.value)}
                          required 
                        />
                        <Input 
                          label="TOTP Secret (for auto-login)" 
                          type="password"
                          value={totpSecret}
                          onChange={(e) => setTotpSecret(e.target.value)}
                        />
                      </>
                    )}
                    {brokerType === 'DHAN' && (
                      <Input 
                        label="Dhan Access Token (24h)" 
                        type="password"
                        value={accessToken}
                        onChange={(e) => setAccessToken(e.target.value)}
                        required 
                      />
                    )}
                  </div>
                  <div className="flex justify-end gap-3 mt-4">
                    <Button variant="ghost" onClick={() => setShowAddForm(false)}>Cancel</Button>
                    <Button type="submit" isLoading={isSubmitting}>Link Account</Button>
                  </div>
                </form>
              </div>
            )}

            {isLoading ? (
              <div className="text-center py-8 text-[var(--text-muted)]">Loading connected brokers...</div>
            ) : brokers.length === 0 ? (
              <div className="text-center py-8 border border-dashed border-white/10 rounded-xl">
                <p className="text-[var(--text-muted)] mb-2">No broker accounts linked yet.</p>
                <p className="text-sm text-[var(--text-muted)] opacity-70">Link a mock broker to start testing.</p>
              </div>
            ) : (
              <div className="space-y-3">
                {brokers.map(broker => (
                  <div key={broker.id} className="flex items-center justify-between p-4 bg-[var(--bg-color)] rounded-xl border border-white/5">
                    <div className="flex items-center gap-4">
                      <div className="w-10 h-10 rounded-lg bg-[var(--primary)]/10 flex items-center justify-center text-[var(--primary)] font-bold">
                        {broker.brokerType.charAt(0)}
                      </div>
                      <div>
                        <h4 className="text-white font-medium">{broker.brokerType} <span className="text-sm text-[var(--text-muted)]">({broker.clientId})</span></h4>
                        <div className="flex items-center gap-1 mt-1">
                          {broker.status === 'CONNECTED' ? (
                            <><CheckCircle2 size={12} className="text-green-400" /><span className="text-xs text-green-400">Connected</span></>
                          ) : (
                            <><AlertCircle size={12} className="text-yellow-400" /><span className="text-xs text-yellow-400">{broker.status}</span></>
                          )}
                        </div>
                      </div>
                    </div>
                    <div>
                      {broker.isPrimary && (
                        <span className="px-2 py-1 bg-[var(--primary)]/20 text-[var(--primary)] text-xs rounded-md">Primary</span>
                      )}
                    </div>
                  </div>
                ))}
              </div>
            )}
          </Card>

          <Card variant="glass" className="p-6">
            <div className="flex justify-between items-center mb-6">
              <h2 className="text-xl font-semibold text-white flex items-center gap-2">
                <AlertCircle size={20} className="text-[var(--primary)]" />
                AI Trade Management Settings
              </h2>
            </div>
            <div className="space-y-4">
              <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                <div>
                  <label className="block text-sm text-[var(--text-muted)] mb-1">Recovery Probability Threshold (%)</label>
                  <input type="range" min="0" max="100" defaultValue="50" className="w-full" />
                  <p className="text-xs text-[var(--text-muted)] mt-1">If stop loss is breached, hold if AI predicts recovery &gt; this value.</p>
                </div>
                <div>
                  <label className="block text-sm text-[var(--text-muted)] mb-1">Exit Confirmation Delay (seconds)</label>
                  <Input type="number" defaultValue="5" />
                  <p className="text-xs text-[var(--text-muted)] mt-1">Wait this long before executing hard stop loss to filter noise.</p>
                </div>
                <div>
                  <label className="block text-sm text-[var(--text-muted)] mb-1">Partial Exit Size (%)</label>
                  <Input type="number" defaultValue="50" />
                </div>
                <div>
                  <label className="block text-sm text-[var(--text-muted)] mb-1">Trailing Stop Method</label>
                  <select className="w-full bg-[var(--bg-surface)] border border-[var(--border)] rounded-lg px-4 py-2.5 text-white focus:outline-none focus:border-[var(--primary)] transition-colors">
                    <option value="ATR">ATR Based</option>
                    <option value="VWAP">VWAP Based</option>
                    <option value="PERCENT">% Trailing</option>
                  </select>
                </div>
              </div>
              <div className="flex justify-end mt-4">
                <Button>Save Settings</Button>
              </div>
            </div>
          </Card>
        </div>
      </div>
    </div>
  );
};

export default Settings;
