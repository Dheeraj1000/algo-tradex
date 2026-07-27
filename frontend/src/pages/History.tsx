import React, { useState, useEffect } from 'react';
import { Card } from '../components/ui/Card';
import { History as HistoryIcon, Clock, CheckCircle2, XCircle, AlertCircle } from 'lucide-react';
import api from '../lib/axios';
import { format } from 'date-fns';

interface OrderHistoryItem {
  id: string;
  brokerOrderId: string | null;
  side: string;
  orderType: string;
  productType: string;
  quantity: number;
  price: number | null;
  status: string;
  createdAt: string;
  instrument?: {
    tradingSymbol: string;
  };
}

const getStatusBadge = (status: string) => {
  switch (status.toUpperCase()) {
    case 'FILLED':
    case 'COMPLETE':
      return <span className="flex items-center gap-1.5 px-2.5 py-1 rounded-md bg-green-500/20 text-green-400 text-xs font-semibold tracking-wide border border-green-500/20"><CheckCircle2 size={12} /> FILLED</span>;
    case 'PENDING':
    case 'OPEN':
      return <span className="flex items-center gap-1.5 px-2.5 py-1 rounded-md bg-blue-500/20 text-blue-400 text-xs font-semibold tracking-wide border border-blue-500/20"><Clock size={12} /> PENDING</span>;
    case 'CANCELLED':
      return <span className="flex items-center gap-1.5 px-2.5 py-1 rounded-md bg-gray-500/20 text-gray-400 text-xs font-semibold tracking-wide border border-gray-500/20"><XCircle size={12} /> CANCELLED</span>;
    case 'REJECTED':
      return <span className="flex items-center gap-1.5 px-2.5 py-1 rounded-md bg-red-500/20 text-red-400 text-xs font-semibold tracking-wide border border-red-500/20"><AlertCircle size={12} /> REJECTED</span>;
    default:
      return <span className="px-2.5 py-1 rounded-md bg-white/10 text-gray-300 text-xs font-semibold tracking-wide border border-white/10">{status}</span>;
  }
};

const History = () => {
  const [orders, setOrders] = useState<OrderHistoryItem[]>([]);
  const [isLoading, setIsLoading] = useState(true);

  const fetchOrders = async () => {
    try {
      const response = await api.get('/orders');
      // Sort newest first
      const sortedOrders = response.data.sort((a: any, b: any) => 
        new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime()
      );
      setOrders(sortedOrders);
    } catch (error) {
      console.error('Failed to fetch orders', error);
    } finally {
      setIsLoading(false);
    }
  };

  useEffect(() => {
    fetchOrders();
  }, []);

  const handleCancelOrder = async (orderId: string) => {
    if (!window.confirm('Are you sure you want to cancel this order?')) return;
    try {
      await api.delete(`/orders/${orderId}`);
      alert('Order cancellation request sent');
      fetchOrders();
    } catch (error: any) {
      console.error('Failed to cancel order', error);
      alert(error.response?.data?.message || 'Failed to cancel order');
    }
  };

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-2xl font-bold text-white flex items-center gap-3">
          <HistoryIcon className="text-[var(--primary)]" /> Order History
        </h1>
        <p className="text-[var(--text-muted)] mt-1">View your complete order execution history and status.</p>
      </div>

      <Card variant="glass" className="overflow-hidden">
        <div className="overflow-x-auto">
          <table className="w-full text-left border-collapse">
            <thead>
              <tr className="bg-white/5 border-b border-white/10 text-[var(--text-muted)] text-sm uppercase tracking-wider">
                <th className="py-4 px-6 font-medium">Time</th>
                <th className="py-4 px-6 font-medium">Symbol</th>
                <th className="py-4 px-6 font-medium">Type</th>
                <th className="py-4 px-6 font-medium text-right">Qty</th>
                <th className="py-4 px-6 font-medium text-right">Price</th>
                <th className="py-4 px-6 font-medium text-center">Status</th>
                <th className="py-4 px-6 font-medium text-right">Order ID</th>
                <th className="py-4 px-6 font-medium text-center">Actions</th>
              </tr>
            </thead>
            <tbody className="text-sm">
              {isLoading ? (
                <tr>
                  <td colSpan={8} className="text-center py-12 text-[var(--text-muted)]">
                    <div className="flex flex-col items-center gap-3">
                      <div className="w-6 h-6 border-2 border-[var(--primary)] border-t-transparent rounded-full animate-spin"></div>
                      Loading order history...
                    </div>
                  </td>
                </tr>
              ) : orders.length === 0 ? (
                <tr>
                  <td colSpan={8} className="text-center py-12 text-[var(--text-muted)]">
                    <div className="flex flex-col items-center gap-3">
                      <HistoryIcon size={32} className="opacity-20" />
                      <p>No orders found.</p>
                    </div>
                  </td>
                </tr>
              ) : (
                orders.map((order) => {
                  return (
                    <tr key={order.id} className="border-b border-white/5 hover:bg-white/[0.02] transition-colors">
                      <td className="py-4 px-6 text-[var(--text-muted)]">
                        <div className="font-medium text-gray-300">
                          {format(new Date(order.createdAt), 'dd MMM yyyy')}
                        </div>
                        <div className="text-xs">
                          {format(new Date(order.createdAt), 'HH:mm:ss')}
                        </div>
                      </td>
                      <td className="py-4 px-6">
                        <div className="font-bold text-white">{order.instrument?.tradingSymbol || 'MOCK_INSTRUMENT'}</div>
                        <div className="text-xs text-[var(--text-muted)] mt-0.5">{order.productType}</div>
                      </td>
                      <td className="py-4 px-6">
                        <div className="flex items-center gap-2">
                          <span className={`px-2 py-0.5 rounded text-xs font-bold tracking-widest ${order.side === 'BUY' ? 'bg-green-500/10 text-green-500' : 'bg-red-500/10 text-red-500'}`}>
                            {order.side}
                          </span>
                          <span className="text-gray-400 text-xs font-medium bg-white/5 px-2 py-0.5 rounded">
                            {order.orderType}
                          </span>
                        </div>
                      </td>
                      <td className="py-4 px-6 text-right font-medium text-white">{order.quantity}</td>
                      <td className="py-4 px-6 text-right text-gray-300">
                        {order.orderType === 'MARKET' ? 'MKT' : `₹${order.price?.toFixed(2) || '0.00'}`}
                      </td>
                      <td className="py-4 px-6">
                        <div className="flex justify-center">
                          {getStatusBadge(order.status)}
                        </div>
                      </td>
                      <td className="py-4 px-6 text-right text-xs text-[var(--text-muted)] font-mono">
                        {order.brokerOrderId || order.id.split('-')[0]}
                      </td>
                      <td className="py-4 px-6 text-center">
                        {(order.status === 'PENDING' || order.status === 'OPEN') ? (
                          <button
                            onClick={() => handleCancelOrder(order.id)}
                            className="p-1 text-red-400 hover:text-red-300 hover:bg-red-500/10 rounded transition-colors"
                            title="Cancel Order"
                          >
                            <XCircle size={16} />
                          </button>
                        ) : (
                          <span className="text-[var(--text-muted)] opacity-30">—</span>
                        )}
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

export default History;
