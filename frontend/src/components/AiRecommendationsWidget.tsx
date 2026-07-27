import React, { useEffect, useState } from 'react';
import { Client } from '@stomp/stompjs';

import { motion, AnimatePresence } from 'framer-motion';
import { Brain, Target, TrendingDown, TrendingUp, X } from 'lucide-react';

interface AiAlert {
  id: string;
  symbol: string;
  spotPrice: number;
  targetOption: string;
  signal: string;
  confidence: number;
  targetProfit: number;
  stopLoss: number;
  targetLtp?: number;
  recommendedEntry?: number;
  timestamp: number;
}

export const AiRecommendationsWidget: React.FC = () => {
  const [alerts, setAlerts] = useState<AiAlert[]>([]);
  const [isOpen, setIsOpen] = useState(true);

  useEffect(() => {
    const stompClient = new Client({
      brokerURL: 'ws://localhost:8080/ws',
      reconnectDelay: 5000,
    });

    stompClient.onConnect = () => {
      stompClient.subscribe('/topic/ai-alerts', (message) => {
        const payload = JSON.parse(message.body);
        const newAlert: AiAlert = {
          ...payload,
          id: Math.random().toString(36).substr(2, 9),
          timestamp: Date.now(),
        };

        setAlerts((prev) => {
          // Keep only alerts from the last 5 minutes
          const fiveMinsAgo = Date.now() - 5 * 60 * 1000;
          const filtered = prev.filter(a => a.timestamp > fiveMinsAgo);
          return [newAlert, ...filtered].slice(0, 5); // Keep max 5 latest
        });
        setIsOpen(true); // Auto-open when new alert arrives
      });
    };

    stompClient.activate();

    // Cleanup stale alerts every minute
    const interval = setInterval(() => {
      setAlerts(prev => {
        const fiveMinsAgo = Date.now() - 5 * 60 * 1000;
        return prev.filter(a => a.timestamp > fiveMinsAgo);
      });
    }, 60000);

    return () => {
      stompClient.deactivate();
      clearInterval(interval);
    };
  }, []);

  if (alerts.length === 0) return null;

  return (
    <div className="fixed bottom-6 right-6 z-50 flex flex-col items-end space-y-4">
      <AnimatePresence>
        {isOpen && (
          <motion.div
            initial={{ opacity: 0, y: 50, scale: 0.9 }}
            animate={{ opacity: 1, y: 0, scale: 1 }}
            exit={{ opacity: 0, y: 20, scale: 0.9 }}
            className="w-96 bg-gray-900 border border-purple-500/30 rounded-xl shadow-2xl shadow-purple-500/10 overflow-hidden backdrop-blur-xl"
          >
            <div className="flex items-center justify-between p-4 border-b border-white/5 bg-gradient-to-r from-purple-900/40 to-transparent">
              <div className="flex items-center space-x-2">
                <Brain className="w-5 h-5 text-purple-400" />
                <h3 className="font-semibold text-white">Live AI Signals</h3>
                <span className="flex h-2 w-2">
                  <span className="animate-ping absolute inline-flex h-2 w-2 rounded-full bg-purple-400 opacity-75"></span>
                  <span className="relative inline-flex rounded-full h-2 w-2 bg-purple-500"></span>
                </span>
              </div>
              <button 
                onClick={() => setIsOpen(false)}
                className="text-gray-400 hover:text-white transition-colors"
              >
                <X className="w-5 h-5" />
              </button>
            </div>
            
            <div className="max-h-[400px] overflow-y-auto p-4 space-y-3 custom-scrollbar">
              {alerts.map((alert) => (
                <div key={alert.id} className="bg-white/5 rounded-lg p-3 border border-white/5 hover:border-purple-500/50 transition-colors">
                  <div className="flex justify-between items-start mb-2">
                    <div>
                      <div className="flex items-center space-x-2">
                        <span className="font-bold text-white">{alert.symbol}</span>
                        <span className="text-xs text-gray-400">@ {alert.spotPrice.toFixed(2)}</span>
                      </div>
                      <div className="text-sm font-medium text-purple-300 mt-0.5">
                        {alert.targetOption || 'No ATM Option Found'}
                      </div>
                    </div>
                    <div className={`flex items-center space-x-1 px-2 py-1 rounded text-xs font-bold ${
                      alert.signal === 'BUY_CALL' ? 'bg-green-500/20 text-green-400' : 'bg-red-500/20 text-red-400'
                    }`}>
                      {alert.signal === 'BUY_CALL' ? <TrendingUp className="w-3 h-3" /> : <TrendingDown className="w-3 h-3" />}
                      <span>{alert.signal.replace('BUY_', '')}</span>
                    </div>
                  </div>
                  
                  <div className="flex justify-between items-center mt-3 pt-3 border-t border-white/5">
                    <div className="flex flex-col">
                      <span className="text-[10px] text-gray-500 uppercase tracking-wider">Confidence</span>
                      <span className="font-bold text-white">{alert.confidence.toFixed(1)}%</span>
                    </div>
                    {alert.targetLtp && alert.targetLtp > 0 && (
                      <div className="flex flex-col">
                        <span className="text-[10px] text-blue-400 uppercase tracking-wider">Premium CMP</span>
                        <span className="font-bold text-blue-300">₹{alert.targetLtp.toFixed(1)}</span>
                      </div>
                    )}
                    {alert.recommendedEntry && alert.recommendedEntry > 0 && (
                      <div className="flex flex-col">
                        <span className="text-[10px] text-yellow-400 uppercase tracking-wider">Ideal Entry</span>
                        <span className="font-bold text-yellow-300">₹{alert.recommendedEntry.toFixed(1)}</span>
                      </div>
                    )}
                    <div className="flex space-x-4">
                      <div className="flex flex-col items-end">
                        <span className="text-[10px] text-green-500 uppercase tracking-wider">Target</span>
                        <span className="font-bold text-green-400">+{alert.targetProfit} pts</span>
                      </div>
                      <div className="flex flex-col items-end">
                        <span className="text-[10px] text-red-500 uppercase tracking-wider">SL</span>
                        <span className="font-bold text-red-400">-{alert.stopLoss} pts</span>
                      </div>
                    </div>
                  </div>
                </div>
              ))}
            </div>
          </motion.div>
        )}
      </AnimatePresence>
      
      {!isOpen && (
        <button 
          onClick={() => setIsOpen(true)}
          className="flex items-center space-x-2 bg-gray-900 border border-purple-500/30 px-4 py-2 rounded-full shadow-lg shadow-purple-500/20 hover:bg-gray-800 transition-colors"
        >
          <Brain className="w-4 h-4 text-purple-400" />
          <span className="text-sm font-medium text-white">{alerts.length} AI Signal{alerts.length !== 1 ? 's' : ''}</span>
          <span className="flex h-2 w-2 ml-1">
            <span className="animate-ping absolute inline-flex h-2 w-2 rounded-full bg-purple-400 opacity-75"></span>
            <span className="relative inline-flex rounded-full h-2 w-2 bg-purple-500"></span>
          </span>
        </button>
      )}
    </div>
  );
};
