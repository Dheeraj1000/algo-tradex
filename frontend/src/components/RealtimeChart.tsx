import { useEffect, useRef, useState } from 'react';
import { createChart, CandlestickSeries } from 'lightweight-charts';
import { Client } from '@stomp/stompjs';

import api from '../lib/axios';

interface RealtimeChartProps {
  symbol: string;
}

export const RealtimeChart = ({ symbol }: RealtimeChartProps) => {
  const chartContainerRef = useRef<HTMLDivElement>(null);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (!chartContainerRef.current) return;

    const chart = createChart(chartContainerRef.current, {
      layout: {
        background: { color: 'transparent' },
        textColor: '#9ca3af',
      },
      grid: {
        vertLines: { color: 'rgba(38, 44, 54, 0.5)' },
        horzLines: { color: 'rgba(38, 44, 54, 0.5)' },
      },
      crosshair: {
        mode: 1, 
      },
      timeScale: {
        borderColor: '#262c36',
        timeVisible: true,
        secondsVisible: false,
      },
      localization: {
        timeFormatter: (businessDayOrTimestamp: any) => {
          const date = new Date(businessDayOrTimestamp * 1000);
          return date.toLocaleTimeString('en-US', { hour: '2-digit', minute: '2-digit', hour12: false, timeZone: 'UTC' });
        }
      },
    });

    const candleSeries = chart.addSeries(CandlestickSeries, {
      upColor: '#10b981',
      downColor: '#ef4444',
      borderUpColor: '#10b981',
      borderDownColor: '#ef4444',
      wickUpColor: '#10b981',
      wickDownColor: '#ef4444',
    });

    const apiBase = api.defaults.baseURL || 'http://localhost:8080/api';
    const backendHost = apiBase.replace('/api', '');

    let active = true;
    let stompClient: Client | null = null;
    let latestCandle: any = null;

    const fetchHistoryAndConnect = async () => {
      try {
        const response = await api.get(`/market-data/candles?symbol=${symbol}&limit=400`);
        const istOffset = 19800; // 5 hours 30 mins
        
        const historicalData = response.data.map((c: any) => ({
          time: (c.time + istOffset) as any,
          open: parseFloat(c.open),
          high: parseFloat(c.high),
          low: parseFloat(c.low),
          close: parseFloat(c.close),
        }));

        if (!active) return;
        candleSeries.setData(historicalData);
        if (historicalData.length > 0) {
          latestCandle = { ...historicalData[historicalData.length - 1] };
        }

        stompClient = new Client({
          brokerURL: `${backendHost.replace(/^http/, 'ws')}/ws`,
          reconnectDelay: 5000,
          heartbeatIncoming: 4000,
          heartbeatOutgoing: 4000,
        });

        stompClient.onConnect = () => {
          if (!active) return;
          stompClient?.subscribe(`/topic/ticks/${symbol}`, (message) => {
            const tick = JSON.parse(message.body);
            const tickPrice = parseFloat(tick.lastPrice);
            
            // Safely parse tick.timestamp (could be ISO string or seconds)
            let tickEpochSeconds = 0;
            if (typeof tick.timestamp === 'number') {
                tickEpochSeconds = tick.timestamp;
            } else {
                tickEpochSeconds = Math.floor(new Date(tick.timestamp).getTime() / 1000);
            }
            
            const istOffset = 19800; // 5 hours 30 mins
            const tickTime = tickEpochSeconds + istOffset;
            const currentBucket = Math.floor(tickTime / 60) * 60;

            if (!latestCandle || currentBucket > latestCandle.time) {
              const newCandle = {
                time: currentBucket as any,
                open: latestCandle ? latestCandle.close : tickPrice,
                high: tickPrice,
                low: tickPrice,
                close: tickPrice,
              };
              latestCandle = newCandle;
              candleSeries.update(newCandle);
            } else {
              latestCandle.high = Math.max(latestCandle.high, tickPrice);
              latestCandle.low = Math.min(latestCandle.low, tickPrice);
              latestCandle.close = tickPrice;
              candleSeries.update(latestCandle);
            }
          });
        };

        stompClient.onStompError = (frame) => {
          console.error('Broker reported error: ' + frame.headers['message']);
        };

        stompClient.activate();

      } catch (err) {
        console.error('Failed to load chart data', err);
        if (active) setError('Failed to load chart data');
      }
    };

    fetchHistoryAndConnect();

    const handleResize = () => {
      if (chartContainerRef.current) {
        chart.applyOptions({
          width: chartContainerRef.current.clientWidth,
          height: chartContainerRef.current.clientHeight,
        });
      }
    };

    window.addEventListener('resize', handleResize);
    const timer = setTimeout(handleResize, 100);

    return () => {
      active = false;
      clearTimeout(timer);
      window.removeEventListener('resize', handleResize);
      stompClient?.deactivate();
      chart.remove();
    };
  }, [symbol]);

  return (
    <div className="relative w-full h-full min-h-[300px] flex-grow flex flex-col">
      {error && (
        <div className="absolute inset-0 flex items-center justify-center bg-black/40 backdrop-blur-sm z-10 rounded-xl">
          <p className="text-red-400 font-semibold">{error}</p>
        </div>
      )}
      <div ref={chartContainerRef} className="w-full h-full flex-grow" style={{ minHeight: '300px' }} />
    </div>
  );
};
